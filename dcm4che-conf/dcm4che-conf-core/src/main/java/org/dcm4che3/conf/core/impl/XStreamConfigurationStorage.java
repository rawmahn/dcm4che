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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by player on 07-Oct-14.
 */
public class XStreamConfigurationStorage implements Configuration {

    XStream xstream;
    String fileName;
    private boolean storeAsJSON;


    public XStreamConfigurationStorage(String fileName, boolean storeAsJSON) {
        this.storeAsJSON = storeAsJSON;
        this.xstream = new XStream();
        this.fileName = fileName;
    }

    @Override
    public boolean nodeExists(String path) throws ConfigurationException {
        return ConfigNodeUtil.nodeExists(getConfigurationRoot(), path);
    }

    @Override
    public Map<String, Object> getConfigurationRoot() throws ConfigurationException {
        try {
            if (storeAsJSON) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(new File(fileName), Map.class);
            } else
                return (Map<String, Object>) xstream.fromXML(new BufferedReader(new FileReader(fileName)));
        } catch (FileNotFoundException e) {
            return new HashMap<String, Object>();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Object getConfigurationNode(String path) throws ConfigurationException {
        Object node = ConfigNodeUtil.getNode(getConfigurationRoot(), path);
        return node;
    }

    @Override
    public Class getConfigurationNodeClass(String path) throws ConfigurationException, ClassNotFoundException {
        try {
            String clazz = (String) ((Map<String, Object>) ConfigNodeUtil.getNode(getConfigurationRoot(), path)).get("#class");
            return (clazz == null ? null : Class.forName(clazz));
        } catch (RuntimeException e) {
            throw new ConfigurationException("Cannot retrieve class for node " + path, e);
        }
    }


    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {
        try {
            Map<String, Object> configurationRoot = getConfigurationRoot();

            if (configurableClass != null)
                configNode.put("#class", configurableClass.getName());

            if (!path.equals("/")) {
                if (ConfigNodeUtil.nodeExists(configurationRoot, path)) {
                    for (String key : configNode.keySet())
                        ConfigNodeUtil.replaceNode(configurationRoot, ConfigNodeUtil.concat(path, key), configNode.get(key));
                } else
                    ConfigNodeUtil.replaceNode(configurationRoot, path, configNode);


            } else
                configurationRoot = (Map<String, Object>) configNode;

            if (storeAsJSON) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    File resultFile = new File(fileName);
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(resultFile, (Object) configurationRoot);
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
            } else {
                PrintWriter out = new PrintWriter(fileName, "UTF-8");
                xstream.toXML(configurationRoot, out);
                out.close();
            }

        } catch (FileNotFoundException e) {
            throw new ConfigurationException(e);
        } catch (UnsupportedEncodingException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void refreshNode(String path) {

    }

    @Override
    public void removeNode(String path) throws ConfigurationException {

        Map<String, Object> configurationRoot = getConfigurationRoot();
        ConfigNodeUtil.removeNode(configurationRoot, path);
        persistNode("/", configurationRoot, null);
    }

    @Override
    public Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException {
        return ConfigNodeUtil.search(getConfigurationRoot(), liteXPathExpression);
    }
}
