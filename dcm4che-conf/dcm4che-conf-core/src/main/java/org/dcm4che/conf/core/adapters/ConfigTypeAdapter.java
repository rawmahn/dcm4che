package org.dcm4che.conf.core.adapters;

import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;

import java.lang.reflect.Field;
import java.util.Map;

public interface ConfigTypeAdapter<T, ST> {


    /**
     * Converts serialized configuration representation to the type provided by this adaptor.
     * @param configNode
     * @param property the property which is going to be assigned the returned value (Can be null)
     * @param vitalizer
     * @return
     * @throws ConfigurationException
     */
    T fromConfigNode(ST configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException;

    /**
     * <p>Creates a serialized configuration representation for a provided object.
     * Throws ConfigurationUnserializableException when the object allows configuration
     * with setters in which case it is impossible to trace the parameters used in the setters back.</p>
     *
     * <p>This method should not be used generally, and the modifications to configuration should
     * be made through the Configuration access API that performs validation, defaults handling, etc. </p>
     * @param object
     * @param vitalizer
     * @return
     * @throws ConfigurationException
     */
    ST toConfigNode(T object, BeanVitalizer vitalizer) throws ConfigurationException;

    Map<String, Object> getMetadata(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException;
}
