package org.dcm4che3.conf.core.misc;


import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.normalization.OptimisticLockingDecorator;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created by player on 09-Mar-15.
 */
public class OptimisticLockingTest {

    @ConfigurableClass(optimisticLockPropertyName = "optimLock")
    public static class TestConfigClass {
        @ConfigurableProperty(name = "optimLock", tags = ConfigurableProperty.Tag.OPTIMISTIC_LOCK_IGNORE)
        String oLock;
        
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
    
    
    @Test
    public void digest() throws ConfigurationException {

        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("one", "two");


        byte[] h0 = OptimisticLockingDecorator.getHash("zero");
        byte[] h00 = OptimisticLockingDecorator.getHash("zero");
        byte[] h000 = OptimisticLockingDecorator.getHash("zero");
        
        

        byte[] h1 = OptimisticLockingDecorator.getHash("one");
        byte[] h2 = OptimisticLockingDecorator.getHash("two");
        byte[] h3 = OptimisticLockingDecorator.getHash("three");

        OptimisticLockingDecorator.addHash(h0, h1);
        OptimisticLockingDecorator.addHash(h0, h2);
        OptimisticLockingDecorator.addHash(h0, h3);

        OptimisticLockingDecorator.addHash(h00, h2);
        OptimisticLockingDecorator.addHash(h00, h1);
        OptimisticLockingDecorator.addHash(h00, h3);

        OptimisticLockingDecorator.addHash(h000, h3);
        OptimisticLockingDecorator.addHash(h000, h1);
        OptimisticLockingDecorator.addHash(h000, h2);

        /*System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(h0));
        System.out.println( javax.xml.bind.DatatypeConverter.printHexBinary(h00));
        System.out.println( javax.xml.bind.DatatypeConverter.printHexBinary(h000));*/

        Assert.assertArrayEquals(h0, h00);
        Assert.assertArrayEquals(h00, h000);


    }
}
