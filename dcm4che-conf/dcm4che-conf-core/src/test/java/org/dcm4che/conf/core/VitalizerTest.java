package org.dcm4che.conf.core;

import org.junit.Assert;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ConfigurableClass;
import org.dcm4che3.conf.api.generic.ConfigurableProperty;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

/**
 * Created by aprvf on 14/10/2014.
 */
@RunWith(JUnit4.class)
public class VitalizerTest {

    BeanVitalizer getVitalizer() {
        return new BeanVitalizer();

    }

    @ConfigurableClass
    public static class TestConfigSubClass {
        @ConfigurableProperty(name = "prop1")
        int prop1;

        @ConfigurableProperty(name = "prop2")
        boolean prop2;

        @ConfigurableProperty(name = "str")
        String s;

        public int getProp1() {
            return prop1;
        }

        public void setProp1(int prop1) {
            this.prop1 = prop1;
        }

        public boolean isProp2() {
            return prop2;
        }

        public void setProp2(boolean prop2) {
            this.prop2 = prop2;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }
    }



    @ConfigurableClass
    public static class TestConfigClass {

        @ConfigurableProperty(name = "prop1")
        int prop1;

        @ConfigurableProperty(name = "intProp1")
        int prop2;

        @ConfigurableProperty(name = "boolProp1")
        boolean boolProp;

        @ConfigurableProperty(name = "boolProp2")
        Boolean boolProp2;

        @ConfigurableProperty(name = "strProp")
        String str;

        @ConfigurableProperty(name = "subProp1")
        TestConfigSubClass subProp1;

        @ConfigurableProperty(name = "subProp2")
        TestConfigSubClass subP;

        @ConfigurableProperty(name = "subMap")
        Map<String,String> mapProp;

        @ConfigurableProperty(name="objSubMap")
        Map<String, TestConfigSubClass> objMapProp;

        @ConfigurableProperty(name="setProp")
        Set<String> aSet;

        @ConfigurableProperty(name="strArrayProp")
        String[] strArrayProp;

        @ConfigurableProperty(name="boolArrayProp")
        boolean[] boolArrayProp;

        @ConfigurableProperty(name="intArrayProp")
        int[] intArrayProp;

        public Set<String> getaSet() {
            return aSet;
        }

        public void setaSet(Set<String> aSet) {
            this.aSet = aSet;
        }

        public String[] getStrArrayProp() {
            return strArrayProp;
        }

        public void setStrArrayProp(String[] strArrayProp) {
            this.strArrayProp = strArrayProp;
        }

        public boolean[] getBoolArrayProp() {
            return boolArrayProp;
        }

        public void setBoolArrayProp(boolean[] boolArrayProp) {
            this.boolArrayProp = boolArrayProp;
        }

        public int[] getIntArrayProp() {
            return intArrayProp;
        }

        public void setIntArrayProp(int[] intArrayProp) {
            this.intArrayProp = intArrayProp;
        }

        public int getProp1() {
            return prop1;
        }

        public void setProp1(int prop1) {
            this.prop1 = prop1;
        }

        public int getProp2() {
            return prop2;
        }

        public void setProp2(int prop2) {
            this.prop2 = prop2;
        }

        public boolean isBoolProp() {
            return boolProp;
        }

        public void setBoolProp(boolean boolProp) {
            this.boolProp = boolProp;
        }

        public Boolean getBoolProp2() {
            return boolProp2;
        }

        public void setBoolProp2(Boolean boolProp2) {
            this.boolProp2 = boolProp2;
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public TestConfigSubClass getSubProp1() {
            return subProp1;
        }

        public void setSubProp1(TestConfigSubClass subProp1) {
            this.subProp1 = subProp1;
        }

        public TestConfigSubClass getSubP() {
            return subP;
        }

        public void setSubP(TestConfigSubClass subP) {
            this.subP = subP;
        }

        public Map<String, String> getMapProp() {
            return mapProp;
        }

        public void setMapProp(Map<String, String> mapProp) {
            this.mapProp = mapProp;
        }

        public Map<String, TestConfigSubClass> getObjMapProp() {
            return objMapProp;
        }

        public void setObjMapProp(Map<String, TestConfigSubClass> objMapProp) {
            this.objMapProp = objMapProp;
        }
    }

    public static class ConfClassWithSetters {

    }



    HashMap<String, Object> getTestConfigClassMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("prop1", 21);
        map.put("intProp1", 111);
        map.put("boolProp1", false);
        map.put("boolProp2", true);
        map.put("strProp", "a cool str");

        HashMap<String, Object> subClassInst1 = new HashMap<String, Object>();
        subClassInst1.put("prop1", 123);
        subClassInst1.put("prop2", false);
        subClassInst1.put("str", "hallo!!");

        HashMap<String, Object> subClassInst2 = new HashMap<String, Object>();
        subClassInst2.put("prop1", 3323);
        subClassInst2.put("prop2", true);
        subClassInst2.put("str", "hallo noch mal!!");

        map.put("subProp1", subClassInst1);
        map.put("subProp2", subClassInst2);

        Map<String, String> m = new HashMap<String, String>();
        m.put("what1", "that1");
        m.put("waht2", "that2");

        map.put("subMap", m);

        HashMap<String, Object> subClassElem1 = new HashMap<String, Object>();
        subClassElem1.put("prop1", 50);
        subClassElem1.put("prop2", false);
        subClassElem1.put("str", "heey    !!");

        HashMap<String, Object> subClassElem2 = new HashMap<String, Object>();
        subClassElem2.put("prop1", 1);
        subClassElem2.put("prop2", true);
        subClassElem2.put("str", "wow much number such true wow");

        HashMap<String, Map> subClassHashMap = new HashMap<String, Map>();
        subClassHashMap.put("elem1", subClassElem1);
        subClassHashMap.put("elem2", subClassElem2);

        map.put("objSubMap", subClassHashMap);

        map.put("setProp", new HashSet<String>(Arrays.asList("abc", "cde", "efg")));

        map.put("strArrayProp", Arrays.asList("abc", "cde", "efg"));
        map.put("intArrayProp", Arrays.asList(1, 2, 3));
        map.put("boolArrayProp", Arrays.asList(true, false, true));

        return map;
    }

    @Test
    public void testBackAndForthTestConfigClass() throws ConfigurationException {
        HashMap<String, Object> testConfigClassNode = getTestConfigClassMap();
        BeanVitalizer beanVitalizer = new BeanVitalizer();

        TestConfigClass configuredInstance = beanVitalizer.newConfiguredInstance(TestConfigClass.class, testConfigClassNode);
        Object generatedNode = beanVitalizer.createConfigNodeFromInstance(configuredInstance);

        //boolean b = DeepEquals.deepEquals(testConfigClassNode, generatedNode);
        //Assert.assertTrue("Objects should be equal.Last pair of unmatched properties:"+DeepEquals.getLastPair(), b);


        DeepEqualsDiffer.assertDeepEquals("Config node before deserialization must be the same as after serializing back", testConfigClassNode, generatedNode);
        //boolean b = DeepEquals.deepEquals(testConfigClassNode, generatedNode);
        //Assert.assertTrue("Config node before deserialization must be the same as after serializing back. Last keys"+DeepEquals.lastDualKey,b);

    }
    @Test
    public void testPerformance() throws ConfigurationException {
        HashMap<String, Object> testConfigClassNode = getTestConfigClassMap();
        BeanVitalizer beanVitalizer = new BeanVitalizer();

        for (int i=0;i<10000;i++)
        {
            TestConfigClass configuredInstance = beanVitalizer.newConfiguredInstance(TestConfigClass.class, testConfigClassNode);
            Object generatedNode = beanVitalizer.createConfigNodeFromInstance(configuredInstance);
        }

    }

}
