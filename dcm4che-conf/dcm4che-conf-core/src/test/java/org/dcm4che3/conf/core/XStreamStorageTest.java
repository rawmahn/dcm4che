package org.dcm4che3.conf.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcm4che3.conf.core.misc.DeepEqualsDiffer;
import org.junit.Assert;
import org.dcm4che3.conf.core.impl.XStreamConfigurationStorage;
import org.dcm4che3.conf.api.ConfigurationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

/**
 * @author Roman K
 */
@RunWith(JUnit4.class)
public class XStreamStorageTest {

    public static Configuration getConfigurationStorage() {
        return new XStreamConfigurationStorage("target/tst.xml", true);
    }

    @Test
    public void testSave() throws ConfigurationException {
        Configuration xCfg = getConfigurationStorage();

        Map<String, Object> p1 = new HashMap<String, Object>();
        p1.put("prop1", 56);
        p1.put("prop2", "I am cool");

        Map<String, Object> p2 = new HashMap<String, Object>();
        p2.put("prop1", true);
        p2.put("prop2", Arrays.asList(1, 2, 3));

        Map<String, Object> p3 = new HashMap<String, Object>();
        p3.put("p1", p1);
        p3.put("p2", p2);

        xCfg.persistNode("/", p3, null);

        xCfg = getConfigurationStorage();

        DeepEqualsDiffer.assertDeepEquals("Stored config node must be equal to the one loaded", p3, xCfg.getConfigurationNode("/"));

        xCfg.persistNode("/p2/newProp",p1,null);

        xCfg.removeNode("/p2/prop11");

        Iterator search = xCfg.search("/*[contains(prop2,'I am ')]");
         Object o = search.next();
        DeepEqualsDiffer.assertDeepEquals("Search should work. ", o, p1);

    }



    @Test
    public void nodeExists() throws ConfigurationException {
        Configuration configurationStorage = getConfigurationStorage();
        configurationStorage.persistNode("/", new HashMap<String, Object>(), null);
        Assert.assertEquals(configurationStorage.nodeExists("asd/fdg/sdsf"), false);

    }

    @Test
    public void testSpecialSymbols() throws ConfigurationException {
        Configuration xCfg = getConfigurationStorage();

        ObjectMapper om = new ObjectMapper();
        // serialize to confignode



        Map<String, Object> pd = new HashMap<String, Object>();
        pd.put("_prop", "hey");
        pd.put("prop1", Arrays.asList(1, 2, 3));

        Map<String, Object> p2 = new HashMap<String, Object>();
        p2.put("device1", pd);
        p2.put("device2", Arrays.asList(1, 2, 3));

        Map<String, Object> p1 = new HashMap<String, Object>();
        p1.put("dicomDevicesRoot", p2);
        p1.put("Unique AE Titles Registry", "I am cool");

        Map<String, Object> p3 = new HashMap<String, Object>();
        p3.put("dicomConfigurationRoot", p1);

        xCfg.persistNode("/", p3, null);

        Assert.assertEquals(xCfg.search("/dicomConfigurationRoot/dicomDevicesRoot/device1/_prop").next().toString(),"hey");

    }
}
