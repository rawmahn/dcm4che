import com.fasterxml.jackson.databind.ObjectMapper
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


    @Test
    void testSearch() {
        def storage = XStreamStorageTest.getConfigurationStorage()

        def json = """{
          "dicomConfigurationRoot": {
            "dicomDeviceRoot": {
              "device1":{
                "dicomDeviceName":"device1",
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
                    "secondSneakyProp":"the/slash"

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
                ]
              }

            }
          }
        }
        """

        Map configNode = jsonToMap(json)

        storage.persistNode("/", configNode, null)

        def res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomConnection[dicomPort=101 and dicomHostname='myhl7']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(101)

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomConnection[sneakyProp='thebackslash\\']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(101)

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomConnection[secondSneakyProp='the/slash']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(101)

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/*[dicomDeviceName='deviceWith/Slash']/dicomConnection[cn='tls']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(2222)

        res = storage.search("dicomConfigurationRoot/dicomDeviceRoot/*[dicomDeviceName='deviceWith&apos;Quote']/dicomConnection[cn='tls']")
        assert ((Map<String, Object>) res.next()).get("dicomPort").equals(2222)

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

        //ConfigNodeUtil.parseReference("dicomConfigurationRoot/dicomDeviceRoot/device1/dicomConnection[dicomPort=101]")
        assert res == ConfigNodeUtil.parseReference("/dicomConfigurationRoot/dicomDeviceRoot/*[deviceName='Qoute&apos;here']/dicomConnection[dicomPort=101 and dicomHostname='myhl7' and dicomInstalled=true]");
    }

}
