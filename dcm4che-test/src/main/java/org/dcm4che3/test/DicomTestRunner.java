package org.dcm4che3.test;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Created by player on 27-Nov-14.
 */
public class DicomTestRunner extends BlockJUnit4ClassRunner {
    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws org.junit.runners.model.InitializationError if the test class is malformed.
     */
    public DicomTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }
}
