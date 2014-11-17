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
import org.dcm4che3.conf.core.api.LDAP;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author: Roman K
 */
public class LdapConfigUtils {
    // resolve map/collection key field
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
        if (ldapAnno == null || ldapAnno.overriddenName().equals("")) {
            return property.getAnnotatedName();
        } else {
            return ldapAnno.overriddenName();
        }
    }

    public static String dnOf(String parentDN, String attrID, String attrValue) {
        return attrID + '=' + attrValue.replace(",","\\,") + ',' + parentDN;
    }

    static ArrayList<String> getObjectClasses(AnnotatedConfigurableProperty property) {
        return new ArrayList<String>(Arrays.asList(property.getAnnotation(LDAP.class).objectClasses()));
    }
}
