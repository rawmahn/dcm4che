package org.dcm4che.conf.core;

import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.net.Device;

import java.util.Map;

/**
 * Created by aprvf on 29/09/2014.
 */
@ConfigClass(path="DicomConfiguration")
public class TestConfigurableObject {

    @ConfigField(name="myProp1")
    String myProp1;

    // maybe split device and device extensions at all in the backend?

    @ConfigField(name="Devices", path="root/nestedstuff")
    Map<String, NestedObj> nested;


    @ConfigClass
    public class NestedObj {

        @ConfigField(name = "ss")
        int prop2;
    }
}
