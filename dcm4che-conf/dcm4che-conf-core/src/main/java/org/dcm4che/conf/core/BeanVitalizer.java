package org.dcm4che.conf.core;

/**
 * @author Roman K
 */

import org.dcm4che.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che.conf.core.adapters.ReflectiveAdapter;
import org.dcm4che.conf.core.util.ConfigIterators;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che.conf.core.adapters.ConfigTypeAdapter;

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


        for (ConfigIterators.AnnotatedProperty annoProp : ConfigIterators.getConfigurableProperties(object.getClass())) {
            annoProp.
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> ConfigTypeAdapter<T, ?> lookupTypeAdapter(Class<T> clazz) {

        ConfigTypeAdapter<T, ?> adapter = null;

        Map<Class, ConfigTypeAdapter> def = DefaultConfigTypeAdapters.get();

        // if it is a config class, use reflective adapter
        if (clazz.getAnnotation(ConfigClass.class) != null) {
            adapter = new ReflectiveAdapter(clazz);
        } else if (clazz.isArray()) {
            // if array
            adapter = (ConfigTypeAdapter<T, ?>) new DefaultConfigTypeAdapters.ArrayTypeAdapter();
        } else {
            if (clazz.isEnum()) {
                adapter = def.get(Enum.class);
            } else {
                adapter = def.get(clazz);
            }
        }

        return adapter;

    }


}
