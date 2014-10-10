package org.dcm4che.conf.dicom;

import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che.conf.core.ConfigurationStorage;
import org.dcm4che.conf.dicom.adapters.AttributeFormatTypeAdapter;
import org.dcm4che.conf.dicom.adapters.DeviceTypeAdapter;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceInfo;
import org.dcm4che3.util.AttributesFormat;

import java.security.cert.X509Certificate;

/**
 * @author Roman K
 */
public class CommonDicomConfiguration implements DicomConfiguration{


    ConfigurationStorage configurationStorage;
    BeanVitalizer vitalizer;

    public CommonDicomConfiguration(ConfigurationStorage configurationStorage, BeanVitalizer vitalizer) {
        this.configurationStorage = configurationStorage;
        this.vitalizer = vitalizer;

        // register type adapters and the DicomConfiguration context
        this.vitalizer.registerCustomConfigTypeAdapter(AttributesFormat.class, new AttributeFormatTypeAdapter());
        this.vitalizer.registerCustomConfigTypeAdapter(Device.class, new DeviceTypeAdapter());
        this.vitalizer.registerContext(DicomConfiguration.class, this);
    }

    @Override
    public boolean configurationExists() throws ConfigurationException {
        return false;
    }

    @Override
    public boolean purgeConfiguration() throws ConfigurationException {
        return false;
    }

    @Override
    public boolean registerAETitle(String aet) throws ConfigurationException {
        return false;
    }

    @Override
    public void unregisterAETitle(String aet) throws ConfigurationException {

    }

    @Override
    public ApplicationEntity findApplicationEntity(String aet) throws ConfigurationException {
        return null;
    }

    @Override
    public Device findDevice(String name) throws ConfigurationException {
        return null;
    }

    @Override
    public DeviceInfo[] listDeviceInfos(DeviceInfo keys) throws ConfigurationException {
        return new DeviceInfo[0];
    }

    @Override
    public String[] listDeviceNames() throws ConfigurationException {
        return new String[0];
    }

    @Override
    public String[] listRegisteredAETitles() throws ConfigurationException {
        return new String[0];
    }

    @Override
    public void persist(Device device) throws ConfigurationException {

    }

    @Override
    public void merge(Device device) throws ConfigurationException {

    }

    @Override
    public void removeDevice(String name) throws ConfigurationException {

    }

    @Override
    public String deviceRef(String name) {
        return null;
    }

    @Override
    public void persistCertificates(String ref, X509Certificate... certs) throws ConfigurationException {

    }

    @Override
    public void removeCertificates(String ref) throws ConfigurationException {

    }

    @Override
    public X509Certificate[] findCertificates(String dn) throws ConfigurationException {
        return new X509Certificate[0];
    }

    @Override
    public void close() {

    }

    @Override
    public void sync() throws ConfigurationException {

    }

    @Override
    public <T> T getDicomConfigurationExtension(Class<T> clazz) {
        return null;
    }
}
