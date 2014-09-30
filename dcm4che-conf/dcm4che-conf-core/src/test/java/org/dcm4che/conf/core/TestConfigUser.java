package org.dcm4che.conf.core;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by aprvf on 29/09/2014.
 */
public class TestConfigUser {

    @Inject
    TestConfigurableObject configuredObject;





    @Produces
    TestConfigurableObject getAnObject() {

        Configuration conf;

        BeanVitalizer.newConfiguredInstance(TestConfigurableObject.class, conf.getRoot());

        return null;
    }
}
