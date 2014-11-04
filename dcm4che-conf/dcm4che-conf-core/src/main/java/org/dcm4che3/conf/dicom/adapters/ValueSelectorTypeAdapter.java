package org.dcm4che3.conf.dicom.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che3.data.Issuer;
import org.dcm4che3.data.ValueSelector;

public class ValueSelectorTypeAdapter extends DefaultConfigTypeAdapters.CommonAbstractTypeAdapter<ValueSelector> {

    public ValueSelectorTypeAdapter() {
        super("string");
        metadata.put("class","ValueSelector");
    }

    @Override
    public ValueSelector fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        return configNode == null ? null : ValueSelector.valueOf(configNode);
    }

    @Override
    public String toConfigNode(ValueSelector object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        return object == null ? null : object.toString();
    }

}
