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

import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.LDAP;
import org.dcm4che3.conf.core.util.ConfigIterators;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.conf.dicom.CommonDicomConfiguration;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.hl7.HL7Application;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.lang.annotation.Annotation;
import java.util.*;

import static org.dcm4che3.conf.core.util.ConfigNodeUtil.escapeApos;

/**
 * @author: Roman K
 */
public class LdapConfigUtils {
    /**
     * Resolve map/collection key field for a property
     *
     * @param property
     * @return
     */
    static String getDistinguishingFieldForCollectionElement(AnnotatedConfigurableProperty property) {

        // by default use what annotated on the property
        LDAP annotation = property.getAnnotation(LDAP.class);
        String elementDistinguishingField = annotation == null ? LDAP.DEFAULT_DISTINGUISHING_FIELD : annotation.distinguishingField();

        // if property is default, check the annotation on the element class if its a confclass
        if (elementDistinguishingField.equals(LDAP.DEFAULT_DISTINGUISHING_FIELD))
            if (property.isArrayOfConfObjects() || property.isCollectionOfConfObjects() || property.isMapOfConfObjects())
                elementDistinguishingField = ((LDAP) property.getPseudoPropertyForCollectionElement().getRawClass().getAnnotation(LDAP.class)).distinguishingField();

        return elementDistinguishingField;
    }

    static String getDistinguishingField(AnnotatedConfigurableProperty property) {
        LDAP propLdapAnno = property.getAnnotation(LDAP.class);

        String distinguishingField;
        if (propLdapAnno != null)
            distinguishingField = propLdapAnno.distinguishingField();
        else
            distinguishingField = LDAP.DEFAULT_DISTINGUISHING_FIELD;

        if (distinguishingField.equals(LDAP.DEFAULT_DISTINGUISHING_FIELD)) {
            Annotation classLdapAnno = property.getRawClass().getAnnotation(LDAP.class);
            if (classLdapAnno != null)
                distinguishingField = ((LDAP) classLdapAnno).distinguishingField();
        }

        return distinguishingField;
    }

    static boolean isNoContainerNode(AnnotatedConfigurableProperty property) {
        LDAP propLdapAnno = property.getAnnotation(LDAP.class);

        LDAP classLdapAnno = null;
        if (property.isConfObject())
            classLdapAnno = propLdapAnno;
        else {
            AnnotatedConfigurableProperty pseudoProperty = property.getPseudoPropertyForCollectionElement();
            if (pseudoProperty != null)
                classLdapAnno = (LDAP) pseudoProperty.getRawClass().getAnnotation(LDAP.class);
        }

        if (propLdapAnno == null || classLdapAnno == null) return false;

        return propLdapAnno.noContainerNode() ||
                classLdapAnno.noContainerNode();
    }

    static String getLDAPPropertyName(AnnotatedConfigurableProperty property) throws ConfigurationException {
        LDAP ldapAnno = property.getAnnotation(LDAP.class);
        if (ldapAnno != null) {
            if (!ldapAnno.overriddenName().equals("")) {
                return ldapAnno.overriddenName();
            } else {
                return property.getAnnotatedName();
            }
        } else {
            return property.getAnnotatedName();
        }
    }

    public static String dnOf(String parentDN, String attrID, String attrValue) {
        return attrID + '=' + attrValue.replace(",", "\\,") + ',' + parentDN;
    }

    static ArrayList<String> extractObjectClasses(AnnotatedConfigurableProperty property) {
        return new ArrayList<String>(Arrays.asList(property.getAnnotation(LDAP.class).objectClasses()));
    }

    static String refToLdapDN(String ref, LdapConfigurationStorage ldapStorage) {
        return refToLdapDN(ref, ldapStorage, new BooleanContainer());
    }

    static String refToLdapDN(String ref, LdapConfigurationStorage ldapStorage, BooleanContainer dnIsKillableWrapper) {

        boolean dnIsKillable = true;
        try {
            Iterator<Map<String, Object>> pathItemIter = ConfigNodeUtil.parseReference(ref).iterator();

            // first pathitem is always dicomconfigroot
            if (!pathItemIter.next().get("$name").equals("dicomConfigurationRoot"))
                throw new IllegalArgumentException("No dicom config root");

            String dn = "cn=DICOM Configuration," + ldapStorage.getBaseDN();

            Class currentClass = CommonDicomConfiguration.DicomConfigurationRootNode.class;
            while (pathItemIter.hasNext()) {

                List<AnnotatedConfigurableProperty> properties = ConfigIterators.getAllConfigurableFieldsAndSetterParameters(currentClass);

                Map<String, Object> pathItem = pathItemIter.next();

                Object name = pathItem.get("$name");

                for (AnnotatedConfigurableProperty property : properties) {
                    if (name.equals(property.getAnnotatedName())) {

                        // in any case
                        if (!isNoContainerNode(property)) {
                            dn = dnOf(dn, "cn", getLDAPPropertyName(property));
                            dnIsKillable = true;
                        } else
                            dnIsKillable = false;

                        // for collections/maps, analyze predicates
                        if (property.isCollectionOfConfObjects() || property.isMapOfConfObjects() || property.isArrayOfConfObjects()) {

                            // remove $name, because it is the collection name in this pathitem
                            pathItem.remove("$name");

                            // add rdn
                            List<String> rdnItems = new ArrayList<String>();
                            while (true) {
                                // collect rdn pieces
                                for (Map.Entry<String, Object> entry : pathItem.entrySet()) {


                                    // skip wildcard - expect predicates
                                    if (entry.getKey().equals("$name") && entry.getValue().equals("*")) {
                                        if (pathItem.size() == 1)
                                            throw new IllegalArgumentException("Wildcard without predicates is not allowed in references");
                                        continue;
                                    }

                                    // add the rest of predicates to rdn
                                    String df = null;
                                    if (entry.getKey().equals("@name") || entry.getKey().equals("$name"))
                                        df = getDistinguishingFieldForCollectionElement(property);
                                    else
                                        df = entry.getKey();
                                    rdnItems.add(df + "=" + escapeStringToLdap(entry.getValue()));
                                }


                                if (!rdnItems.isEmpty()) {
                                    // if rdn found, proceed
                                    dn = StringUtils.join(rdnItems, "+") + "," + dn;
                                    dnIsKillable = true;
                                    break;
                                } else if (!pathItemIter.hasNext()) {
                                    // rdn not found, path is over,.. nothing to look for
                                    break;
                                } else {
                                    // get next path item and collect the rdn there
                                    pathItem = pathItemIter.next();
                                }
                            }

                            currentClass = property.getPseudoPropertyForCollectionElement().getRawClass();
                        }

                        // ConfObject
                        if (property.isConfObject())
                            currentClass = property.getRawClass();

                    }
                }

                // handle extensions
                if (currentClass.equals(Device.class)) {
                    if (name.equals("deviceExtensions") ||
                            name.equals("aeExtensions") ||
                            name.equals("hl7AppExtensions")) {

                        dnIsKillable = false;

                        String extName;
                        if (pathItem.containsKey("@name")) {
                            extName = pathItem.get("@name").toString();
                        } else {
                            pathItem = pathItemIter.next();
                            extName = pathItem.get("$name").toString();
                        }

                        currentClass = getExtensionClassBySimpleName(ldapStorage, extName);

                        LDAP ldapanno = (LDAP) currentClass.getAnnotation(LDAP.class);
                        if (ldapanno == null || !ldapanno.noContainerNode()) {
                            dn = dnOf(dn, "cn", currentClass.getSimpleName());
                            dnIsKillable = true;
                        }

                    }
                }

            }

            dnIsKillableWrapper.setKillable(dnIsKillable);
            return dn;
        } catch (Exception e) {
            throw new RuntimeException("Cannot transform reference " + ref + " to LDAP dn", e);
        }
    }

    public static ArrayList<String> extractObjectClasses(Class configurableClass) {
        LDAP classLdapAnno = (LDAP) configurableClass.getAnnotation(LDAP.class);
        ArrayList<String> objectClasses;
        if (classLdapAnno != null)
            objectClasses = new ArrayList<String>(Arrays.asList(classLdapAnno.objectClasses()));
        else objectClasses = new ArrayList<String>();
        return objectClasses;
    }


    public static String connectionLdapDnToRef(String dn, LdapConfigurationStorage ldapStorage) {
        try {

            String baseDN = ldapStorage.getBaseDN();
            List<Rdn> rdns = LdapConfigUtils.getNonBaseRdns(dn, baseDN);

            if (!rdns.get(0).toString().equals("cn=DICOM Configuration") ||
                    !rdns.get(1).toString().equals("cn=Devices") ||
                    !rdns.get(2).getType().equals("dicomDeviceName")
                    ) throw new IllegalArgumentException("Invalid dn " + dn);

            String deviceName = (String) rdns.get(2).getValue();


            Attributes attributes = rdns.get(3).toAttributes();
            ArrayList<String> predicates = new ArrayList<String>();

            if (attributes.get("cn") != null)
                predicates.add("cn='" + escapeApos(attributes.get("cn").get().toString()) + "'");

            if (attributes.get("dicomHostname") != null)
                predicates.add("dicomHostname='" + attributes.get("dicomHostname").get().toString() + "'");

            if (attributes.get("dicomPort") != null)
                predicates.add("dicomPort='" + attributes.get("dicomPort").get().toString() + "'");

            return "/dicomConfigurationRoot/dicomDevicesRoot/*[dicomDeviceName='" + escapeStringFromLdap(deviceName) + "']/dicomConnection[" + StringUtils.join(predicates, ",") + "]";

        } catch (javax.naming.NamingException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private static List<Rdn> getNonBaseRdns(String dn, String baseDN) throws InvalidNameException {
        LdapName baseDnName = new LdapName(baseDN);
        LdapName name = new LdapName(dn);

        // ffd to the interesting part
        List<Rdn> rdns = new LinkedList<Rdn>(name.getRdns());
        Iterator<Rdn> nameIter = rdns.iterator();
        Iterator<Rdn> baseIter = baseDnName.getRdns().iterator();
        while (baseIter.hasNext() && baseIter.next().equals(nameIter.next()))
            nameIter.remove();
        if (baseIter.hasNext())
            throw new IllegalArgumentException("Dn " + dn + " does not match base dn " + baseDnName);
        return rdns;
    }

    private static String escapeStringToLdap(Object value) {

        return ConfigNodeUtil.unescapeApos(value.toString()).replace(",", "\\,");
    }

    private static String escapeStringFromLdap(Object value) {

        return escapeApos(value.toString()).replace("\\,", ",");
    }

    static String LdapDNToRef(String ldapDn) {
        return ldapDn;
    }

    static Class<?> getExtensionClassBySimpleName(LdapConfigurationStorage configurationStorage, String extensionSimpleName) throws ClassNotFoundException {

        List<Class<?>> extensionClasses = configurationStorage.getAllExtensionClasses();

        for (Class<?> aClass : extensionClasses) {
            if (aClass.getSimpleName().equals(extensionSimpleName)) return aClass;
        }

        throw new ClassNotFoundException();
    }

    static Object readNode(LdapConfigurationStorage ldapConfigurationStorage, String dn, Class configurableClass) throws ConfigurationException, NamingException {
        ArrayList<String> objectClasses = extractObjectClasses(configurableClass);

        Attributes attributes;
        try {
            attributes = ldapConfigurationStorage.getLdapCtx().getAttributes(dn);
        } catch (NameNotFoundException noname) {
            attributes = null;
        }

        Map<String, Object> configNode = new HashMap<String, Object>();
        for (AnnotatedConfigurableProperty property : ConfigIterators.getAllConfigurableFieldsAndSetterParameters(configurableClass)) {


            //TODO:use storedAsReference three times below

            // map
            if (property.isMapOfConfObjects()) {
                String subDn = getSubDn(dn, property);

                Map<String, Object> map = new HashMap<String, Object>();
                try {
                    NamingEnumeration<SearchResult> enumeration = searchForCollectionElements(ldapConfigurationStorage, subDn, property);
                    while (enumeration.hasMore()) {
                        SearchResult res = enumeration.next();
                        String distField = getDistinguishingFieldForCollectionElement(property);
                        Attributes resAttributes = res.getAttributes();
                        String key = (String) resAttributes.get(distField).get();

                        // check if it is a primitive or a custom representation, e.g a ref
                        if (property.getAnnotation(LDAP.class).storedAsReference()) {
                            map.put(key, resAttributes.get(getLDAPPropertyName(property)).get());
                        } else {
                            Object value = readNode(ldapConfigurationStorage, res.getName() + "," + subDn, property.getPseudoPropertyForCollectionElement().getRawClass());
                            map.put(key, value);
                        }
                    }
                    configNode.put(property.getAnnotatedName(), map);
                } catch (NameNotFoundException e) {
                    //noop
                }

                continue;

            }

            // nested
            if (property.isConfObject()) {

                // custom rep?
                if (property.getAnnotation(LDAP.class).storedAsReference()) {
                    if (attributes != null) {
                        configNode.put(property.getAnnotatedName(),attributes.get(getLDAPPropertyName(property)).get());
                    }
                } else {
                    String subDn = getSubDn(dn, property);
                    configNode.put(property.getAnnotatedName(), readNode(ldapConfigurationStorage, subDn, property.getRawClass()));
                }

                continue;
            }

            // collection with confObjects
            if (property.isArrayOfConfObjects()
                    || property.isCollectionOfConfObjects()
                    && !property.getAnnotation(ConfigurableProperty.class).collectionOfReferences()
                    && !property.getAnnotation(LDAP.class).storedAsReference()) {

                Class elemClass = property.getPseudoPropertyForCollectionElement().getRawClass();
                String subDn = getSubDn(dn, property);

                try {
                    NamingEnumeration<SearchResult> enumeration = searchForCollectionElements(ldapConfigurationStorage, subDn, property);
                    ArrayList<Object> list = new ArrayList<Object>();
                    while (enumeration.hasMore()) {
                        SearchResult next = enumeration.next();
                        // check if it is a primitive or a custom representation, e.g a ref
                        list.add(readNode(ldapConfigurationStorage, next.getName() + "," + dn, elemClass));
                    }

                    configNode.put(property.getAnnotatedName(), list);
                } catch (NameNotFoundException e) {
                    //noop
                }
                continue;
            }

            // primitive collection or custom representation collection
            if ((Collection.class.isAssignableFrom(property.getRawClass()) || property.getRawClass().isArray()) && attributes != null) {
                Attribute attribute = attributes.get(getLDAPPropertyName(property));

                if (attribute == null) continue;

                ArrayList<Object> list = new ArrayList<Object>();

                // special case with references
                for (int i = 0; i < attribute.size(); i++)
                    if (property.getAnnotation(ConfigurableProperty.class).collectionOfReferences() &&
                            property.getPseudoPropertyForCollectionElement().getRawClass().equals(Connection.class)) {
                        list.add(connectionLdapDnToRef((String) attribute.get(i), ldapConfigurationStorage));
                    } else {
                        list.add(attribute.get(i));
                    }
                configNode.put(property.getAnnotatedName(), list);
                continue;
            }

            if (attributes != null) {
                Attribute attribute = attributes.get(getLDAPPropertyName(property));
                if (attribute != null)
                    configNode.put(property.getAnnotatedName(), attribute.get().toString());
            }

        }

        if (configurableClass.equals(Device.class)) {
            ldapConfigurationStorage.fillExtension(dn, configNode, "deviceExtensions");
        } else if (configurableClass.equals(ApplicationEntity.class)) {
            ldapConfigurationStorage.fillExtension(dn, configNode, "aeExtensions");
        } else if (configurableClass.equals(HL7Application.class)) {
            ldapConfigurationStorage.fillExtension(dn, configNode, "hl7AppExtensions");
        }

        return configNode;
    }

    public static String getSubDn(String dn, AnnotatedConfigurableProperty property) throws ConfigurationException {
        String subDn;
        if (isNoContainerNode(property))
            subDn = dn;
        else
            subDn = dnOf(dn, "cn", getLDAPPropertyName(property));
        return subDn;
    }

    static NamingEnumeration<SearchResult> searchSubcontextWithClass(LdapConfigurationStorage ldapConfigurationStorage, String childObjClass, String dn) throws NamingException {
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(1);
        ctls.setReturningObjFlag(false);
        return ldapConfigurationStorage.getLdapCtx().search(dn, "(objectclass=" + childObjClass + ")", ctls);
    }

    protected static NamingEnumeration<SearchResult> searchForCollectionElements(LdapConfigurationStorage ldapConfigurationStorage, String dn, AnnotatedConfigurableProperty property) throws NamingException, ConfigurationException {
        NamingEnumeration<SearchResult> enumeration;
        Class aClass = property.getPseudoPropertyForCollectionElement().getRawClass();
        try {
            enumeration = searchSubcontextWithClass(ldapConfigurationStorage, extractObjectClasses(aClass).get(0), dn);
        } catch (IndexOutOfBoundsException e) {
            throw new ConfigurationException("No object class defined for class " + aClass, e);
        }
        return enumeration;
    }

    protected static class BooleanContainer {
        private boolean killable;

        public void setKillable(boolean killable) {
            this.killable = killable;
        }

        public boolean isKillable() {
            return killable;
        }
    }
}
