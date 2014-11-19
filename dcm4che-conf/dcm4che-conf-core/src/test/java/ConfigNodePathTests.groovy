/*
 * **** BEGIN LICENSE BLOCK *****
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
 *  Portions created by the Initial Developer are Copyright (C) 2014
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
import com.fasterxml.jackson.databind.ObjectMapper
import junit.extensions.TestSetup
import org.apache.commons.jxpath.ri.Compiler
import org.apache.commons.jxpath.ri.Parser
import org.apache.commons.jxpath.ri.compiler.TreeCompiler
import org.dcm4che3.conf.core.XStreamStorageTest
import org.dcm4che3.conf.core.impl.XStreamConfigurationStorage
import org.dcm4che3.conf.core.util.ConfigNodeUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Created by aprvf on 07/11/2014.
 */
@RunWith(JUnit4.class)
class ConfigNodePathTests {

    def dicomConfJsonMock = """{
          "dicomConfigurationRoot": {
            "dicomDeviceRoot": {
              "device1":{
                "dicomDeviceName":"device1",
                "dicomNetworkAE":
                [
                  {
                    "dicomAETitle":"aTitle",
                    "connection":"conn1Ref",
                    "aeExtensions":{
                        "hiExt": {"extensionName":"hiExt",
                                "someProp":123},
                        "SomeOtherExt": {"extensionName":"SomeOtherExt",
                                "someProp":123}
                    }
                  },
                  {
                    "dicomAETitle":"aTitle1",
                    "connection":"conn1Ref"
                  }
                ],
                "dicomConnection":[
                  {
                    "cn": "tls",
                    "dicomHostname":"localhost",
                    "dcmBindAddress":"127.0.0.1",
                    "dicomPort":2222
                  },
                  {
                    "cn": "hl7",
                    "dicomHostname":"myhl7",
                    "dcmBindAddress":"127.0.0.1",
                    "dicomPort":101,
                    "sneakyProp":"thebackslash\\\\",
                    "secondSneakyProp":"the/slash",
                    "arr":[
                      {"n":1,
                       "v":3
                      },
                      {"n":2
                      }
                    ]

                  },
                  {
                    "cn": "hl7-1",
                    "dicomHostname":"myhl7",
                    "dcmBindAddress":"123.26.123.1"
                    }
                ]
              },
              "device2":{"dicomDevicename":"device2"},
              "deviceWith/Slash":{
                "dicomDeviceName":"deviceWith/Slash",
                "dicomConnection":[
                  {
                    "cn": "tls",
                    "dicomHostname":"localhost",
                    "dcmBindAddress":"127.0.0.1",
                    "dicomPort":2222
                  }
                ]
              },
              "deviceWith'Quote":{
                "dicomDeviceName":"deviceWith'Quote",
                "dicomConnection":[
                  {
                    "cn": "tls",
                    "dicomHostname":"localhost",
                    "dcmBindAddress":"127.0.0.1",
                    "dicomPort":2222
                  }
                ],
                "dicomNetworkAE":
                [
                  {
                    "dicomAETitle":"aTitle3",
                    "connection":"conn1Ref"
                  },
                  {
                    "dicomAETitle":"aTitle5",
                    "connection":"conn1Ref"
                  }
                ]
              }

            }
          }
        }
        """
    @Test
    void testSearch() {
        def storage = XStreamStorageTest.getConfigurationStorage()



        Map configNode = jsonToMap(dicomConfJsonMock)

        storage.persistNode("/", configNode, null)

        def res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/*[dicomNetworkAE/dicomAETitle='aTitle1']")
        assert ((Map<String, Object>) res.next()).get("dicomDeviceName").equals("device1")

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomConnection[sneakyProp='thebackslash\\']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(101)

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomConnection[secondSneakyProp='the/slash']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(101)

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/*[dicomDeviceName='deviceWith/Slash']/dicomConnection[cn='tls']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(2222)

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/*[dicomDeviceName='deviceWith&apos;Quote']/dicomConnection[cn='tls']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(2222)

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/*[dicomConnection[dcmBindAddress='123.26.123.1']]")

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/*/dicomDeviceName")
        def dnames = ["device1","deviceWith'Quote","deviceWith/Slash"];
        for (def i=0;i<3;i++)
            assert res.next() == dnames[i]

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/*/dicomNetworkAE/dicomAETitle")
        def atitles = [
                "aTitle",
                "aTitle1",
                "aTitle3",
                "aTitle5"
        ]
        for (def i=0;i<4;i++)
            assert res.next() == atitles[i]

        storage.persistNode("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomNetworkAE[dicomAETitle='aTitle']/aeExtensions/thenewextension",
                [name:"testExt", someProp:"someVal", id:1234], null);

        def node = storage.getConfigurationNode("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomNetworkAE[dicomAETitle='aTitle']")
        assert node == [dicomAETitle:"aTitle", connection:"conn1Ref", aeExtensions:[hiExt:[extensionName:"hiExt", someProp:123], SomeOtherExt:[extensionName:"SomeOtherExt", someProp:123], thenewextension:[name:"testExt", someProp:"someVal", id:1234]]]
    }

    private Map jsonToMap(String json) {
        def mapper = new ObjectMapper()
        def configNode = mapper.readValue(json, Map.class)
        configNode
    }

    private List jsonToList(String json) {
        def mapper = new ObjectMapper()
        def configNode = mapper.readValue(json, List.class)
        configNode
    }

    @Test
    void testParseReference() {

        def res = jsonToList("""            [
                {
                    "\$name":"dicomConfigurationRoot"
                },
                {
                    "\$name":"dicomDeviceRoot"
                },
                {
                    "\$name":"*",
                    "deviceName":"Qoute'here"
                },
                {
                    "\$name":"dicomConnection",
                    "dicomPort" : 101,
                    "dicomHostname": "myhl7",
                    "dicomInstalled":true
                }
            ]""")

        assert res == ConfigNodeUtil.parseReference("/dicomConfigurationRoot/dicomDeviceRoot/*[deviceName='Qoute&apos;here']/dicomConnection[dicomPort=101 and dicomHostname='myhl7' and dicomInstalled=true]");

        // test parse
        // "dicomConfigurationRoot/dicomDeviceRoot/*[dicomNetworkAE/dicomAETitle='aTitle1']"
    }

    @Test
    public void dicomTest() {
        URL resource = ConfigNodePathTests.class.getResource("mockConfig.json");
        def config = new XStreamConfigurationStorage(resource.getPath(), true);

        Iterator search = config.search("dicomConfigurationRoot/dicomDevicesRoot/*[dicomNetworkAE[@name='DCM4CHEE']]");
        assert search.next()['dicomDeviceName'] == "dcm4chee-arc"

//        search = config.search("dicomConfigurationRoot/dicomDevicesRoot[dicomNetworkAE[@name='DCM4CHEE']]");
  //      assert search.next()['dicomDeviceName'] == "dcm4chee-arc"

    }

}
