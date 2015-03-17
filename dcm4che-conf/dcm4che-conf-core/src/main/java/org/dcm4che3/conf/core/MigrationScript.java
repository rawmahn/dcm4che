package org.dcm4che3.conf.core;

import org.dcm4che3.conf.api.ConfigurationException;

/**
 * @author Roman K
 */
public interface MigrationScript {

    public void migrate(Configuration config) throws ConfigurationException;
}
