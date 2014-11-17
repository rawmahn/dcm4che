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
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.dicom.CommonDicomConfiguration;

import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
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

    public synchronized void destroySubcontextWithChilds(String name) throws NamingException {
        NamingEnumeration list = ldapCtx.list(name);

        while (list.hasMore()) {
            this.destroySubcontextWithChilds(((NameClassPair) list.next()).getNameInNamespace());
        }

        ldapCtx.destroySubcontext(name);
    }

    private void persist(LdapNode ldapNode) {

        Iterator<String> iterator = ldapNode.getObjectClasses().iterator();
        BasicAttribute objectClass = new BasicAttribute("objectClass");
        while (iterator.hasNext()) objectClass.add(iterator.next());
        ldapNode.getAttributes().put(objectClass);

        try {
            destroySubcontextWithChilds(ldapNode.getDn());
        } catch (NameNotFoundException e1) {
            //noop
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        try {
            ldapCtx.createSubcontext(ldapNode.getDn(), ldapNode.getAttributes());
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        for (LdapNode child : ldapNode.getChildren()) persist(child);
    }

    @Override
    public Map<String, Object> getConfigurationRoot() throws ConfigurationException {
        return new HashMap<String, Object>();
    }

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
            ldapNode.setDn(LdapConfigUtils.dnOf(baseDN, "cn", "DICOM Configuration"));
            ldapNode.populate(configNode, CommonDicomConfiguration.DicomConfigurationRootNode.class);


            persist(ldapNode);

            // TODO: also fill in other parameters from the configNode according to 'partially overwritten' contract
        } else
            throw new RuntimeException("Not implemented yet");
    }


    /*void storeCtx() {
        try {
            ldapCtx.createSubcontext(dnOf(currentDN, classLDAPAnno.distinguishingField(), itemValue), attrs);
        } catch (NamingException e) {
            throw new ConfigurationException("Error while storing configuration for class " + configurableClass.getSimpleName());
        }

    }*/


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
