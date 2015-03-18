package org.dcm4che3.conf.core.misc;


import junit.framework.AssertionFailedError;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.SimpleStorageTest;
import org.dcm4che3.conf.core.VitalizerTest;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.normalization.OLockExtractingFilter;
import org.dcm4che3.conf.core.normalization.OLockHashCalcFilter;
import org.dcm4che3.conf.core.normalization.OptimisticLockingDecorator;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.conf.core.util.NodeTraverser;
import org.dcm4che3.conf.core.util.PathPattern;
import org.dcm4che3.conf.dicom.DicomPath;
import org.dcm4che3.conf.dicom.util.DicomNodeTraverser;
import org.dcm4che3.net.Device;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by player on 09-Mar-15.
 */
public class OptimisticLockingTest {

    @ConfigurableClass(optimisticLockPropertyName = "oLock")
    public static class PartyPlan {

        @ConfigurableProperty(tags = ConfigurableProperty.Tag.OPTIMISTIC_LOCK_IGNORE)
        String olock;
        
        @ConfigurableProperty
        String name;
        
        @ConfigurableProperty
        int budget; 
        
        @ConfigurableProperty
        Map<String, Party> parties;

        public String getOlock() {
            return olock;
        }

        public void setOlock(String olock) {
            this.olock = olock;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBudget() {
            return budget;
        }

        public void setBudget(int budget) {
            this.budget = budget;
        }

        public Map<String, Party> getParties() {
            return parties;
        }

        public void setParties(Map<String, Party> parties) {
            this.parties = parties;
        }
    }
    
    @ConfigurableClass(optimisticLockPropertyName = "optimLock")
    public static class Party {
        @ConfigurableProperty(name = "optimLock", tags = ConfigurableProperty.Tag.OPTIMISTIC_LOCK_IGNORE)
        String oLock;
        
        @ConfigurableProperty(name = "prop1")
        int guests;

        @ConfigurableProperty(name = "prop2")
        boolean outside;

        @ConfigurableProperty(name = "str")
        String occasion;

        public String getoLock() {
            return oLock;
        }

        public void setoLock(String oLock) {
            this.oLock = oLock;
        }

        public int getGuests() {
            return guests;
        }

        public void setGuests(int guests) {
            this.guests = guests;
        }

        public boolean isOutside() {
            return outside;
        }

        public void setOutside(boolean outside) {
            this.outside = outside;
        }

        public String getOccasion() {
            return occasion;
        }

        public void setOccasion(String occasion) {
            this.occasion = occasion;
        }
    }
    
    
    @Test
    public void testPartyOlock() throws ConfigurationException, IOException {
        PartyPlan partyPlan = new PartyPlan();
        partyPlan.setName("great Plan!");
        partyPlan.setBudget(10000);

        Party party = new Party();
        party.setGuests(10);
        party.setOccasion("gettogether");
        party.setOutside(true);

        Party party1 = new Party();
        party1.setGuests(5);
        party1.setOccasion("boring birthday");
        party1.setOutside(false);

        HashMap<String, Party> parties = new HashMap<String, Party>();
        parties.put("p1", party);
        parties.put("p2", party1);
        
        partyPlan.setParties(parties);


        BeanVitalizer beanVitalizer = new BeanVitalizer();
        Map<String, Object> oldNode = beanVitalizer.createConfigNodeFromInstance(partyPlan);

        

        NodeTraverser nodeTraverser = new NodeTraverser();
        nodeTraverser.traverseTree(oldNode, PartyPlan.class, new OLockHashCalcFilter());

        // consistent?        
        Assert.assertEquals(oldNode.get("oLock"),"216D681F9E2A754D49971F3D700EE16B68FF36D2");
        //new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, (Object) oldNode);

        
        // remember old hash, change smth, check
        party1.setGuests(party1.getGuests() + 1);

        Map<String, Object> newNode = beanVitalizer.createConfigNodeFromInstance(partyPlan);
        nodeTraverser.traverseTree(newNode,PartyPlan.class,new OLockHashCalcFilter());

        Assert.assertNotEquals("node changed",
                ConfigNodeUtil.getNode(oldNode, "/parties/p2/optimLock"),
                ConfigNodeUtil.getNode(newNode, "/parties/p2/optimLock"));

        Assert.assertEquals("node not changed",
                ConfigNodeUtil.getNode(oldNode, "/parties/p1/optimLock"),
                ConfigNodeUtil.getNode(newNode, "/parties/p1/optimLock"));
        
        Assert.assertEquals("parent should not change",
                ConfigNodeUtil.getNode(oldNode, "/oLock"),
                ConfigNodeUtil.getNode(newNode, "/oLock"));


        // try changing map keys 
        parties.put("aNewOldParty", parties.remove("p1"));

        Map<String, Object> nodeWithMapChanged = beanVitalizer.createConfigNodeFromInstance(partyPlan);
        nodeTraverser.traverseTree(nodeWithMapChanged,PartyPlan.class,new OLockHashCalcFilter());

        Assert.assertNotEquals("parent should change",
                ConfigNodeUtil.getNode(newNode, "/oLock"),
                ConfigNodeUtil.getNode(nodeWithMapChanged, "/oLock"));

        Assert.assertEquals("map entry hash should not change",
                ConfigNodeUtil.getNode(newNode, "/parties/p1/optimLock"),
                ConfigNodeUtil.getNode(nodeWithMapChanged, "/parties/aNewOldParty/optimLock"));

        
        
    }
    
    @Test
    public void digest() throws ConfigurationException {

        byte[] h0 = OLockHashCalcFilter.getHash("zero");
        byte[] h00 = OLockHashCalcFilter.getHash("zero");
        byte[] h000 = OLockHashCalcFilter.getHash("zero");

        byte[] h1 = OLockHashCalcFilter.getHash("one");
        byte[] h2 = OLockHashCalcFilter.getHash("two");
        byte[] h3 = OLockHashCalcFilter.getHash("three");

        OLockHashCalcFilter.addHash(h0, h1);
        OLockHashCalcFilter.addHash(h0, h2);
        OLockHashCalcFilter.addHash(h0, h3);

        OLockHashCalcFilter.addHash(h00, h2);
        OLockHashCalcFilter.addHash(h00, h1);
        OLockHashCalcFilter.addHash(h00, h3);

        OLockHashCalcFilter.addHash(h000, h3);
        OLockHashCalcFilter.addHash(h000, h1);
        OLockHashCalcFilter.addHash(h000, h2);

        /*System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(h0));
        System.out.println( javax.xml.bind.DatatypeConverter.printHexBinary(h00));
        System.out.println( javax.xml.bind.DatatypeConverter.printHexBinary(h000));*/

        Assert.assertArrayEquals(h0, h00);
        Assert.assertArrayEquals(h00, h000);


    }


    @Test
    public void testConnectionOlockHash() throws ConfigurationException, IOException {

        Configuration mockDicomConfStorage = SimpleStorageTest.getMockDicomConfStorage();
        Map<String,Object> configurationNode1 = (Map<String, Object>) mockDicomConfStorage.getConfigurationNode(DicomPath.DeviceByName.set("deviceName", "dcmqrscp").path(), Device.class);

        new DicomNodeTraverser(new ArrayList<Class<?>>()).traverseTree(configurationNode1, Device.class, new OLockHashCalcFilter());

        Assert.assertEquals("BA7C7621709912234F89C4D9BD96A3DD5FB29417", ConfigNodeUtil.getNode(configurationNode1,"/dicomConnection[cn='dicom']/oLock"));
        
        // extract in '_old_olock' in node being persisted
        new DicomNodeTraverser(new ArrayList<Class<?>>()).traverseTree(configurationNode1, Device.class, new OLockExtractingFilter("_old_olock"));
        new DicomNodeTraverser(new ArrayList<Class<?>>()).traverseTree(configurationNode1, Device.class, new OLockHashCalcFilter());

        Assert.assertEquals("BA7C7621709912234F89C4D9BD96A3DD5FB29417", ConfigNodeUtil.getNode(configurationNode1,"/dicomConnection[cn='dicom']/oLock"));

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, configurationNode1);

    }
    
    @Test
    public void testDeco() throws ConfigurationException, IOException {

        Configuration mockDicomConfStorage = SimpleStorageTest.getMockDicomConfStorage();
        
        List<PathPattern> paths = new ArrayList<PathPattern>();
        paths.add(DicomPath.DeviceByName.getPattern());
        Configuration lockedConfig = new OptimisticLockingDecorator(mockDicomConfStorage, paths, new ArrayList<Class<?>>());

        // imitate 3 users simultaneously getting the same node

        Map<String,Object> configurationNode1 = (Map<String, Object>) lockedConfig.getConfigurationNode(DicomPath.DeviceByName.set("deviceName", "dcmqrscp").path(), Device.class);
        Map<String,Object> configurationNode2 = (Map<String, Object>) lockedConfig.getConfigurationNode(DicomPath.DeviceByName.set("deviceName", "dcmqrscp").path(), Device.class);
        Map<String,Object> configurationNode3 = (Map<String, Object>) lockedConfig.getConfigurationNode(DicomPath.DeviceByName.set("deviceName", "dcmqrscp").path(), Device.class);

        // imitate simultaneous changes

        ConfigNodeUtil.replaceNode(configurationNode1,"/dicomNetworkAE/DCMQRSCP/dicomAssociationAcceptor", false);
        ConfigNodeUtil.replaceNode(configurationNode2,"/dicomNetworkAE/DCMQRSCP/dicomAssociationInitiator", false);
        ConfigNodeUtil.replaceNode(configurationNode3,"/dicomConnection[cn='dicom']/dcmIdleTimeout", 100);
        
        // persist some changes from 1st user
        lockedConfig.persistNode(DicomPath.DeviceByName.set("deviceName", "dcmqrscp").path(), configurationNode1, Device.class);

        // persist some conflicting changes from 2nd user - should fail
        try {
            lockedConfig.persistNode(DicomPath.DeviceByName.set("deviceName", "dcmqrscp").path(), configurationNode2, Device.class);
            throw new AssertionFailedError("Should have failed!");
        } catch (Exception e) {
            // its ok
        }

        // persist some non-conflicting changes from 3rd user - should be fine
        lockedConfig.persistNode(DicomPath.DeviceByName.set("deviceName", "dcmqrscp").path(), configurationNode3, Device.class);

        
        
        // assert the changes that were supposed to be persisted
        Map<String,Object> newNode = (Map<String, Object>) lockedConfig.getConfigurationNode(DicomPath.DeviceByName.set("deviceName", "dcmqrscp").path(), Device.class);

        Assert.assertEquals(
                100,
                ConfigNodeUtil.getNode(newNode, "/dicomConnection[cn='dicom']/dcmIdleTimeout"));

        Assert.assertEquals(
                false,
                ConfigNodeUtil.getNode(newNode, "/dicomNetworkAE/DCMQRSCP/dicomAssociationAcceptor"));

        Assert.assertEquals(
                true,
                ConfigNodeUtil.getNode(newNode, "/dicomNetworkAE/DCMQRSCP/dicomAssociationInitiator"));

        //new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(System.out, newNode);
        
    }
}
