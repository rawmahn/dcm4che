/*
 * **** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *  Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 *  The Initial Developer of the Original Code is
 *  Agfa Healthcare.
 *  Portions created by the Initial Developer are Copyright (C) 2014
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *  See @authors listed below
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 2 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 *
 *  ***** END LICENSE BLOCK *****
 */
package org.dcm4che3.conf.ldap;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.LDAP;
import org.dcm4che3.conf.core.util.ConfigIterators;
import org.dcm4che3.conf.dicom.CommonDicomConfiguration;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.hl7.HL7Application;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.*;


public class LdapConfigurationStorage implements Configuration {

    private final String baseDN;
    private final InitialDirContext ldapCtx;
    private final List<Class<?>> allExtensionClasses;

    public List<Class<?>> getAllExtensionClasses() {
        return allExtensionClasses;
    }

    public LdapConfigurationStorage(Hashtable<String, String> env, List<Class<?>> allExtensionClasses) throws ConfigurationException {
        this.allExtensionClasses = allExtensionClasses;

        try {
            env = (Hashtable) env.clone();
            String e = (String) env.get("java.naming.provider.url");
            int end = e.lastIndexOf(47);
            env.put("java.naming.provider.url", e.substring(0, end));
            this.baseDN = e.substring(end + 1);
            this.ldapCtx = new InitialDirContext(env);
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public synchronized void destroySubcontextWithChilds(String name) throws NamingException {
        NamingEnumeration list = ldapCtx.list(name);

        while (list.hasMore()) {
            this.destroySubcontextWithChilds(((NameClassPair) list.next()).getNameInNamespace());
        }

        ldapCtx.destroySubcontext(name);
    }


    private void merge(LdapNode ldapNode) {
        try {

            mergeIn(ldapNode);

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getConfigurationRoot() throws ConfigurationException {
        return new HashMap<String, Object>();
    }

    @Override
    public Object getConfigurationNode(String path, Class configurableClass) throws ConfigurationException {
        // TODO: byte[],x509 to base64
        // special booleanBased EnumSet

        if (path.equals("/dicomConfigurationRoot"))
            configurableClass = CommonDicomConfiguration.DicomConfigurationRootNode.class;

        String dn = LdapConfigUtils.refToLdapDN(path, this);

        try {
            return readNode(dn, configurableClass);
        } catch (NamingException e) {
            throw new ConfigurationException("Cannot read node from ldap :" + path, e);
        }
    }

    private Map<String, Object> readNode(String dn, Class configurableClass) throws ConfigurationException, NamingException {
        ArrayList<String> objectClasses = LdapConfigUtils.extractObjectClasses(configurableClass);

        Attributes attributes;
        try {
            attributes = ldapCtx.getAttributes(dn);
        } catch (NameNotFoundException noname) {
            // workaround - endless loops due to the fact that we allow nodes without attributes.. and device is a ref... so just prohibit Device
            // we have to use references on devices...
            if (configurableClass.equals(Device.class)) return null;
            attributes = null;
        }

        Map<String, Object> map = new HashMap<String, Object>();
        for (AnnotatedConfigurableProperty property : ConfigIterators.getAllConfigurableFieldsAndSetterParameters(configurableClass)) {

            // map
            if (property.isMapOfConfObjects()) {
                String subDn = getSubDn(dn, property);

                Map<String, Object> m = new HashMap<String, Object>();
                try {
                    NamingEnumeration<SearchResult> enumeration = searchForCollectionElements(subDn, property);
                    while (enumeration.hasMore()) {
                        SearchResult res = enumeration.next();
                        String distField = LdapConfigUtils.getDistinguishingFieldForCollectionElement(property);
                        String key = (String) res.getAttributes().get(distField).get();
                        Object value = readNode(res.getName() + "," + subDn, property.getPseudoPropertyForCollectionElement().getRawClass());
                        m.put(key, value);
                    }
                    map.put(property.getAnnotatedName(), m);
                } catch (NameNotFoundException e) {
                    //noop
                }

                continue;

            }

            // nested
            if (property.isConfObject()) {

                String subDn = getSubDn(dn, property);
                map.put(property.getAnnotatedName(), readNode(subDn, property.getRawClass()));
                continue;
            }

            // collection with confObjects
            if (property.isArrayOfConfObjects() || property.isCollectionOfConfObjects() && !property.getAnnotation(ConfigurableProperty.class).collectionOfReferences()) {

                Class elemClass = property.getPseudoPropertyForCollectionElement().getRawClass();
                String subDn = getSubDn(dn, property);

                try {
                    NamingEnumeration<SearchResult> enumeration = searchForCollectionElements(subDn, property);
                    ArrayList<Object> list = new ArrayList<Object>();
                    while (enumeration.hasMore()) {
                        SearchResult next = enumeration.next();
                        list.add(readNode(next.getName() + "," + dn, elemClass));
                    }
                    map.put(property.getAnnotatedName(), list);
                } catch (NameNotFoundException e) {
                    //noop
                }
                continue;
            }

            // primitive collection
            if ((Collection.class.isAssignableFrom(property.getRawClass()) || property.getRawClass().isArray()) && attributes != null) {
                Attribute attribute = attributes.get(LdapConfigUtils.getLDAPPropertyName(property));

                if (attribute == null) continue;

                ArrayList<Object> list = new ArrayList<Object>();

                // special case with references
                for (int i = 0; i < attribute.size(); i++)
                    if (property.getAnnotation(ConfigurableProperty.class).collectionOfReferences() &&
                            property.getPseudoPropertyForCollectionElement().getRawClass().equals(Connection.class)) {
                        list.add(LdapConfigUtils.connectionLdapDnToRef((String) attribute.get(i), this));
                    } else {
                        list.add(attribute.get(i));
                    }
                map.put(property.getAnnotatedName(), list);
                continue;
            }

            if (attributes != null) {
                Attribute attribute = attributes.get(LdapConfigUtils.getLDAPPropertyName(property));
                if (attribute != null)
                    map.put(property.getAnnotatedName(), attribute.get().toString());
            }

        }

        if (configurableClass.equals(Device.class)) {
            fillExtension(dn, map, "deviceExtensions");
        } else if (configurableClass.equals(ApplicationEntity.class)) {
            fillExtension(dn, map, "aeExtensions");
        } else if (configurableClass.equals(HL7Application.class)) {
            fillExtension(dn, map, "hl7AppExtensions");
        }

        return map;
    }

    private void fillExtension(String dn, Map<String, Object> map, String extensionLabel) throws NamingException, ConfigurationException {
        HashMap<String, Object> exts = new HashMap<String, Object>();
        map.put(extensionLabel, exts);

        for (Class<?> aClass : allExtensionClasses) {

            LDAP ldapAnno = aClass.getAnnotation(LDAP.class);

            String subDn;
            if (ldapAnno == null || !ldapAnno.noContainerNode())
                subDn = LdapConfigUtils.dnOf(dn, "cn", aClass.getSimpleName());
            else
                subDn = dn;

            Map<String, Object> ext = readNode(subDn, aClass);
            if (ext == null || ext.isEmpty()) continue;

            exts.put(aClass.getSimpleName(), ext);

        }
    }

    private String getSubDn(String dn, AnnotatedConfigurableProperty property) throws ConfigurationException {
        String subDn;
        if (LdapConfigUtils.isNoContainerNode(property))
            subDn = dn;
        else
            subDn = LdapConfigUtils.dnOf(dn, "cn", LdapConfigUtils.getLDAPPropertyName(property));
        return subDn;
    }

    private NamingEnumeration<SearchResult> searchForCollectionElements(String dn, AnnotatedConfigurableProperty property) throws NamingException, ConfigurationException {
        NamingEnumeration<SearchResult> enumeration;
        Class aClass = property.getPseudoPropertyForCollectionElement().getRawClass();
        try {
            enumeration = searchSubcontextWithClass(LdapConfigUtils.extractObjectClasses(aClass).get(0), dn);
        } catch (IndexOutOfBoundsException e) {
            throw new ConfigurationException("No object class defined for class " + aClass, e);
        }
        return enumeration;
    }

    @Override
    public Class getConfigurationNodeClass(String path) throws ConfigurationException, ClassNotFoundException {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean nodeExists(String path) throws ConfigurationException {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {
        // TODO: byte[], x509 from base64
        // dynamic dn generation for lists... maybe allow to use an extension

        String dn = LdapConfigUtils.refToLdapDN(path, this);

        LdapNode ldapNode = new LdapNode(this);
        ldapNode.setDn(dn);
        ldapNode.populate(configNode, configurableClass);


        merge(ldapNode);

        // TODO: also fill in other parameters from the configNode according to 'partially overwritten' contract
    }


    private void mergeIn(LdapNode ldapNode) throws NamingException {

        // merge attributes of this node
        if (!ldapNode.getObjectClasses().isEmpty()) {

            BasicAttribute objectClass = new BasicAttribute("objectClass");
            for (String c : ldapNode.getObjectClasses()) objectClass.add(c);
            ldapNode.getAttributes().put(objectClass);

            try {
                ldapCtx.createSubcontext(ldapNode.getDn(), ldapNode.getAttributes());
            } catch (NameAlreadyBoundException alreadyBoundE) {

                // Append objectClass
                ldapNode.getAttributes().remove("objectClass");
                Attribute existingObjectClasses = ldapCtx.getAttributes(ldapNode.getDn(), new String[]{"objectClass"}).get("objectClass");
                for (String c : ldapNode.getObjectClasses())
                    if (!existingObjectClasses.contains(c))
                        existingObjectClasses.add(c);

                ldapNode.getAttributes().put(existingObjectClasses);

                // replace attributes
                ldapCtx.modifyAttributes(ldapNode.getDn(), DirContext.REPLACE_ATTRIBUTE, ldapNode.getAttributes());
            }
        }

        // remove children that do not exist in the new config
        // see which objectclasses are children of the node and remove them all
        for (String childObjClass : ldapNode.getChildrenObjectClasses()) {

            NamingEnumeration<SearchResult> ne = searchSubcontextWithClass(childObjClass, ldapNode.getDn());

            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                // TODO: filter out those who dont need to be killed
                try {
                    destroySubcontextWithChilds(sr.getName());
                } catch (NameNotFoundException exception) {
                    //noop, proceed
                }
            }


        }

        // descent recursively
        for (LdapNode child : ldapNode.getChildren()) mergeIn(child);
    }

    private NamingEnumeration<SearchResult> searchSubcontextWithClass(String childObjClass, String dn) throws NamingException {
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(1);
        ctls.setReturningObjFlag(false);
        return ldapCtx.search(dn, "(objectclass=" + childObjClass + ")", ctls);
    }

    @Override
    public void refreshNode(String path) throws ConfigurationException {
        throw new RuntimeException("Not implemented yet");

    }

    @Override
    public void removeNode(String path) throws ConfigurationException {
        LdapConfigUtils.BooleanContainer dnIsKillableWrapper = new LdapConfigUtils.BooleanContainer();
        String dn = LdapConfigUtils.refToLdapDN(path, this, dnIsKillableWrapper);
        if (dnIsKillableWrapper.isKillable()) {
            try {
                destroySubcontextWithChilds(dn);
            } catch (NameNotFoundException nnfe) {
                //noop
            } catch (NamingException e) {
                throw new ConfigurationException(e);
            }
        }

    }

    @Override
    public Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException {
        throw new RuntimeException("Not implemented yet");
    }

    public String getBaseDN() {
        return baseDN;
    }
}
