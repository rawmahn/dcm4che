package org.dcm4che3.conf.migration;

import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.core.api.Configuration;
import org.dcm4che3.conf.core.api.ConfigurationException;

/**
 *
 * @param <V> type of version. Must be convertable to String
 */
public interface MigrationScript<V> {
    boolean doRunThisScript(ConfigurationMetadata metadataBeforeMigration, V migrationTargetVersion);
    void migrate(Configuration config, DicomConfiguration dicomConfiguration) throws ConfigurationException;
}
