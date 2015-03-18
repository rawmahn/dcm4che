package org.dcm4che3.conf.core;

import org.dcm4che3.conf.api.ConfigurationException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Roman K
 */
public class MigrationRunner {

    Configuration configuration;
    Set<MigrationScript> migrationScripts;

    public MigrationRunner(Configuration configuration, Set<MigrationScript> migrationScripts) {
        this.configuration = configuration;
        this.migrationScripts = migrationScripts;
    }

    /**
     * Performs migration with all registered scripts according to fromVersion and toVersion annotations
     * @return The latest metadata of the configuration storage after the migration
     * @throws ConfigurationException
     */
    public ConfigurationMetadata performMigration() throws ConfigurationException {

        BeanVitalizer beanVitalizer = new BeanVitalizer();

        String metadataPath = "/metadataRoot/versioning/current";
        Object metadataNode = configuration.getConfigurationNode(metadataPath, ConfigurationMetadata.class);
        ConfigurationMetadata configMetadata = null;
        if (metadataNode != null)
            configMetadata = beanVitalizer.newConfiguredInstance((Map<String, Object>) metadataNode, ConfigurationMetadata.class);
        else {
            configMetadata = new ConfigurationMetadata();
            configMetadata.setVersion(Migration.NOVERSION);
        }


        Set<MigrationScript> migrateInThisIteration;
        do {

            migrateInThisIteration = new HashSet<MigrationScript>();
            String toVersion = null;

            // filter out the scripts that should be run for current version
            for (MigrationScript script : migrationScripts) {
                Migration migrationAnno = script.getClass().getAnnotation(Migration.class);

                if (migrationAnno == null)
                    throw new RuntimeException("'Migration' annotation on migration script " + script.getClass() + " is missing");

                if (migrationAnno.fromVersion().equals(configMetadata.getVersion())) {
                    migrateInThisIteration.add(script);
                    if (toVersion == null) toVersion = migrationAnno.toVersion();
                    if (!toVersion.equals(migrationAnno.toVersion()))
                        throw new IllegalArgumentException("All migration scripts from version '" + configMetadata.getVersion() + "' must have the same toVersion." +
                                " Different toVersions found:" + toVersion + " and " + migrationAnno.toVersion());
                }
            }

            // migrate from current version
            for (MigrationScript script : migrateInThisIteration) {
                System.out.println("Running migration script " + script.getClass());
                script.migrate(configuration);
            }

            // bump version
            if (toVersion != null) {
                configMetadata.setVersion(toVersion);
                configuration.persistNode(metadataPath, beanVitalizer.createConfigNodeFromInstance(configMetadata), ConfigurationMetadata.class);
            }

        } while (migrateInThisIteration.size() > 0);

        return configMetadata;
    }


}

