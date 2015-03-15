package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.DelegatingConfiguration;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.util.NodeTraverser;
import org.dcm4che3.conf.core.util.PathPattern;
import org.dcm4che3.conf.dicom.util.DicomNodeTraverser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by player on 08-Mar-15.
 */
@SuppressWarnings("unchecked")
public class OptimisticLockingDecorator extends DelegatingConfiguration {

    /**
     * Temporary solution to prevent any inconsistency. 
     * Should contains all path for which persistNode can be called.
     * In the long term, a write to a subnode should just trigger a 
     * full persist of a closest parent with an optimistic lock  
     */
    private List<PathPattern> allowedPersistPaths;
    
    public OptimisticLockingDecorator(Configuration delegate, List<PathPattern> allowedPersistPaths) {
        super(delegate);
        this.allowedPersistPaths = allowedPersistPaths;
    }

    


    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {

        
        
        // validate path
        boolean pathAllowed = false;
        for (PathPattern allowedPersistPath : allowedPersistPaths)
            try {
                allowedPersistPath.parse(path);
                pathAllowed = true;
            } catch (Exception e) {
            }
        
        if (!pathAllowed)
            throw new IllegalArgumentException("Path " + path + " is not allowed by optimistic lock handler.");


        //TODO: acquire pessimistic lock

        // merge the object
        Map<String, Object> nodeInStorage = null;
        try {
            nodeInStorage = (Map<String, Object>) delegate.getConfigurationNode(path, configurableClass);
        } catch (ClassCastException e) {
            super.persistNode(path, configNode, configurableClass);
            return;
        }


    }


    @Override
    public Object getConfigurationNode(String path, Class configurableClass) throws ConfigurationException {
        return super.getConfigurationNode(path, configurableClass);
    }
}
