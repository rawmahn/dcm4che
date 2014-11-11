package org.dcm4che3.conf.dicom.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che3.data.Issuer;
import org.dcm4che3.net.Device;

public class IssuerTypeAdapter extends DefaultConfigTypeAdapters.CommonAbstractTypeAdapter<Issuer> {

    public IssuerTypeAdapter() {
        super("string");
        metadata.put("class","Issuer");
    }

    @Override
    public Issuer fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        return configNode == null? null : new Issuer(configNode);
    }

    @Override
    public String toConfigNode(Issuer object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationUnserializableException {
        return object==null ? null : object.toString();
    }

}
