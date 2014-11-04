package org.dcm4che3.conf.core.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.data.Issuer;

import java.util.TimeZone;

public class TimeZoneTypeAdapter extends DefaultConfigTypeAdapters.CommonAbstractTypeAdapter<TimeZone> {

    public TimeZoneTypeAdapter() {
        super("string");
        metadata.put("class","TimeZone");
    }

    @Override
    public TimeZone fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        return (configNode == null ? null : TimeZone.getTimeZone(configNode));
    }

    @Override
    public String toConfigNode(TimeZone object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationUnserializableException {
        return (object == null ? null : object.getID());
    }

}
