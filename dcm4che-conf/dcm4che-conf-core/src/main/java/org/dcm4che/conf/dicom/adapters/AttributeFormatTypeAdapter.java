package org.dcm4che.conf.dicom.adapters;

import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.util.AttributesFormat;

/**
 * AttributesFormat
 */
public class AttributeFormatTypeAdapter extends DefaultConfigTypeAdapters.CommonAbstractTypeAdapter<AttributesFormat> {

    public AttributeFormatTypeAdapter() {
        super("AttributesFormat");
    }

    @Override
    public AttributesFormat fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        return AttributesFormat.valueOf(configNode);
    }

    @Override
    public String toConfigNode(AttributesFormat object, BeanVitalizer vitalizer) throws ConfigurationUnserializableException {
        return (object == null ? null : object.toString());
    }
}
