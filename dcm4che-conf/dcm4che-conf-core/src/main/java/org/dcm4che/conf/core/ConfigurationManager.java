package org.dcm4che.conf.core;

/**
 * in progress...
 */
public class ConfigurationManager {

    public void setConstantNodeForBeanClass(Class clazz, String path) {

    };

    /**
     * Should generate config path based on context
     * @param <T> Context type
     */
    public static interface ConfigPathFactory<T>  {
        String getPath(Class clazz, T context);
    }

    public void setPathFactoryForBeanClass(Class clazz, String path) {

    }

    /**
     * dicomConfigurationRoot/dicomDeviceRoot/* ~ Device.class
     */

    // pojofrombackend
    // - apply defaults
    // - normalize

    // jsonfrombackend
    // normalize
    // apply defaults

    // persistfromjson
    // normalize
    // defaults filter

}
