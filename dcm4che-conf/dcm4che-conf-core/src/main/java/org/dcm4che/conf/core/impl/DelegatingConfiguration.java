package org.dcm4che.conf.core.impl;

import org.dcm4che.conf.core.Configuration;
import org.dcm4che3.conf.api.ConfigurationException;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Roman K
 */
public class DelegatingConfiguration implements Configuration {

    protected Configuration delegate;

    public DelegatingConfiguration(Configuration delegate) {
        this.delegate = delegate;
    }

    @Override
    public Map<String, Object> getConfigurationRoot() throws ConfigurationException {
        return delegate.getConfigurationRoot();
    }

    @Override
    public Object getConfigurationNode(String path) throws ConfigurationException {
        return delegate.getConfigurationNode(path);
    }

    @Override
    public Class getConfigurationNodeClass(String path) throws ConfigurationException, ClassNotFoundException {
        return delegate.getConfigurationNodeClass(path);
    }

    @Override
    public boolean nodeExists(String path) throws ConfigurationException {
        return delegate.nodeExists(path);
    }

    @Override
    public void persistNode(String path, Object configNode, Class configurableClass) throws ConfigurationException {
        delegate.persistNode(path, configNode, configurableClass);
    }

    @Override
    public void refreshNode(String path) throws ConfigurationException {
        delegate.refreshNode(path);
    }

    @Override
    public void removeNode(String path) throws ConfigurationException {
        delegate.removeNode(path);
    }

    @Override
    public Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException {
        return delegate.search(liteXPathExpression);
    }
}
