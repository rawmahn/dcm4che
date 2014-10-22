package org.dcm4che.conf.core.adapters;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che.conf.core.util.ConfigIterators;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.api.generic.ConfigurableProperty;

/**
 * Reflective adapter that handles classes with ConfigurableClass annotations.<br/>
 * <br/>
 *
 * User has to use the special constructor and initialize providedConfObj when the
 * already created conf object should be used instead of instantiating one
 */
@SuppressWarnings("unchecked")
public class ReflectiveAdapter<T> implements ConfigTypeAdapter<T, Map<String,Object>> {

    private T providedConfObj;

    public ReflectiveAdapter() {
    }

    public ReflectiveAdapter(T providedConfigurationObjectInstance) {
        this.providedConfObj = providedConfigurationObjectInstance;
    }


    @Override
    public T fromConfigNode(Map<String, Object> configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        if (configNode == null) return null;
        Class<T> clazz = (Class<T>) property.getType();

        if (!Map.class.isAssignableFrom(configNode.getClass())) throw new ConfigurationException("Provided configuration node is not a map (type "+clazz.getName()+")");


        T confObj;

        // create instance or use provided when it was created for us
        if (providedConfObj == null) {
            try {
                confObj = (T) clazz.newInstance();
            } catch (Exception e) {
                throw new ConfigurationException("Error while instantiating config class " + clazz.getSimpleName()
                        + ". Check whether null-arg constructor exists.", e);
            }
        } else
            confObj = providedConfObj;

        // iterate and populate annotated fields
        for (AnnotatedConfigurableProperty fieldProperty : ConfigIterators.getAllConfigurableFields(clazz))
            try {
                Object fieldValue = DefaultConfigTypeAdapters.delegateGetChildFromConfigNode(configNode, fieldProperty, vitalizer);
                PropertyUtils.setSimpleProperty(confObj, fieldProperty.getName(), fieldValue);
            } catch (Exception e) {
                throw new ConfigurationException("Error while reading configuration field " + fieldProperty.getName(), e);
            }

        // iterate over setters
        for (ConfigIterators.AnnotatedSetter setter : ConfigIterators.getAllConfigurableSetters(clazz)) {
            try {
                // populate parameters for the setter
                Object[] args = new Object[setter.getParameters().size()];
                int i = 0;
                for (AnnotatedConfigurableProperty paramProperty : setter.getParameters())
                    args[i++] = DefaultConfigTypeAdapters.delegateGetChildFromConfigNode(configNode, paramProperty, vitalizer);

                // invoke setter
                setter.getMethod().invoke(confObj, args);
            } catch (Exception e) {
                throw new ConfigurationException("Error while trying to initialize the object with method "+ setter.getMethod().getName(), e);
            }
        }

        return confObj;
    }


    @Override
    public Map<String, Object> toConfigNode(T object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationUnserializableException {

        if (object == null) return null;

        Class<T> clazz = (Class<T>) object.getClass();

        Map<String,Object> configNode = new HashMap<String,Object>();

        // get data from all the configurable fields
        for (AnnotatedConfigurableProperty fieldProperty : ConfigIterators.getAllConfigurableFields(clazz)) {
            try {
                Object value = PropertyUtils.getSimpleProperty(object, fieldProperty.getName());
                DefaultConfigTypeAdapters.delegateChildToConfigNode(value,configNode,fieldProperty, vitalizer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // there must be no setters
        for (ConfigIterators.AnnotatedSetter setter : ConfigIterators.getAllConfigurableSetters(clazz))
            throw new ConfigurationUnserializableException("Cannot infer properties which are setter parameters. This object has a setter (" + setter.getMethod().getName() + ")");

        return configNode;
    }


    @Override
    public Map<String, Object> getSchema(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Class<T> clazz = (Class<T>) property.getType();

        Map<String,Object> classMetaDataWrapper = new HashMap<String,Object>();
        Map<String,Object> classMetaData = new HashMap<String,Object>();
        classMetaDataWrapper.put("properties", classMetaData);
        classMetaDataWrapper.put("type", "object");
        classMetaDataWrapper.put("class", clazz.getSimpleName());

        for (AnnotatedConfigurableProperty configurableChildProperty : ConfigIterators.getAllConfigurableFieldsAndSetterParameters(clazz)) {

            ConfigurableProperty propertyAnnotation = configurableChildProperty.getAnnotation(ConfigurableProperty.class);

            Map<String, Object> childPropertyMetadata = new HashMap<String, Object>();
            classMetaData.put(configurableChildProperty.getAnnotatedName(), childPropertyMetadata);
            childPropertyMetadata.put("title", propertyAnnotation.label());
            childPropertyMetadata.put("description", propertyAnnotation.description());
            // TODO: default value should be converted to proper type
            childPropertyMetadata.put("default", propertyAnnotation.defaultValue());

            // also merge in the metadata from this child itself
            ConfigTypeAdapter adapter = vitalizer.lookupTypeAdapter(configurableChildProperty.getType());
            Map<String, Object> childMetaData = adapter.getSchema(configurableChildProperty, vitalizer);
            if (childMetaData != null) childPropertyMetadata.putAll(childMetaData);
        }

        return classMetaDataWrapper;
    }

    @Override
    public Map<String, Object> normalize(Object configNode) throws ConfigurationException {
        return (Map<String, Object>) configNode;
    }
}