package org.dcm4che.conf.core;

import org.dcm4che3.conf.api.ConfigurationException;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by aprvf on 29/09/2014.
 */
public class TestConfigUser {

    @Inject
    TestConfigurableObject configuredObject;





    @Produces
    TestConfigurableObject getAnObject() throws ConfigurationException {

        BasicConfigurationStorage conf = new BasicConfigurationStorage();

        BeanVitalizer.newConfiguredInstance(TestConfigurableObject.class, conf.getConfigurationRoot());

        conf.search("124");

        return null;
    }
}
