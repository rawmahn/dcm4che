package org.dcm4che.conf.dicom;

import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che.conf.core.Configuration;
import org.dcm4che.conf.core.util.ConfigNodeUtil;
import org.dcm4che.conf.dicom.adapters.AttributeFormatTypeAdapter;
import org.dcm4che.conf.dicom.adapters.DeviceTypeAdapter;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.generic.ConfigurableProperty;
import org.dcm4che3.conf.ldap.generic.LDAP;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceInfo;
import org.dcm4che3.util.AttributesFormat;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Roman K
 */
public class CommonDicomConfiguration implements DicomConfiguration{


    Configuration config;
    BeanVitalizer vitalizer;


    /**
     * Needed for avoiding infinite loops when dealing with extensions containing circular references
     * e.g., one device extension references another device which has an extension that references the former device.
     * Devices that have been created but not fully loaded are added to this threadlocal. See loadDevice.
     */
    private ThreadLocal<Map<String,Device>> currentlyLoadedDevicesLocal = new ThreadLocal<Map<String,Device>>();

    public CommonDicomConfiguration(Configuration configurationStorage, BeanVitalizer vitalizer) {
        this.config = configurationStorage;
        this.vitalizer = vitalizer;

        // register type adapters and the DicomConfiguration context
        this.vitalizer.registerCustomConfigTypeAdapter(AttributesFormat.class, new AttributeFormatTypeAdapter());
        this.vitalizer.registerCustomConfigTypeAdapter(Device.class, new DeviceTypeAdapter());
        this.vitalizer.registerContext(DicomConfiguration.class, this);
    }

    @Override
    public boolean configurationExists() throws ConfigurationException {
        return config.nodeExists("dicomConfigurationRoot");
    }

    @Override
    public boolean purgeConfiguration() throws ConfigurationException {
        if (!configurationExists()) return false;
        config.persistNode("dicomConfigurationRoot", new HashMap<String, Object>(), null);
        return true;
    }

    @LDAP(objectClass = "dicomUniqueAETitle", distinguishingField = "dicomAETitle")
    public static class AETitleItem {

        public AETitleItem(String aeTitle) {
            this.aeTitle = aeTitle;
        }

        @ConfigurableProperty(name = "dicomAETitle")
        String aeTitle;


        public String getAeTitle() {
            return aeTitle;
        }

        public void setAeTitle(String aeTitle) {
            this.aeTitle = aeTitle;
        }
    }

    @Override
    public boolean registerAETitle(String aet) throws ConfigurationException {

        final String path = getAETPath(aet);
        if (config.nodeExists(path)) return false;

        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("dicomAETitle", aet);

        config.persistNode(path, map, AETitleItem.class);
        return true;

    }

    private String getAETPath(String aet) {
        return "dicomConfigurationRoot/dicomUniqueAETitlesRegistryRoot/" + ConfigNodeUtil.escape(aet);
    }

    @Override
    public void unregisterAETitle(String aet) throws ConfigurationException {
        config.removeNode(getAETPath(aet));
    }

    @Override
    public ApplicationEntity findApplicationEntity(String aet) throws ConfigurationException {
        //vitalizer.newConfiguredInstance(ApplicationEntity.class, config.getConfigurationNode(""))
        return null;
    }

    @Override
    public Device findDevice(String name) throws ConfigurationException {

        // get the device cache for this loading phase
        Map<String, Device> deviceCache = currentlyLoadedDevicesLocal.get();

        // if there is none, create one for the current thread and remember that it should be cleaned up when the device is loaded
        boolean doCleanUpCache = false;
        if (deviceCache == null) {
            doCleanUpCache = true;
            deviceCache = new HashMap<String, Device>();
            currentlyLoadedDevicesLocal.set(deviceCache);
        }

        // if a requested device is already being (was) loaded, do not load it again, just return existing Device object
        if (deviceCache.containsKey(name))
            return deviceCache.get(name);

        try {
            Object configurationNode = config.getConfigurationNode("dicomConfigurationRoot/Devices/" + ConfigNodeUtil.escape(name));
            return vitalizer.newConfiguredInstance(Device.class, (Map<String, Object>) configurationNode);
        } catch (ConfigurationException e) {
            throw new ConfigurationException("Configuration for device "+name+" cannot be loaded");
        } finally {
            // if this loadDevice call initialized the cache, then clean it up
            if (doCleanUpCache) currentlyLoadedDevicesLocal.remove();
        }
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
        config.refreshNode("dicomConfigurationRoot");
    }

    @Override
    public <T> T getDicomConfigurationExtension(Class<T> clazz) {
        return null;
    }
}
