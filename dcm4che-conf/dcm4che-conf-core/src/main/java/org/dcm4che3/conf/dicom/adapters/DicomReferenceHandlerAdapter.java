package org.dcm4che3.conf.dicom.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.adapters.DefaultReferenceAdapter;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DicomReferenceHandlerAdapter<T> extends DefaultReferenceAdapter<T> {
    public DicomReferenceHandlerAdapter(BeanVitalizer vitalizer, Configuration config) {
        super(vitalizer, config);
    }

    @Override
    public T fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        // Connection of a device. Get the device (it will grab the current one from threadLocal), and get the connection from there
        if (Connection.class.isAssignableFrom(BeanVitalizer.getRawClass(property))) {

            List<Map<String, Object>> props = ConfigNodeUtil.parseReference(configNode);
            //"dicomConfigurationRoot/dicomDevicesRoot/Impax/dicomConnection/*[dicomHostname='theHost']");

            try {

                String deviceName = (String) props.get(2).get("dicomDeviceName");
                if (deviceName == null) deviceName = (String) props.get(2).get("$name");


                boolean valid = props.get(0).get("$name").equals("dicomConfigurationRoot") &&
                        props.get(1).get("$name").equals("dicomDevicesRoot") &&
                        deviceName != null &&
                        props.get(3).get("$name").equals("dicomConnection");

                if (!valid) throw new RuntimeException("Path is invalid");

                Device device = vitalizer.getContext(DicomConfiguration.class).findDevice(deviceName);

                //device.getCo

            } catch (Exception e) {
                throw new ConfigurationException("Cannot find referenced connection ("+configNode+")",e);
            }

        }
        property.getType();

        return null;
    }

}
