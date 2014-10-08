package org.dcm4che3.conf.api.generic;

import org.dcm4che3.conf.api.ConfigurationException;

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
     * Optional, may just throw exception. Kept for backward compatibility.
     * @param object
     * @return
     */
    ST toConfigNode(T object);

    Map<String, Object> getMetadata(ReflectiveConfig config, Field field) throws ConfigurationException;

}
