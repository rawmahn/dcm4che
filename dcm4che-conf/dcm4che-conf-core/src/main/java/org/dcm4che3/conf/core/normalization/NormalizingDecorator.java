package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.impl.DelegatingConfiguration;
import org.dcm4che3.conf.api.ConfigurationException;

import java.util.Iterator;
import java.util.Map;

/**
 * All properties' values are validated against the <code>javax.validation</code> annotations.
 * If there is no value for property, and no default is specified, validation exception is raised.
 * @author Roman K
 */
public class NormalizingDecorator extends DelegatingConfiguration{


    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {
        // TODO: normalize
        super.persistNode(path, configNode, configurableClass);
    }


    @Override
    public Object getConfigurationNode(String path) throws ConfigurationException {
        // TODO: normalize
        return super.getConfigurationNode(path);
    }

    @Override
    public Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException {

        final Iterator found = super.search(liteXPathExpression);
        return new Iterator() {

            @Override
            public boolean hasNext() {
                return found.hasNext();
            }

            @Override
            public Object next() {
                // TODO: normalize
                return found.next();
            }

            @Override
            public void remove() {
                found.remove();
            }
        };

    }

    public NormalizingDecorator(Configuration delegate) {
        super(delegate);
    }

}
