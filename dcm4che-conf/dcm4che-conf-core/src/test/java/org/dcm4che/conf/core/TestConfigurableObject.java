package org.dcm4che.conf.core;

import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;

/**
 * Created by aprvf on 29/09/2014.
 */
@ConfigClass
public class TestConfigurableObject {

    @ConfigField(name="myProp1")
    String myProp1;


}
