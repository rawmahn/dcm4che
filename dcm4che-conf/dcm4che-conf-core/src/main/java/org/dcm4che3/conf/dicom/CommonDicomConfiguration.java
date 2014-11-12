package org.dcm4che3.conf.dicom;

import org.dcm4che3.conf.api.ConfigurationAlreadyExistsException;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.hl7.HL7Configuration;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.LDAP;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.conf.dicom.adapters.*;
import org.dcm4che3.data.Code;
import org.dcm4che3.data.Issuer;
import org.dcm4che3.data.ValueSelector;
import org.dcm4che3.net.*;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4che3.util.AttributesFormat;
import org.dcm4che3.util.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author Roman K
 */
public class CommonDicomConfiguration implements DicomConfiguration {

    private static final Logger LOG =
            LoggerFactory.getLogger(CommonDicomConfiguration.class);

    Configuration config;
    BeanVitalizer vitalizer;
    private final Collection<Class<? extends DeviceExtension>> deviceExtensionClasses;
    private final Collection<Class<? extends AEExtension>> aeExtensionClasses;


    /**
     * Needed for avoiding infinite loops when dealing with extensions containing circular references
     * e.g., one device extension references another device which has an extension that references the former device.
     * Devices that have been created but not fully loaded are added to this threadlocal. See findDevice.
     */
    private ThreadLocal<Map<String, Device>> currentlyLoadedDevicesLocal = new ThreadLocal<Map<String, Device>>();

    public CommonDicomConfiguration(Configuration configurationStorage, BeanVitalizer vitalizer, Collection<Class<? extends DeviceExtension>> deviceExtensionClasses, Collection<Class<? extends AEExtension>> aeExtensionClasses) {
        this.config = configurationStorage;
        this.vitalizer = vitalizer;
        this.deviceExtensionClasses = deviceExtensionClasses;
        this.aeExtensionClasses = aeExtensionClasses;

        // register reference handler
        vitalizer.setReferenceTypeAdapter(new DicomReferenceHandlerAdapter(vitalizer,configurationStorage));

        // register type adapters and the DicomConfiguration context
        this.vitalizer.registerCustomConfigTypeAdapter(AttributesFormat.class, new AttributeFormatTypeAdapter());
        this.vitalizer.registerCustomConfigTypeAdapter(Code.class, new CodeTypeAdapter());
        this.vitalizer.registerCustomConfigTypeAdapter(Device.class, new DeviceReferenceByNameTypeAdapter());
        this.vitalizer.registerCustomConfigTypeAdapter(Issuer.class, new IssuerTypeAdapter());
        this.vitalizer.registerCustomConfigTypeAdapter(ValueSelector.class, new ValueSelectorTypeAdapter());
        this.vitalizer.registerCustomConfigTypeAdapter(Property.class, new PropertyTypeAdapter());

        this.vitalizer.registerContext(DicomConfiguration.class, this);


        // quick init
        try {
            if (!configurationExists()) {
                Map<String, Object> rootNode = new HashMap<String, Object>();
                HashMap<String, Map<String, Object>> subroots = new HashMap<String, Map<String, Object>>();
                subroots.put("dicomDevicesRoot", new HashMap<String, Object>());
                subroots.put("dicomUniqueAETitlesRegistryRoot", new HashMap<String, Object>());
                rootNode.put("dicomConfigurationRoot", subroots);
                config.persistNode("/", rootNode, null);

            }
        } catch (ConfigurationException e) {
            throw new RuntimeException("Dicom configuration cannot be initialized",e);
        }
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

    @LDAP(objectClasses = "dicomUniqueAETitle", distinguishingField = "dicomAETitle")
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

        final HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("dicomAETitle", aet);

        config.persistNode(path, map, AETitleItem.class);
        return true;

    }

    private String getAETPath(String aet) {
        return "dicomConfigurationRoot/dicomUniqueAETitlesRegistryRoot[@name='" + ConfigNodeUtil.escapeApos(aet)+"']";
    }

    @Override
    public void unregisterAETitle(String aet) throws ConfigurationException {
        config.removeNode(getAETPath(aet));
    }

    @Override
    public ApplicationEntity findApplicationEntity(String aet) throws ConfigurationException {

        Iterator search = config.search("dicomConfigurationRoot/dicomDevicesRoot/*[dicomNetworkAE[@name='"+ aet + "']]/dicomDeviceName");

        try {
            String deviceNameNode = (String) search.next();
            if (search.hasNext())
                LOG.warn("Application entity title '{}' is not unique. Check the configuration!", aet);
            Device device = findDevice(deviceNameNode);

            ApplicationEntity ae = device.getApplicationEntitiesMap().get(aet);
            if (ae == null) throw new NoSuchElementException("Unexpected error");
            return ae;

        } catch (NoSuchElementException e) {
            throw new ConfigurationNotFoundException("AE '" + aet + "' not found",e);
        }
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
            Object configurationNode = config.getConfigurationNode(deviceRef(name));

            if (configurationNode == null) return null;

            Device device = new Device();
            deviceCache.put(name, device);

            vitalizer.configureInstance(device, Device.class, (Map < String, Object >)configurationNode);

            // add device extensions
            for (Class<? extends DeviceExtension> deviceExtensionClass : deviceExtensionClasses) {
                Map<String, Object> deviceExtensionNode = (Map<String, Object>) config.getConfigurationNode(deviceRef(name) + "/deviceExtensions/"+deviceExtensionClass.getSimpleName());
                if (deviceExtensionNode!= null)
                    device.addDeviceExtension(vitalizer.newConfiguredInstance(deviceExtensionClass, deviceExtensionNode));
            }

            // add ae extensions
            for (Map.Entry<String, ApplicationEntity> entry : device.getApplicationEntitiesMap().entrySet()) {
                String aeTitle = entry.getKey();
                ApplicationEntity ae = entry.getValue();
                for (Class<? extends AEExtension> aeExtensionClass : aeExtensionClasses) {
                    Object aeExtNode = config.getConfigurationNode(deviceRef(name) + "/dicomNetworkAE[@name='" + ConfigNodeUtil.escapeApos(aeTitle) + "']/aeExtensions/" + aeExtensionClass.getSimpleName());
                    if (aeExtNode != null) {
                        ae.addAEExtension(vitalizer.newConfiguredInstance(aeExtensionClass, (Map<String, Object>) aeExtNode));
                    }
                }
            }

            return device;
            } catch (ConfigurationException e) {
            throw new ConfigurationException("Configuration for device " + name + " cannot be loaded", e);
        } finally {
            // if this loadDevice call initialized the cache, then clean it up
            if (doCleanUpCache) currentlyLoadedDevicesLocal.remove();
        }
    }

    @Override
    public DeviceInfo[] listDeviceInfos(DeviceInfo keys) throws ConfigurationException {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public String[] listDeviceNames() throws ConfigurationException {
        Iterator search = config.search("dicomConfigurationRoot/dicomDeviceRoot/*/dicomDeviceName");
        List<String> deviceNames = null;
        try {
            deviceNames = new ArrayList<String>();
            while (search.hasNext())
                deviceNames.add((String) search.next());
        } catch (Exception e) {
            throw new ConfigurationException("Error while getting list of device names", e);
        }
        return deviceNames.toArray(new String[deviceNames.size()]);
    }

    @Override
    public String[] listRegisteredAETitles() throws ConfigurationException {
        List<String> aeNames = new ArrayList<String>();
        try {
            Iterator search = config.search("dicomConfigurationRoot/dicomDeviceRoot/*/dicomNetworkAE/dicomAETitle");
            while (search.hasNext())
                aeNames.add((String) search.next());
        } catch (Exception e) {
            throw new ConfigurationException("Error while getting the list of registered AE titles", e);
        }
        return aeNames.toArray(new String[aeNames.size()]);
    }

    @Override
    public void persist(Device device) throws ConfigurationException {
        if (config.nodeExists(deviceRef(device.getDeviceName())))
            throw new ConfigurationAlreadyExistsException("Device " + device.getDeviceName() + " already exists");
        // otherwise it is the same as merge
        merge(device);
    }

    @Override
    public void merge(Device device) throws ConfigurationException {
        String devicePath = deviceRef(device.getDeviceName());

        // persist device
        final Map<String, Object> deviceConfigNode = vitalizer.createConfigNodeFromInstance(device, Device.class);

        config.persistNode(devicePath, deviceConfigNode, Device.class);

        // persist AEExtensions
        for (Map.Entry<String, ApplicationEntity> entry : device.getApplicationEntitiesMap().entrySet()) {

            ApplicationEntity ae = entry.getValue();

            for (Class<? extends AEExtension> aeExtensionClass : aeExtensionClasses) {
                AEExtension aeExtension = ae.getAEExtension(aeExtensionClass);
                if (aeExtension == null) continue;
                Map<String, Object> aeExtNode = vitalizer.createConfigNodeFromInstance(aeExtension, aeExtensionClass);
                config.persistNode(devicePath+"/dicomNetworkAE[@name='"+ae.getAETitle()+"']/aeExtensions/"+aeExtensionClass.getSimpleName(),aeExtNode, aeExtensionClass);
            }
        }

        // persist DeviceExtensions
        for (Class<? extends DeviceExtension> deviceExtensionClass : deviceExtensionClasses) {
            final DeviceExtension deviceExtension = device.getDeviceExtension(deviceExtensionClass);
            final String extensionPath = devicePath + "/deviceExtensions/" + deviceExtensionClass.getSimpleName();

            if (deviceExtension == null)
                config.removeNode(extensionPath);
            else
                config.persistNode(extensionPath, vitalizer.createConfigNodeFromInstance(deviceExtension, deviceExtensionClass), deviceExtensionClass);
        }
    }



    @Override
    public void removeDevice(String name) throws ConfigurationException {
        config.removeNode(deviceRef(name));
    }

    @Override
    public String deviceRef(String name) {
        return "dicomConfigurationRoot/dicomDevicesRoot[@name='" + ConfigNodeUtil.escapeApos(name) + "']";
    }

    @Override
    public void persistCertificates(String ref, X509Certificate... certs) throws ConfigurationException {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void removeCertificates(String ref) throws ConfigurationException {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public X509Certificate[] findCertificates(String dn) throws ConfigurationException {
        throw new RuntimeException("Not implemented yet");
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
        throw new RuntimeException("Not implemented yet");
    }


}

