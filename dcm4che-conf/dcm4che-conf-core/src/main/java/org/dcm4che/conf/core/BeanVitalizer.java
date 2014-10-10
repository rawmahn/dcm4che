package org.dcm4che.conf.core;

/**
 * @author Roman K
 */

import org.dcm4che.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che.conf.core.adapters.ReflectiveAdapter;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che.conf.core.adapters.ConfigTypeAdapter;
import org.dcm4che3.conf.api.generic.ConfigurableClass;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class that is used to initialize annotated Java objects with settings fetched from a configuration backend.
 * These are mostly low-level access methods.
 */
public class BeanVitalizer {


    private Map<Class, Object> contextMap = new HashMap<Class, Object>();
    private HashMap<Class, ConfigTypeAdapter> customConfigTypeAdapters= new HashMap<Class, ConfigTypeAdapter>();

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

    @SuppressWarnings("unchecked")
    public <T> ConfigTypeAdapter<T, ?> lookupTypeAdapter(Type type) throws ConfigurationException {

        // for now we don't expect any other cases
        Class clazz = (Class) type;

        ConfigTypeAdapter<T, ?> adapter = null;

        // first check for a custom adapter
        adapter = customConfigTypeAdapters.get(clazz);
        if (adapter != null) return adapter;

        // if it is a config class, use reflective adapter
        if (clazz.getAnnotation(ConfigurableClass.class) != null)
            adapter = new ReflectiveAdapter(clazz);
        else if (clazz.isArray())
            adapter = (ConfigTypeAdapter<T, ?>) new DefaultConfigTypeAdapters.ArrayTypeAdapter();
        else if (clazz.isEnum())
            adapter = DefaultConfigTypeAdapters.get(Enum.class);
        else
            adapter = DefaultConfigTypeAdapters.get(clazz);

        if (adapter == null)
            throw new ConfigurationException("TypeAdapter not found for class " + clazz.getName());

        return adapter;
    }

    /**
     * Register any context data needed by custom ConfigTypeAdapters
     * @return
     */
    public void registerContext(Class clazz, Object context) {
        this.contextMap.put(clazz, context);
    }

    public <T> T getContext(Class<T> clazz) {
        return (T) contextMap.get(clazz);
    }

    /**
     * Registers a custom type adapter for configurable properties for the specified class
     * @param clazz
     * @param typeAdapter
     */
    public void registerCustomConfigTypeAdapter(Class clazz, ConfigTypeAdapter typeAdapter) {
        customConfigTypeAdapters.put(clazz, typeAdapter);
    }
}
