package org.dcm4che3.conf.core.migration;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.Migration;
import org.dcm4che3.conf.core.MigrationScript;
import org.dcm4che3.conf.dicom.DicomPath;

/**
 * @author Roman K
 */
@Migration(toVersion = "C33")
public class MigrationSample implements MigrationScript{

    @Override
    public void migrate(Configuration config) throws ConfigurationException {

        //config.getConfigurationNode( null)

    }
}
