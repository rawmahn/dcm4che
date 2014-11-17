package org.dcm4che3.conf.ldap;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.impl.CachedRootNodeConfiguration;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;

import java.util.Map;

/**
 * Created by player on 17-Nov-14.
 */
public class CachedRootNodeDicomConfigStorage extends CachedRootNodeConfiguration {
    public CachedRootNodeDicomConfigStorage(Configuration delegate) {
        super(delegate);
    }


    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {

        getConfigurationRoot();

        if (configurableClass != null)
            configNode.put("#class", configurableClass.getName());

        if (!path.equals("/")) {
            if (ConfigNodeUtil.nodeExists(configurationRoot, path)) {
                for (String key : configNode.keySet())
                    ConfigNodeUtil.replaceNode(configurationRoot, ConfigNodeUtil.concat(path, key), configNode.get(key));
            } else
                ConfigNodeUtil.replaceNode(configurationRoot, path, configNode);
        } else
            configurationRoot = configNode;


        persistRoot();
    }

    private void persistRoot() throws ConfigurationException {
        Object dicomConfigurationRoot = getConfigurationRoot().get("dicomConfigurationRoot");
        if (dicomConfigurationRoot != null)
            super.persistNode("/dicomConfigurationRoot", (Map<String, Object>) dicomConfigurationRoot, null);
    }

    @Override
    public void removeNode(String path) throws ConfigurationException {
        ConfigNodeUtil.removeNode(getConfigurationRoot(), path);
        persistRoot();
    }
}
