package org.dcm4che3.conf.core.misc;


import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.VitalizerTest;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.normalization.OLockHashCalcFilter;
import org.dcm4che3.conf.core.util.ConfigNodeUtil;
import org.dcm4che3.conf.core.util.NodeTraverser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
}
