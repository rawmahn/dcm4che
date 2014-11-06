package org.dcm4che3.conf.core;

/**
 * @author Roman K
 */

import org.dcm4che3.conf.core.adapters.ArrayTypeAdapter;
import org.dcm4che3.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che3.conf.core.adapters.ReflectiveAdapter;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.adapters.ConfigTypeAdapter;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class that is used to initialize annotated Java objects with settings fetched from a configuration backend.
 * These are mostly low-level access methods.
 */
public class BeanVitalizer {


    //TODO: whats with references??? first take a look on persisting of certificates

    private Map<Class, Object> contextMap = new HashMap<Class, Object>();
    private Map<Class, ConfigTypeAdapter> customConfigTypeAdapters= new HashMap<Class, ConfigTypeAdapter>();
    private ConfigTypeAdapter referenceTypeAdapter;

    public void setReferenceTypeAdapter(ConfigTypeAdapter referenceTypeAdapter) {
        this.referenceTypeAdapter = referenceTypeAdapter;
    }

    public ConfigTypeAdapter getReferenceTypeAdapter() {
        return referenceTypeAdapter;
    }

    public <T> T newConfiguredInstance(Class<T> clazz, Map<String, Object> configNode) throws ConfigurationException {
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
    public <T> T configureInstance(T object, Map<String, Object> configNode) throws ConfigurationException {

        return (T) new ReflectiveAdapter<T>(object).fromConfigNode(configNode, new AnnotatedConfigurableProperty(object.getClass()), this);
    }

    /**
     * Will not work (throws an exception) if <b>object</b> has setters that use configurable properties!
     * @param object
     * @param <T>
     * @return
     */
    public <T> Map<String, Object> createConfigNodeFromInstance(T object) throws ConfigurationException {
        return (Map<String, Object>) lookupDefaultTypeAdapter(object.getClass()).toConfigNode(object, new AnnotatedConfigurableProperty(object.getClass()), this);
    }


    @SuppressWarnings("unchecked")
    public ConfigTypeAdapter lookupTypeAdapter(AnnotatedConfigurableProperty property) throws ConfigurationException {

        Class clazz = getRawClass(property);

        // first check for a custom adapter
        ConfigTypeAdapter typeAdapter = customConfigTypeAdapters.get(clazz);
        if (typeAdapter != null) return typeAdapter;

        // delegate to default otherwise
        return lookupDefaultTypeAdapter(clazz);
    }

    public static Class getRawClass(AnnotatedConfigurableProperty property) {
        Type type = property.getType();
        Class clazz;

        if (ParameterizedType.class.isAssignableFrom((Class<?>) type))
            clazz = (Class) ((ParameterizedType) type).getRawType();
        else {
            clazz = (Class) type;
        }
        return clazz;
    }

    @SuppressWarnings("unchecked")
    public ConfigTypeAdapter lookupDefaultTypeAdapter(Class clazz) throws ConfigurationException {

        ConfigTypeAdapter adapter = null;

        // if it is a config class, use reflective adapter
        if (clazz.getAnnotation(ConfigurableClass.class) != null)
            adapter = new ReflectiveAdapter();
        else if (clazz.isArray())
            adapter = new ArrayTypeAdapter();
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
