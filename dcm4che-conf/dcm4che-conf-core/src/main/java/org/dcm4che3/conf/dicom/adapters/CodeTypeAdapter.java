package org.dcm4che3.conf.dicom.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che3.data.Code;
import org.dcm4che3.data.Issuer;

public class CodeTypeAdapter extends DefaultConfigTypeAdapters.CommonAbstractTypeAdapter<Code> {

    public CodeTypeAdapter() {
        super("string");
        metadata.put("class","Code");
    }

    @Override
    public Code fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        return new Code(configNode);
    }

    @Override
    public String toConfigNode(Code object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationUnserializableException {
        return object.toString();
    }

}
