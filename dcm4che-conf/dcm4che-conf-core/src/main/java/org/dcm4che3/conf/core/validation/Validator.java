package org.dcm4che3.conf.core.validation;

import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;

/**
 * Created by aprvf on 22/10/2014.
 */
public interface Validator<T> {

    public void validate(T configNode, AnnotatedConfigurableProperty property) throws ValidationException;
}
