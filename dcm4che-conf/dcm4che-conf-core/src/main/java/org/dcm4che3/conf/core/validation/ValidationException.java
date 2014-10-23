package org.dcm4che3.conf.core.validation;

import org.dcm4che3.conf.api.ConfigurationException;

/**
 * @author Roman K
 */
public class ValidationException extends ConfigurationException {


    public ValidationException(String s) {
        super(s);
    }
}
