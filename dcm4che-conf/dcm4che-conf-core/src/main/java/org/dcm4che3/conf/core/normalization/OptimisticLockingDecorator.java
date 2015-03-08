package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.DelegatingConfiguration;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by player on 08-Mar-15.
 */
@SuppressWarnings("unchecked")
public class OptimisticLockingDecorator extends DelegatingConfiguration {

    private String upperLockablePath;
    
    public OptimisticLockingDecorator(Configuration delegate, String upperLockablePath) {
        super(delegate);
        this.upperLockablePath = upperLockablePath;
    }



    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {

        Map<String, Object> nodeInStorage;
        try {
            nodeInStorage = (Map<String, Object>) delegate.getConfigurationNode(path, configurableClass);
        } catch (ClassCastException e) {
            super.persistNode(path, configNode, configurableClass);
        }
        
        
        


    }

    @Override
    public Object getConfigurationNode(String path, Class configurableClass) throws ConfigurationException {


        
         

        
        
        MessageDigest cript = null;
        try {
            cript = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new ConfigurationException(e);
        }

        cript.reset();
        //cript.update();


        return super.getConfigurationNode(path, configurableClass);
    }
}
