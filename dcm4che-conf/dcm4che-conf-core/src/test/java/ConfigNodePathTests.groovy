import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.jxpath.Pointer
import org.dcm4che3.conf.core.XStreamStorageTest
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

        println storage.getConfigurationNode("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomNetworkAE[dicomAETitle='aTitle']")
        storage.persistNode("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomNetworkAE[dicomAETitle='aTitle']/aeExtensions/thenewextension",
                [name:"testExt", someProp:"someVal", id:1234], null);

        println storage.getConfigurationNode("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomNetworkAE[dicomAETitle='aTitle']")
        def node = storage.getConfigurationNode("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomNetworkAE[dicomAETitle='aTitle']")
        assert node == [dicomAETitle:"aTitle", connection:"conn1Ref", aeExtensions:[hiExt:[extensionName:"hiExt", someProp:123], SomeOtherExt:[extensionName:"SomeOtherExt", someProp:123], thenewextension:[name:"testExt", someProp:"someVal", id:1234]]]

        storage.removeNode("dicomConfigurationRoot")


//        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(2222)
        // invalid
        /*res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/deviceWith&apos;Quote/dicomConnection[cn='tls']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(2222)*/
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

}
