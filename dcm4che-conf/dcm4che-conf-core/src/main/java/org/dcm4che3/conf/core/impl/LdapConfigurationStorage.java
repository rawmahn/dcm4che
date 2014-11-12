package org.dcm4che3.conf.core.impl;

import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.api.ConfigurationException;

import java.util.Iterator;
import java.util.Map;

/**
 * TODO: thread safety
 */
public class LdapConfigurationStorage implements Configuration {
    @Override
    public Map<String, Object> getConfigurationRoot() throws ConfigurationException {
        return null;
    }

    // special classes for root, app registrues,(or allow to register)

    @Override
    public Object getConfigurationNode(String path) throws ConfigurationException {
        // TODO: byte[],x509 to base64
        // transform references
        // special booleanBased EnumSet

        return null;
    }

    @Override
    public Class getConfigurationNodeClass(String path) throws ConfigurationException, ClassNotFoundException {
        return null;
    }

    @Override
    public boolean nodeExists(String path) throws ConfigurationException {
        return false;
    }

    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {
        // TODO: byte[], x509 from base64
        // dynamic dn generation for lists... maybe allow to use an extension
    }

    @Override
    public void refreshNode(String path) throws ConfigurationException {

    }

    @Override
    public void removeNode(String path) throws ConfigurationException {

    }

    @Override
    public Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException {
        return null;
    }
}
