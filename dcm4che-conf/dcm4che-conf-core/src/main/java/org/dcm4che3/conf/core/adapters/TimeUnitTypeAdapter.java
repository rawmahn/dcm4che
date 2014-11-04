package org.dcm4che3.conf.core.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TimeUnitTypeAdapter extends DefaultConfigTypeAdapters.CommonAbstractTypeAdapter<TimeUnit> {

    public TimeUnitTypeAdapter() {
        super("string");
        metadata.put("class","TimeUnit");
    }

    @Override
    public TimeUnit fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        return (configNode == null ? null : TimeUnit.valueOf(configNode));
    }

    @Override
    public String toConfigNode(TimeUnit object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationUnserializableException {
        return (object == null ? null : object.toString());
    }

}
