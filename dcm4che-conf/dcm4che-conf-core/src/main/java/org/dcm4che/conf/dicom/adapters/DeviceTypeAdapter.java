package org.dcm4che.conf.dicom.adapters;

import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.net.Device;

/**
 * Device by name
 */
public class DeviceTypeAdapter extends DefaultConfigTypeAdapters.CommonAbstractTypeAdapter<Device> {

    public DeviceTypeAdapter() {
        super("string");
        metadata.put("class","Device");
    }

    @Override
    public Device fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        try {
            return configNode == null ? null : vitalizer.getContext(DicomConfiguration.class).findDevice(configNode);
        } catch (NullPointerException e) {
            throw new ConfigurationException("Cannot dereference DICOM Device without DicomConfiguration context", e);
        }
    }

    @Override
    public String toConfigNode(Device object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationUnserializableException {
        return object.getDeviceName();
    }

}
