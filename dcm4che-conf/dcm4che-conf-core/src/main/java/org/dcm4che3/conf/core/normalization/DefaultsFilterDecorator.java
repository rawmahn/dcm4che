package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.impl.DelegatingConfiguration;
import org.dcm4che3.conf.api.ConfigurationException;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by aprvf on 22/10/2014.
 */

/**
 * 
 * $$default as a reserved value that represents that the value from default item is used
 *
 * Switchable - for JSON and for POJOS - first will leave $$defaults untouched, second will .. but LDAP...??
 */
public class DefaultsFilterDecorator extends DelegatingConfiguration {
    public DefaultsFilterDecorator(Configuration delegate) {
        super(delegate);
    }

    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {
        // TODO: filter out properties that equals to defaults
        super.persistNode(path, configNode, configurableClass);
    }


    @Override
    public Object getConfigurationNode(String path) throws ConfigurationException {
        // TODO: fill in default values for properties that are null and have defaults
        return super.getConfigurationNode(path);
    }

    @Override
    public Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException {
        return super.search(liteXPathExpression);
    }
}
