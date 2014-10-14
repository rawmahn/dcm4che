package org.dcm4che.conf.core;

import org.junit.Assert;
import org.dcm4che.conf.core.impl.XStreamConfigurationStorage;
import org.dcm4che3.conf.api.ConfigurationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Roman K
 */
@RunWith(JUnit4.class)
public class XStreamTest {

    private ConfigurationStorage getConfigurationStorage() {
        return new XStreamConfigurationStorage("c:\\agfa\\tst.xml");
    }

    @Test
    public void testSave() throws ConfigurationException {
        ConfigurationStorage xCfg = getConfigurationStorage();

        Map<String, Object> p1 = new HashMap<String, Object>();
        p1.put("prop1", 56);
        p1.put("prop2", "I am cool");

        Map<String, Object> p2 = new HashMap<String, Object>();
        p2.put("prop11", true);
        p2.put("prop22", new int[]{1,2,3});

        Map<String, Object> p3 = new HashMap<String, Object>();
        p3.put("p1", p1);
        p3.put("p2", p2);

        xCfg.persistNode("/", p3, null);

        xCfg = getConfigurationStorage();

        DeepEquals.deepEquals(p3, xCfg.getConfigurationNode("/"));

        xCfg.persistNode("/p2/newProp",p1,null);

        xCfg.removeNode("/p2/prop11");

    }



    @Test
    public void nodeExists() throws ConfigurationException {
        ConfigurationStorage configurationStorage = getConfigurationStorage();
        configurationStorage.persistNode("/", new HashMap<String, Object>(), null);
        Assert.assertEquals(configurationStorage.nodeExists("asd/fdg/sdsf"), false);

    }
}
