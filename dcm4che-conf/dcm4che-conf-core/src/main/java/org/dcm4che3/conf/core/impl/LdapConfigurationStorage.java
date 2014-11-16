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
package org.dcm4che3.conf.core.impl;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.LDAP;
import org.dcm4che3.conf.core.util.ConfigIterators;
import org.dcm4che3.conf.dicom.CommonDicomConfiguration;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InitialDirContext;
import java.util.*;

/**
 * TODO: thread safety
 */
public class LdapConfigurationStorage implements Configuration {

    private final String baseDN;
    private final InitialDirContext ldapCtx;


    public LdapConfigurationStorage(Hashtable<String, String> env) throws ConfigurationException {

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

    @Override
    public Map<String, Object> getConfigurationRoot() throws ConfigurationException {
        return new HashMap<String, Object>();
    }

    // special classes for root, app registrues,(or allow to register)

    @Override
    public Object getConfigurationNode(String path, Class configurableClass) throws ConfigurationException {
        // TODO: byte[],x509 to base64
        // transform references
        // special booleanBased EnumSet

        throw new RuntimeException("Not implemented yet");
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

        if (path.equals("/dicomConfigurationRoot")) {
            LdapNode ldapNode = new LdapNode();
            ldapNode.dn = dnOf(baseDN, "cn", "DICOM Configuration");
            populateLdapNode(configNode, CommonDicomConfiguration.DicomConfigurationRootNode.class, ldapNode);

            System.out.println("");


            // TODO: also fill in other parameters from the configNode according to 'partially overwritten' contract
        } else
            throw new RuntimeException("Not implemented yet");
    }


    public static class LdapNode {

        private LdapNode parent;
        String rdn;
        String dn;
        Collection<String> objectClasses = new ArrayList<String>();
        Attributes attributes = new BasicAttributes();
        Collection<LdapNode> children = new ArrayList<LdapNode>();

        public LdapNode getParent() {
            return parent;
        }

        public void setParent(LdapNode parent) {
            this.parent = parent;
            parent.children.add(this);
        }
    }


    /*void storeCtx() {
        try {
            ldapCtx.createSubcontext(dnOf(currentDN, classLDAPAnno.distinguishingField(), itemValue), attrs);
        } catch (NamingException e) {
            throw new ConfigurationException("Error while storing configuration for class " + configurableClass.getSimpleName());
        }

    }*/


    /**
     * Responsible for
     * objectClasses, attributes, children
     * <p/>
     * NOT responsible for
     * parent, dn
     *
     * @param configNode
     * @param configurableClass
     * @param ldapNode
     * @throws ConfigurationException
     */
    private void populateLdapNode(Map<String, Object> configNode, Class configurableClass, LdapNode ldapNode) throws ConfigurationException {

        if (configurableClass == null ||
                configurableClass.getAnnotation(ConfigurableClass.class) == null ||
                configurableClass.getAnnotation(LDAP.class) == null)
            throw new ConfigurationException("Unexpected error - class '" + configurableClass == null ? null : configurableClass.getName() + "' is not a configurable class");

        ldapNode.objectClasses = getObjectClasses(configurableClass);

        List<AnnotatedConfigurableProperty> properties = ConfigIterators.getAllConfigurableFieldsAndSetterParameters(configurableClass);
        for (AnnotatedConfigurableProperty property : properties) {

            Object propertyConfigNode = configNode.get(property.getAnnotatedName());

            if (propertyConfigNode == null) continue;

            // map of anything
            if (Map.class.isAssignableFrom(property.getRawClass())) {
                LdapNode thisParent = makeLdapCollectionNode(ldapNode, property);
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) propertyConfigNode).entrySet()) {
                    LdapNode elementNode = makeLdapElementNode(property, thisParent, entry.getKey());

                    // now if it is a conf obj, not primitive or custom representation, go deeper
                    if (property.isMapOfConfObjects() && entry.getValue() instanceof Map) {
                        populateLdapNode((Map<String, Object>) entry.getValue(), property.getPseudoPropertyForCollectionElement().getRawClass(), elementNode);
                        continue;
                    }

                    //otherwise, make this mock with value
                    elementNode.objectClasses = Arrays.asList(property.getAnnotation(LDAP.class).mapEntryObjectClass());
                    elementNode.attributes.put(property.getAnnotation(LDAP.class).mapValueAttribute(), entry.getValue());

                }
                continue;
            }

            //collection/array of confobjects, (but not custom representations)
            if (property.isArrayOfConfObjects() || property.isCollectionOfConfObjects()) {
                Iterator iterator = ((Collection) propertyConfigNode).iterator();
                // collection not empty and first element is not a custom rep
                if (iterator.hasNext() && iterator.next() instanceof Map) {
                    LdapNode thisParent = makeLdapCollectionNode(ldapNode, property);
                    for (Map<String, Object> o : ((Collection<Map<String, Object>>) propertyConfigNode)) {
                        LdapNode elementNode = makeLdapElementNode(property, thisParent, (String) o.get(getDistinguishingFieldForCollectionElement(property)));
                        populateLdapNode(o, property.getPseudoPropertyForCollectionElement().getRawClass(), elementNode);
                    }
                    continue;
                }
            }

            // nested conf object, not custom representation
            if (property.getRawClass().getAnnotation(ConfigurableClass.class) != null)
                if (propertyConfigNode instanceof Map) {
                    LdapNode nestedNode;
                    if (isNoContainerNode(property)) nestedNode = ldapNode;
                    else {
                        nestedNode = new LdapNode();
                        nestedNode.dn = dnOf(ldapNode.dn, getDistinguishingField(property), getLDAPPropertyName(property));
                        nestedNode.attributes.put(getDistinguishingField(property), getLDAPPropertyName(property));
                    }
                    populateLdapNode((Map<String, Object>) propertyConfigNode, property.getRawClass(), nestedNode);
                    continue;
                }

            // array/collection
            /*if (propertyConfigNode instanceof Collection) {
                ldapNode.attributes.put(propertyConfigNode);
            }*/

            // regular attribute
            ldapNode.attributes.put(getLDAPPropertyName(property), propertyConfigNode);

        }

    }

    private LdapNode makeLdapElementNode(AnnotatedConfigurableProperty property, LdapNode parent, String key) {
        LdapNode elementNode1 = new LdapNode();
        String elementDistinguishingField = getDistinguishingFieldForCollectionElement(property);
        elementNode1.dn = dnOf(parent.dn, elementDistinguishingField, key);
        elementNode1.attributes.put(elementDistinguishingField, key);
        elementNode1.setParent(parent);
        return elementNode1;
    }

    // resolve map/collection key field
    private String getDistinguishingFieldForCollectionElement(AnnotatedConfigurableProperty property) {

        // by default use what annotated on the property
        String elementDistinguishingField = property.getAnnotation(LDAP.class).distinguishingField();

        // if property is default, check the annotation on the element class if its a confclass
        if (elementDistinguishingField.equals(LDAP.DEFAULT_DISTINGUISHING_FIELD))
            if (property.isArrayOfConfObjects() || property.isCollectionOfConfObjects() || property.isMapOfConfObjects())
                elementDistinguishingField = ((LDAP) property.getPseudoPropertyForCollectionElement().getRawClass().getAnnotation(LDAP.class)).distinguishingField();

        return elementDistinguishingField;
    }

    private String getDistinguishingField(AnnotatedConfigurableProperty property) {
        String distinguishingField = property.getAnnotation(LDAP.class).distinguishingField();
        if (distinguishingField.equals(LDAP.DEFAULT_DISTINGUISHING_FIELD))
            distinguishingField = ((LDAP) property.getRawClass().getAnnotation(LDAP.class)).distinguishingField();
        return distinguishingField;
    }

    private LdapNode makeLdapCollectionNode(LdapNode ldapNode, AnnotatedConfigurableProperty property) throws ConfigurationException {

        LdapNode thisParent;

        if (isNoContainerNode(property)) {
            thisParent = ldapNode;
        } else {
            thisParent = new LdapNode();
            thisParent.dn = dnOf(ldapNode.dn, "cn", getLDAPPropertyName(property));

            if (property.getAnnotation(LDAP.class).objectClasses().length == 0)
                thisParent.objectClasses.add("dcmCollection");
            else
                thisParent.objectClasses = getObjectClasses(property);

            thisParent.setParent(ldapNode);
        }
        return thisParent;
    }

    private boolean isNoContainerNode(AnnotatedConfigurableProperty property) {
        return property.getAnnotation(LDAP.class).noContainerNode() ||
                ((LDAP) property.getPseudoPropertyForCollectionElement().getRawClass().getAnnotation(LDAP.class)).noContainerNode();
    }

    private ArrayList<String> getObjectClasses(AnnotatedConfigurableProperty property) {
        return new ArrayList<String>(Arrays.asList(property.getAnnotation(LDAP.class).objectClasses()));
    }

    private ArrayList<String> getObjectClasses(Class clazz) {
        return new ArrayList<String>(Arrays.asList(((LDAP) clazz.getAnnotation(LDAP.class)).objectClasses()));
    }

    private String getLDAPPropertyName(AnnotatedConfigurableProperty property) throws ConfigurationException {
        LDAP ldapAnno = property.getAnnotation(LDAP.class);
        if (ldapAnno == null || ldapAnno.overriddenName().equals("")) {
            return property.getAnnotatedName();
        } else {
            return ldapAnno.overriddenName();
        }
    }

    public String dnOf(String parentDN, String attrID, String attrValue) {
        return attrID + '=' + attrValue + ',' + parentDN;
    }

    @Override
    public void refreshNode(String path) throws ConfigurationException {
        throw new RuntimeException("Not implemented yet");

    }

    @Override
    public void removeNode(String path) throws ConfigurationException {
        throw new RuntimeException("Not implemented yet");

    }

    @Override
    public Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException {
        throw new RuntimeException("Not implemented yet");
    }
}
