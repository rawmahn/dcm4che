package org.dcm4che3.conf.core.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.Configuration;

import java.util.*;

/**
 * Default dereferencer.
 * @param <T>
 */
public class DefaultReferenceAdapter<T> implements ConfigTypeAdapter<T,String> {

    private BeanVitalizer vitalizer;
    private Configuration config;


    public DefaultReferenceAdapter(BeanVitalizer vitalizer, Configuration config) {
        this.vitalizer = vitalizer;
        this.config = config;
    }

    @Override
    public T fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        // treat configNode as path, load the node at that path, create an instance from it
        return (T) vitalizer.newConfiguredInstance((Class) property.getType(), (Map<String, Object>) config.getConfigurationNode(configNode));
    }

    @Override
    public String toConfigNode(T object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        throw new ConfigurationUnserializableException("No information about where to look for the reference");
    }

    @Override
    public Map<String, Object> getSchema(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("type", "string");
        metadata.put("class", "Reference");
        //TODO: add regex..
        return metadata;
    }

    @Override
    public String normalize(Object configNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
        return (String) configNode;
    }
}
