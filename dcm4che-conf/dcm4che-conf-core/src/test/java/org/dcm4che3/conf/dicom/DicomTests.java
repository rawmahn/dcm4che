package org.dcm4che3.conf.dicom;

import org.dcm4che3.data.Issuer;
import org.dcm4che3.net.Device;

/**
 * Created by aprvf on 24/10/2014.
 */
public class DicomTests {


    public Device getDevice() {
        final Device device = new Device("aDevice");
        device.setIssuerOfPatientID(new Issuer("1.2", "0.0.1", "1.2.3.4"));
        device.setKeyStoreKeyPin("23");
        return device;
    }


}
