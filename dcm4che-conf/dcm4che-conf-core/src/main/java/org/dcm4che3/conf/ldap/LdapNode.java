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
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.LDAP;
import org.dcm4che3.conf.core.util.ConfigIterators;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.hl7.HL7Application;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import java.util.*;

/**
 * @author: Roman K
 */
public class LdapNode {


    private LdapNode parent;
    private String dn;
    private Collection<String> objectClasses = new ArrayList<String>();
    private Attributes attributes = new BasicAttributes();
    private Collection<LdapNode> children = new ArrayList<LdapNode>();

    private LdapConfigurationStorage ldapConfigurationStorage;

    public LdapNode() {
    }

    public LdapNode(LdapConfigurationStorage ldapConfigurationStorage) {

        this.ldapConfigurationStorage = ldapConfigurationStorage;
    }

    public LdapConfigurationStorage getLdapConfigurationStorage() {
        return ldapConfigurationStorage == null && parent != null ? parent.getLdapConfigurationStorage() : ldapConfigurationStorage;
    }

    private String getBaseDn() {
        if (parent == null) return dn;
        else return parent.getBaseDn();
    }


    //TODO hardcoded. Implement!!!
    String refToLdapDN(String ref, AnnotatedConfigurableProperty property) {
        try {
            Class clazz = null;
            if (property.getAnnotation(ConfigurableProperty.class).collectionOfReferences())
                clazz = property.getPseudoPropertyForGenericsParamater(0).getRawClass();


            if (Connection.class.isAssignableFrom(clazz)) {
                List<Map<String, Object>> props = ConfigNodeUtil.parseReference(ref);

                String deviceName = (String) props.get(2).get("dicomDeviceName");
                if (deviceName == null) deviceName = (String) props.get(2).get("$name");

                boolean valid = props.get(0).get("$name").equals("dicomConfigurationRoot") &&
                        props.get(1).get("$name").equals("dicomDevicesRoot") &&
                        deviceName != null &&
                        props.get(3).get("$name").equals("dicomConnection");
                if (!valid) throw new RuntimeException("Path is invalid");

                // diRRty hardcoding TODO implement
                return "cn=" + props.get(3).get("cn") + ",dicomDeviceName=" + deviceName + ",cn=Devices,cn=DICOM Configuration" + getBaseDn();
            } else
                throw new RuntimeException("Not supported reference type " + clazz);
        } catch (Exception e) {
            throw new RuntimeException("Cannot transform reference " + ref + " to LDAP dn", e);
        }
    }

    String LdapDNToRef(String ldapDn) {
        return ldapDn;
    }

    public LdapNode getParent() {
        return parent;
    }

    public void setParent(LdapNode parent) {
        this.parent = parent;
        parent.getChildren().add(this);
    }

    /**
     * Responsible for
     * objectClasses, attributes, children
     * <p/>
     * NOT responsible for
     * parent, dn
     *
     * @param configNode
     * @param configurableClass
     * @throws org.dcm4che3.conf.api.ConfigurationException
     */
    public void populate(Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {

        if (configurableClass == null ||
                configurableClass.getAnnotation(ConfigurableClass.class) == null)
            throw new ConfigurationException("Unexpected error - class '" + configurableClass == null ? null : configurableClass.getName() + "' is not a configurable class");

        // fill in objectclasses
        LDAP classLdapAnno = (LDAP) configurableClass.getAnnotation(LDAP.class);
        if (classLdapAnno != null)
            getObjectClasses().addAll(new ArrayList<String>(Arrays.asList(classLdapAnno.objectClasses())));

        // iterate over configurable properties
        List<AnnotatedConfigurableProperty> properties = ConfigIterators.getAllConfigurableFieldsAndSetterParameters(configurableClass);
        for (AnnotatedConfigurableProperty property : properties) {

            Object propertyConfigNode = configNode.get(property.getAnnotatedName());

            if (propertyConfigNode == null) continue;

            // map of anything
            if (Map.class.isAssignableFrom(property.getRawClass())) {
                LdapNode thisParent = makeLdapCollectionNode(property);
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) propertyConfigNode).entrySet()) {
                    LdapNode elementNode = thisParent.makeLdapElementNode(property, entry.getKey());

                    // now if it is a conf obj, not primitive or custom representation, go deeper
                    if (property.isMapOfConfObjects() && entry.getValue() instanceof Map) {
                        elementNode.populate((Map<String, Object>) entry.getValue(), property.getPseudoPropertyForCollectionElement().getRawClass());
                        continue;
                    }

                    //otherwise, make this mock with value
                    elementNode.setObjectClasses(Arrays.asList(property.getAnnotation(LDAP.class).mapEntryObjectClass()));
                    elementNode.getAttributes().put(property.getAnnotation(LDAP.class).mapValueAttribute(), entry.getValue());

                }
                continue;
            }

            //collection/array of confobjects, (but not custom representations)
            if (property.isArrayOfConfObjects() || property.isCollectionOfConfObjects()) {
                Iterator iterator = ((Collection) propertyConfigNode).iterator();
                // collection not empty and first element is not a custom rep
                if (iterator.hasNext() && iterator.next() instanceof Map) {
                    LdapNode thisParent = makeLdapCollectionNode(property);
                    for (Map<String, Object> o : ((Collection<Map<String, Object>>) propertyConfigNode)) {
                        LdapNode elementNode = thisParent.makeLdapElementNode(property, (String) o.get(LdapConfigUtils.getDistinguishingFieldForCollectionElement(property)));
                        elementNode.populate(o, property.getPseudoPropertyForCollectionElement().getRawClass());
                    }
                    continue;
                }
            }

            // nested conf object, not custom representation
            if (property.getRawClass().getAnnotation(ConfigurableClass.class) != null)
                if (propertyConfigNode instanceof Map) {
                    LdapNode nestedNode;
                    if (LdapConfigUtils.isNoContainerNode(property)) nestedNode = this;
                    else {
                        nestedNode = new LdapNode();
                        nestedNode.setDn(LdapConfigUtils.dnOf(getDn(), LdapConfigUtils.getDistinguishingField(property), LdapConfigUtils.getLDAPPropertyName(property)));
                        nestedNode.getAttributes().put(LdapConfigUtils.getDistinguishingField(property), LdapConfigUtils.getLDAPPropertyName(property));
                    }
                    nestedNode.populate((Map<String, Object>) propertyConfigNode, property.getRawClass());
                    continue;
                }

            // any other array/collection
            if (propertyConfigNode instanceof Collection) {

                Collection<Object> collection = (Collection<Object>) propertyConfigNode;

                // special case - boolean based enumSet
                LDAP ldapAnno = property.getAnnotation(LDAP.class);
                if (ldapAnno != null && ldapAnno.booleanBasedEnumStorageOptions().length > 0) {
                    int i = 0;
                    for (String enumStorageOption : ldapAnno.booleanBasedEnumStorageOptions()) {
                        getAttributes().put(enumStorageOption, collection.contains(i) ? "TRUE" : "FALSE");
                        i++;
                    }
                    continue;
                }

                // store only if not empty
                if (collection.isEmpty()) continue;

                BasicAttribute attribute = new BasicAttribute(LdapConfigUtils.getLDAPPropertyName(property));

                for (Object o : collection) {
                    String attrVal = o.toString();

                    // handle refs
                    if (property.getAnnotation(ConfigurableProperty.class).collectionOfReferences())
                        attrVal = refToLdapDN(attrVal, property);

                    attribute.add(attrVal);
                }

                getAttributes().put(attribute);
                continue;
            }

            // regular attribute
            if (propertyConfigNode instanceof Boolean)
                getAttributes().put(LdapConfigUtils.getLDAPPropertyName(property), (Boolean) propertyConfigNode ? "TRUE" : "FALSE");
            else
                getAttributes().put(LdapConfigUtils.getLDAPPropertyName(property), propertyConfigNode.toString());

        }

        // hardcoded workarounds for extensions
        if (configurableClass.equals(Device.class)) {
            fillInExtensions(configNode, "deviceExtensions");
        } else if (configurableClass.equals(ApplicationEntity.class)) {
            fillInExtensions(configNode, "aeExtensions");
        } else if (configurableClass.equals(HL7Application.class)) {
            fillInExtensions(configNode, "hl7AppExtensions");
        }


        /**
         * Workaround for objectClass dcmArchiveExtension
         * TODO: inspect. dicomArchiveDevice added due to presence of attributes from there.
         * extensions are added later, so this objectclass is not there while persisting the device alone.
         * Adding it to Device does not work since there are some required attributes
         * */

        String[] archiveDeviceExtensionPropNames = {
                "dcmIncorrectWorklistEntrySelectedCode",
                "dcmFuzzyAlgorithmClass",
                "dcmHostNameAEResolution",
                "dcmConfigurationStaleTimeout",
                "dcmDeIdentifyLogs",
                "dcmUpdateDbRetries",
                "dcmWadoAttributesStaleTimeout",
                "dcmRejectedObjectsCleanUpPollInterval",
                "dcmRejectedObjectsCleanUpMaxNumberOfDeletes",
                "dcmMPPSEmulationPollInterval"};


        boolean isArchiveExtension = false;
        for (String propName : archiveDeviceExtensionPropNames)
            if (configNode.containsKey(propName) && !getObjectClasses().contains(propName))
                isArchiveExtension = true;

/*
        if (isArchiveExtension) {
            if (!getObjectClasses().contains("dcmArchiveDevice"))
                getObjectClasses().add("dcmArchiveDevice");
        }
*/

    }

    private void fillInExtensions(Map<String, Object> configNode, String whichExtensions) throws ConfigurationException {
        Map<String, Map<String, Object>> extensions = (Map<String, Map<String, Object>>) configNode.get(whichExtensions);
        if (extensions != null) {
            for (Map.Entry<String, Map<String, Object>> ext : extensions.entrySet()) {
                Class<?> extClass = null;
                try {
                    extClass = getExtensionClassBySimpleName(ext);
                } catch (Exception e) {
                    throw new ConfigurationException("Cannot find extension class " + ext.getKey(), e);
                }

                LdapNode extNode = this;
                LDAP ldapAnno = (LDAP) extClass.getAnnotation(LDAP.class);
                if (ldapAnno == null || !ldapAnno.noContainerNode()) {
                    extNode = new LdapNode();
                    String attrID = ldapAnno == null ? LDAP.DEFAULT_DISTINGUISHING_FIELD : ldapAnno.distinguishingField();
                    extNode.setDn(LdapConfigUtils.dnOf(getDn(), attrID, ext.getKey()));
                    extNode.getAttributes().put(attrID, ext.getKey());
                    extNode.setParent(this);
                }

                extNode.populate(ext.getValue(), extClass);
            }
        }
    }

    private Class<?> getExtensionClassBySimpleName(Map.Entry<String, Map<String, Object>> ext) throws ClassNotFoundException {

        List<Class<?>> extensionClasses = getLdapConfigurationStorage().getAllExtensionClasses();

        for (Class<?> aClass : extensionClasses) {
            if (aClass.getSimpleName().equals(ext.getKey())) return aClass;
        }

        throw new ClassNotFoundException();
    }

    private LdapNode makeLdapCollectionNode(AnnotatedConfigurableProperty property) throws ConfigurationException {

        LdapNode thisParent;

        if (LdapConfigUtils.isNoContainerNode(property)) {
            thisParent = this;
        } else {
            thisParent = new LdapNode();
            thisParent.setDn(LdapConfigUtils.dnOf(getDn(), "cn", LdapConfigUtils.getLDAPPropertyName(property)));

            LDAP annotation = property.getAnnotation(LDAP.class);
            if (annotation == null || annotation.objectClasses().length == 0)
                thisParent.getObjectClasses().add("dcmCollection");
            else
                thisParent.setObjectClasses(LdapConfigUtils.getObjectClasses(property));

            thisParent.setParent(this);
        }
        return thisParent;
    }

    private LdapNode makeLdapElementNode(AnnotatedConfigurableProperty property, String key) {
        LdapNode elementNode1 = new LdapNode();
        String elementDistinguishingField = LdapConfigUtils.getDistinguishingFieldForCollectionElement(property);
        elementNode1.setDn(LdapConfigUtils.dnOf(getDn(), elementDistinguishingField, key));
        elementNode1.getAttributes().put(elementDistinguishingField, key);
        elementNode1.setParent(this);
        return elementNode1;
    }

    //<editor-fold desc="getters/setters">
    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public Collection<String> getObjectClasses() {
        return objectClasses;
    }

    public void setObjectClasses(Collection<String> objectClasses) {
        this.objectClasses = objectClasses;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public Collection<LdapNode> getChildren() {
        return children;
    }

    public void setChildren(Collection<LdapNode> children) {
        this.children = children;
    }
    //</editor-fold>

}