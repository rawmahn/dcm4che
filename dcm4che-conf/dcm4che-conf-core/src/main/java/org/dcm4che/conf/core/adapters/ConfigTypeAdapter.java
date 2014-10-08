package org.dcm4che.conf.core.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;

import java.lang.reflect.Field;
import java.util.Map;

public interface ConfigTypeAdapter<T, ST> {


    /**
     * should include validation
     * @param configNode
     * @param property
     * @return
     * @throws ConfigurationException
     */
    T fromConfigNode(ST configNode, AnnotatedProperty property, BeanVitalizer vitalizer) throws ConfigurationException;

    /**
     * Might
     * @param object
     * @return
     */
    ST toConfigNode(T object) throws ConfigurationException;

    Map<String, Object> getMetadata(ReflectiveConfig config, Field field) throws ConfigurationException;

}
