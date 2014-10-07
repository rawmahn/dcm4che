package org.dcm4che.conf.core;

/**
 * @author Roman K
 */

import org.dcm4che3.conf.api.ConfigurationException;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Main class that is used to initialize annotated Java objects with settings fetched from a configuration backend.
 * These are mostly low-level access methods.
 */
public class BeanVitalizer {

    static <T> T newConfiguredInstance(Class<T> clazz, Map<String, Object> configNode) throws ConfigurationException {
        try {

            return configureInstance(clazz.newInstance(), configNode);

        } catch (InstantiationException e) {
            throw new ConfigurationException();
        } catch (IllegalAccessException e) {
            throw new ConfigurationException();
        }
    }

    /**
     * Scans for annotations in <i>object</i> and initializes all its properties from the provided configuration node.
     * @param object
     * @param configNode
     * @param <T>
     * @return
     */
    static <T> T configureInstance(T object, Map<String, Object> configNode) {
        return null;
    }




}
