package org.dcm4che3.conf.migration;


import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.core.DefaultBeanVitalizer;
import org.dcm4che3.conf.core.api.Configuration;
import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.conf.core.api.internal.BeanVitalizer;

import java.util.Map;
import java.util.Set;

/**
 * @author Roman K
 */
public class MigrationRunner {

    public static final String METADATA_ROOT_PATH = "/metadataRoot/versioning/current";

    Configuration configuration;
    Set<MigrationScript> migrationScripts;
    DicomConfiguration dicomConfiguration;

    public MigrationRunner(Configuration configuration, Set<MigrationScript> migrationScripts, DicomConfiguration dicomConfiguration) {
        this.configuration = configuration;
        this.migrationScripts = migrationScripts;
        this.dicomConfiguration = dicomConfiguration;
    }


    /**
     * Performs migration with all registered scripts according to fromVersion and toVersion annotations
     * @return The latest metadata of the configuration storage after the migration
     * @throws ConfigurationException
     */
    public ConfigurationMetadata performMigration() throws ConfigurationException {

        BeanVitalizer beanVitalizer = new DefaultBeanVitalizer();

        String metadataPath = METADATA_ROOT_PATH;
        Object metadataNode = configuration.getConfigurationNode(metadataPath, ConfigurationMetadata.class);
        ConfigurationMetadata configMetadata = null;
        if (metadataNode != null)
            configMetadata = beanVitalizer.newConfiguredInstance((Map<String, Object>) metadataNode, ConfigurationMetadata.class);
        else {
            configMetadata = new ConfigurationMetadata();
            configMetadata.setVersion("");
        }

        MigrationScript ms;





        return configMetadata;
    }


}

