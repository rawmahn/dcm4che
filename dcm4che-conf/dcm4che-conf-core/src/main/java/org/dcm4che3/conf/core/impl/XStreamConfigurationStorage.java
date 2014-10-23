package org.dcm4che3.conf.core.impl;

import com.thoughtworks.xstream.XStream;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.conf.api.ConfigurationException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by player on 07-Oct-14.
 */
public class XStreamConfigurationStorage implements Configuration {

    XStream xstream;
    String fileName;

    public XStreamConfigurationStorage(String fileName) {
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
            return (Map<String, Object>) xstream.fromXML(new BufferedReader(new FileReader(fileName)));
        } catch (FileNotFoundException e) {
            return new HashMap<String, Object>();
        }
    }

    @Override
    public Object getConfigurationNode(String path) throws ConfigurationException {
        return ConfigNodeUtil.getNode(getConfigurationRoot(), path);
    }

    @Override
    public Class getConfigurationNodeClass(String path) throws ConfigurationException, ClassNotFoundException {
        try {
            String clazz = (String) ((Map<String, Object>) ConfigNodeUtil.getNode(getConfigurationRoot(), path)).get("#class");
            return (clazz == null ? null : Class.forName(clazz));
        } catch (RuntimeException e) {
            throw new ConfigurationException("Cannot retrieve class for node "+path,e);
        }
    }


    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {
        try {
            Map<String, Object> configurationRoot = getConfigurationRoot();
            Map<String, Object> node = (Map<String, Object>) ConfigNodeUtil.getNode(configurationRoot, path);

            if (configurableClass != null)
                configNode.put("#class", configurableClass.getName());

            if (!path.equals("/"))
                ConfigNodeUtil.replaceNode(configurationRoot, path, configNode); else
                configurationRoot = (Map<String, Object>) configNode;


            PrintWriter out = new PrintWriter(fileName, "UTF-8");
            xstream.toXML(configurationRoot, out);
            out.close();


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
        persistNode("/",configurationRoot, null);
    }

    @Override
    public java.util.Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException {
        return ConfigNodeUtil.search(getConfigurationRoot(), liteXPathExpression);
    }
}
