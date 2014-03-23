/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2014
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4che3.conf.ldap.generic;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.dcm4che3.conf.ldap.LdapDicomConfiguration;
import org.dcm4che3.conf.ldap.LdapUtils;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigReader;

public class LdapConfigReader implements ConfigReader {
    private final Attributes attrs;

    /**
     * readCollection won't work with this constructor
     * 
     * @param attrs
     */
    @Deprecated
    public LdapConfigReader(Attributes attrs) {
        this.attrs = attrs;
    }

    public LdapConfigReader(Attributes attrs, String dn, LdapDicomConfiguration config) {
        super();
        this.attrs = attrs;
        this.dn = dn;
        this.config = config;
    }

    /**
     * DN with the whatever extension included
     */
    private String dn;
    private LdapDicomConfiguration config;

    @Override
    public String[] asStringArray(String propName) throws NamingException {
        return LdapUtils.stringArray(attrs.get(propName));
    }

    @Override
    public int[] asIntArray(String propName) throws NamingException {
        return LdapUtils.intArray(attrs.get(propName));
    }

    @Override
    public int asInt(String propName, String def) throws NamingException {
        return LdapUtils.intValue(attrs.get(propName), Integer.parseInt(def));
    }

    @Override
    public String asString(String propName, String def) throws NamingException {
        return LdapUtils.stringValue(attrs.get(propName), def);
    }

    @Override
    public boolean asBoolean(String propName, String def) throws NamingException {
        return LdapUtils.booleanValue(attrs.get(propName), Boolean.parseBoolean(def));
    }

    public ConfigReader getChild(String propName) {
        String folderDn = LdapUtils.dnOf("cn", propName, dn);
        return new LdapConfigReader(attrs, folderDn, config);
    }
    
    @Override
    public Map<String, ConfigReader> readCollection(String propName, String keyName) throws ConfigurationException {

        try {

            if (dn == null)
                throw new NamingException("dn was not provided for readCollection");


            // do ldap search
            NamingEnumeration<SearchResult> ne =
            // get 'folder' (cn=propName), then lookup all its children with
            // keyname=*

            config.search(dn, String.format("(%s=*)", keyName));
            Map<String, ConfigReader> map = new HashMap<String, ConfigReader>();

            try {

                while (ne.hasMore()) {
                    SearchResult sr = ne.next();

                    Attributes attrs = sr.getAttributes();
                    String keyValue = LdapUtils.stringValue(attrs.get(keyName), null);

                    if (keyValue == null)
                        throw new NamingException("A key attribute for a map cannot be read");

                    String collectionElementDn = LdapUtils.dnOf(keyName, keyValue, dn);

                    // generate a reader for nested node (i.e. map entry)
                    map.put(keyValue, new LdapConfigReader(attrs, collectionElementDn, config));
                }

                return map;

            } finally {
                LdapUtils.safeClose(ne);
            }
        } catch (NamingException e) {
            throw new ConfigurationException(e);
        }
    }
}