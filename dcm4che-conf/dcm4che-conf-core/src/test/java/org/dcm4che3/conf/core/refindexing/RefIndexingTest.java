package org.dcm4che3.conf.core.refindexing;

import org.dcm4che3.conf.core.DefaultBeanVitalizer;
import org.dcm4che3.conf.core.api.Configuration;
import org.dcm4che3.conf.core.api.Path;
import org.dcm4che3.conf.core.storage.InMemoryConfiguration;
import org.dcm4che3.conf.core.storage.ReferenceIndexingDecorator;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class RefIndexingTest {

    private HashMap<String, Path> uuidToSimplePathCache;
    private Configuration configuration;
    private DefaultBeanVitalizer vitalizer;

    @Before
    public void init() {
        configuration = new InMemoryConfiguration();
        uuidToSimplePathCache = new HashMap<String, Path>();
        configuration = new ReferenceIndexingDecorator(configuration, uuidToSimplePathCache);
        vitalizer = new DefaultBeanVitalizer();
    }

    @Test
    public void testSimpleChanges() {


        SampleBigConf sampleBigConf = new SampleBigConf();

        sampleBigConf.child1 = new SampleReferableConfClass("UUID1");
        sampleBigConf.childList.add(new SampleReferableConfClass("UUID2"));
        sampleBigConf.childList.add(new SampleReferableConfClass("UUID3"));

        sampleBigConf.childMap.put("first", new SampleReferableConfClass("UUID4"));
        sampleBigConf.childMap.put("second", new SampleReferableConfClass("UUID5"));

        configuration.persistNode("/confRoot/bigConf1", vitalizer.createConfigNodeFromInstance(sampleBigConf), null);

        // check if uuids are indexed
        System.out.println(uuidToSimplePathCache.get("UUID1"));


    }

    public void testExistingUUID(){
        //TODO
    }



}
