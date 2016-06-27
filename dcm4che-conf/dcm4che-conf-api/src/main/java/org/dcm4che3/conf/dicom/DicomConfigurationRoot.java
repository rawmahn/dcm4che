package org.dcm4che3.conf.dicom;

import org.dcm4che3.conf.api.upgrade.ConfigurationMetadata;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.LDAP;
import org.dcm4che3.net.Device;

import java.util.Map;

@ConfigurableClass
public class DicomConfigurationRoot {

    @ConfigurableProperty
    DicomConfigurationNode dicomConfigurationRoot;

    public DicomConfigurationNode getDicomConfigurationRoot() {
        return dicomConfigurationRoot;
    }

    public void setDicomConfigurationRoot(DicomConfigurationNode dicomConfigurationRoot) {
        this.dicomConfigurationRoot = dicomConfigurationRoot;
    }

    @ConfigurableClass
    public static class DicomConfigurationNode {

        @ConfigurableProperty
        Map<String, Device> dicomDevicesRoot;

        @ConfigurableProperty
        MetadataRoot metadataRoot;

        public MetadataRoot getMetadataRoot() {
            return metadataRoot;
        }

        public void setMetadataRoot(MetadataRoot metadataRoot) {
            this.metadataRoot = metadataRoot;
        }

        public Map<String, Device> getDevices() {
            return dicomDevicesRoot;
        }

        public void setDevices(Map<String, Device> devices) {
            this.dicomDevicesRoot = devices;
        }


    }


    @ConfigurableClass
    public static class MetadataRoot {

        @ConfigurableProperty
        ConfigurationMetadata versioning;

        public ConfigurationMetadata getVersioning() {
            return versioning;
        }

        public void setVersioning(ConfigurationMetadata versioning) {
            this.versioning = versioning;
        }
    }
}
