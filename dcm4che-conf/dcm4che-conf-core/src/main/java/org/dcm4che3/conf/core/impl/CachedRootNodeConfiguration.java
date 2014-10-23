package org.dcm4che3.conf.core.impl;

import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.impl.DelegatingConfiguration;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.conf.api.ConfigurationException;

import java.util.Map;

/**
 * Created by aprvf on 29/09/2014.
 */

public class CachedRootNodeConfiguration extends DelegatingConfiguration {



    private Map<String, Object> configurationRoot = null;

    public CachedRootNodeConfiguration(Configuration delegate) {
        super(delegate);
    }


    @Override
    public Map<String, Object> getConfigurationRoot() throws ConfigurationException {
        if (configurationRoot==null)
            configurationRoot = delegate.getConfigurationRoot();
        return configurationRoot;
    }

    /**
     * Return cached node
     * @param path
     * @return
     * @throws ConfigurationException
     */
    @Override
    public Object getConfigurationNode(String path) throws ConfigurationException {
        return ConfigNodeUtil.getNode(getConfigurationRoot(), path);
    }

    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {
        ConfigNodeUtil.replaceNode(getConfigurationRoot(), path, configNode);
        delegate.persistNode(path, configNode, configurableClass);
    }

    @Override
    public void refreshNode(String path) throws ConfigurationException {
        ConfigNodeUtil.replaceNode(getConfigurationRoot(), path, delegate.getConfigurationNode(path));
    }

    @Override
    public void removeNode(String path) throws ConfigurationException {
        delegate.removeNode(path);
        ConfigNodeUtil.removeNode(getConfigurationRoot(), path);
    }
}
