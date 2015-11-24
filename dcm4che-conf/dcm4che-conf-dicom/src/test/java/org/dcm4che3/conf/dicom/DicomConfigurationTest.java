/*
 * *** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *  Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 *  The Initial Developer of the Original Code is
 *  Agfa Healthcare.
 *  Portions created by the Initial Developer are Copyright (C) 2015
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *  See @authors listed below
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 2 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 *
 *  ***** END LICENSE BLOCK *****
 */

package org.dcm4che3.conf.dicom;

import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman K
 */
@RunWith(JUnit4.class)
public class DicomConfigurationTest {

    @Test
    public void renameAETest() throws ConfigurationException {

        CommonDicomConfigurationWithHL7 config = SimpleStorageTest.createCommonDicomConfiguration();


        // create device
        String aeRenameTestDevice = "AERenameTestDevice";

        Device testDevice = createDevice(aeRenameTestDevice);

        config.removeDevice(aeRenameTestDevice);
        config.persist(testDevice);

        // replace connection
        testDevice.getApplicationEntity("aet1").setAETitle("aet2");
        config.merge(testDevice);

        // see if there is only aet2
        Device deviceLoaded = config.findDevice(aeRenameTestDevice);

        Assert.assertEquals("There must stay only 1 ae", 1, deviceLoaded.getApplicationEntities().size());

        Assert.assertEquals("The new aet must have 1 connection", 1, deviceLoaded.getApplicationEntity("aet2").getConnections().size());

    }

    @Test
    public void testSearchByUUID() throws ConfigurationException {
        CommonDicomConfigurationWithHL7 config = SimpleStorageTest.createCommonDicomConfiguration();

        config.purgeConfiguration();

        Device device = new Device("ABC");
        ApplicationEntity ae1 = new ApplicationEntity("myAE1");
        ApplicationEntity ae2 = new ApplicationEntity("myAE2");


        String uuid1 = ae1.getUuid();
        String uuid2 = ae2.getUuid();

        device.addApplicationEntity(ae1);
        device.addApplicationEntity(ae2);
        config.persist(device);

        Device device2 = new Device("CDE");
        ApplicationEntity ae3 = new ApplicationEntity("myAE3");

//        String devUUID = device2.getUuid();

        String uuid3 = ae3.getUuid();

        device2.addApplicationEntity(ae3);
        config.persist(device2);

        Assert.assertEquals("myAE1", config.findApplicationEntityByUUID(uuid1).getAETitle());
        Assert.assertEquals("myAE2",config.findApplicationEntityByUUID(uuid2).getAETitle());
        Assert.assertEquals("myAE3",config.findApplicationEntityByUUID(uuid3).getAETitle());

//        Assert.assertEquals("CDE", config.findDeviceByUUID(devUUID).getDeviceName());


        try {
            config.findApplicationEntityByUUID("nonexistent");
            Assert.fail("An AE should have not been found");
        } catch (ConfigurationNotFoundException e) {
            // noop
        }
    }


    @Test
    public void testByAnyUUIDSearch() {
        throw new RuntimeException("hey!");
    }

    // AE refs self
    // AE refs Connection refs original AE
    // Device refs self's AE refs self Connection refs self Device

    private Device createDevice(String aeRenameTestDevice) {
        Device testDevice = new Device(aeRenameTestDevice);
        Connection connection = new Connection();
        connection.setProtocol(Connection.Protocol.DICOM);
        connection.setCommonName("myConn");
        connection.setHostname("localhost");

        ApplicationEntity ae = new ApplicationEntity();
        List<Connection> list = new ArrayList<Connection>();
        list.add(connection);

        testDevice.addConnection(connection);
        ae.setConnections(list);
        ae.setAETitle("aet1");
        testDevice.addApplicationEntity(ae);
        return testDevice;
    }
}
