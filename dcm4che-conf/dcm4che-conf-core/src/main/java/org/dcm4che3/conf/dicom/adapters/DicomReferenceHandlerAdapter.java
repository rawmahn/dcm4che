package org.dcm4che3.conf.dicom.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.adapters.DefaultConfigTypeAdapters;
import org.dcm4che3.conf.core.adapters.ReferenceHandlerAdapter;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.net.Connection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DicomReferenceHandlerAdapter<T> extends ReferenceHandlerAdapter<T> {
    public DicomReferenceHandlerAdapter(BeanVitalizer vitalizer, Configuration config) {
        super(vitalizer, config);
    }

    @Override
    public T fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        // Connection of a device. Get the device - it will grab the current one from threadLocal
        if (Connection.class.isAssignableFrom(BeanVitalizer.getRawClass(property))) {

            ConfigNodeUtil.parseReference("dicomConfigurationRoot/dicomDevicesRoot/Impax/dicomConnection/*[dicomHostname='theHost']");

            String mydata = "some string with 'the data i want' inside";
            Pattern pattern = Pattern.compile("'(.*?)'");
            Matcher matcher = pattern.matcher(mydata);
            if (matcher.find()) {
                System.out.println(matcher.group(1));
            }


        }
        property.getType();

        return null;
    }

}
