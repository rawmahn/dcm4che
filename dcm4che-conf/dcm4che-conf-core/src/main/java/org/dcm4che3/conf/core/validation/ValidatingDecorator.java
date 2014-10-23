package org.dcm4che3.conf.core.validation;

import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.impl.DelegatingConfiguration;
import org.dcm4che3.conf.api.ConfigurationException;

import java.util.Map;
/**
 * All properties' values are validated against the <code>javax.validation</code> annotations.
 * If there is no value for property, and no default is specified, validation exception is raised.
 * @author Roman K
 */
public class ValidatingDecorator extends DelegatingConfiguration{

    public ValidatingDecorator(Configuration delegate) {
        super(delegate);
    }

    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {
        // TODO: normalize
        // TODO: perform validation on configNode
        super.persistNode(path, configNode, configurableClass);
    }




}
