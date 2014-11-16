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
        } catch (Exception var4) {
            throw new ConfigurationException(var4);
        }
    }

    @Override
    public Map<String, Object> getConfigurationRoot() throws ConfigurationException {
        return null;
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
            populateLdapNode(configNode, configurableClass, ldapNode);

        } else
            throw new RuntimeException("Not implemented yet");
    }


    public static class LdapNode {

        LdapNode parent;
        String rdn;
        String dn;
        Collection<String> objectClasses = new ArrayList<String>();
        Attributes attributes = new BasicAttributes();
        ;
        Collection<LdapNode> children = new ArrayList<LdapNode>();

    }


    void storeCtx() {
        try {
            ldapCtx.createSubcontext(dnOf(currentDN, classLDAPAnno.distinguishingField(), itemValue), attrs);
        } catch (NamingException e) {
            throw new ConfigurationException("Error while storing configuration for class " + configurableClass.getSimpleName());
        }

    }

    private void populateLdapNode(Map<String, Object> configNode, Class configurableClass, LdapNode ldapNode) throws ConfigurationException {

        LDAP classLDAPAnno = (LDAP) configurableClass.getAnnotation(LDAP.class);
        ConfigurableClass classAnno = (ConfigurableClass) configurableClass.getAnnotation(ConfigurableClass.class);

        if (classAnno == null || classLDAPAnno == null) throw new ConfigurationException("Unexpected error");

        BasicAttributes attrs = new BasicAttributes();

        List<AnnotatedConfigurableProperty> properties = ConfigIterators.getAllConfigurableFieldsAndSetterParameters(configurableClass);

        for (AnnotatedConfigurableProperty property : properties) {

            // map
            if (property.isMapOfConfObjects()) {
                Map<String, Object> map = (Map<String, Object>) configNode.get(property.getAnnotatedName());

                AnnotatedConfigurableProperty elementProperty = property.getPseudoPropertyForGenericsParamater(1);
                boolean noContainerNode1 = elementProperty.getAnnotation(LDAP.class).noContainerNode();

                LdapNode thisParent;
                boolean noContainerNode = property.getAnnotation(LDAP.class).noContainerNode() || noContainerNode1;

                if (noContainerNode) {
                    thisParent = ldapNode;
                } else {
                    thisParent = new LdapNode();
                    thisParent.dn = dnOf(ldapNode.dn, "cn", getLDAPPropertyName(property));

                    if (property.getAnnotation(LDAP.class).objectClasses().length == 0)
                        thisParent.objectClasses.add("dcmCollection");
                    else
                        thisParent.objectClasses = new ArrayList<String>(Arrays.asList(property.getAnnotation(LDAP.class).objectClasses()));

                    thisParent.parent = ldapNode;
                    ldapNode.children.add(thisParent);
                }

                String elementDistibguishingName = elementProperty.getAnnotation(LDAP.class).distinguishingField();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    LdapNode elementNode = new LdapNode();
                    elementNode.dn = dnOf(thisParent.dn, elementDistibguishingName, entry.getKey());
                }


            }

            //collection/array


            // nested object


            // default

            //TODO map
            //TODO array/collection

            ldapNode.attributes.put(getLDAPPropertyName(property), configNode.get(property.getAnnotatedName()));

        }

    }

    private String getLDAPPropertyName(AnnotatedConfigurableProperty property) throws ConfigurationException {
        String ldapName = property.getAnnotation(LDAP.class).overriddenName();
        return ldapName.equals("") ? property.getAnnotatedName() : ldapName;
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
