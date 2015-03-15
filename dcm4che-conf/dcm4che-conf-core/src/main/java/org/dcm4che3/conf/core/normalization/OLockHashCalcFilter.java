package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.util.NodeTraverser;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Stack;

/**
* Calculates and simply sets olock hashes for a node tree
*/
public class OLockHashCalcFilter extends NodeTraverser.NoopFilter {

    final Deque<byte[]> stack = new ArrayDeque<byte[]>();

    public static void addHash(byte[] one, byte[] two) {

        for (int i = 0; i < one.length; i++) {
            one[i] = (byte) (one[i] + two[i]);
        }


    }

    public static byte[] getHash(String what)  {
        MessageDigest cript = null;
        try {
            cript = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        cript.reset();
        cript.update(what.getBytes(StandardCharsets.UTF_8));
        return cript.digest();

    }


    public Deque<byte[]> getStack() {
        return stack;
    }

    @Override
    public void onPrimitiveNode(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {

        Object o = containerNode.get(property.getAnnotatedName());
        byte[] hash;

        // primitive map
        if (o instanceof Map) {
            hash = getHash(property.getAnnotatedName());
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) o).entrySet()) {
                addHash(hash, getHash(entry.getKey() + entry.getValue()));
            }
        } else {
            hash = getHash(property.getAnnotatedName() + o);
        }

        // add up to hash for all but optimistic lock props
        if (!property.getTags().contains(ConfigurableProperty.Tag.OPTIMISTIC_LOCK_IGNORE))
            addHash(stack.peek(), hash);


        logHashes("added primitive "+property.getAnnotatedName());
    }

    private void logHashes(String s) {
//        System.out.println();
//        System.out.println(s);
//
//        for (byte[] bytes : stack) {
//            System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(bytes));
//        }
    }
    
    @Override
    public void applyRefNodeFilter(Object node, Class nodeClass) {
        addHash(stack.peek(), getHash(node.toString()));
    }
    
    @Override
    public void onSubNodeBegin(AnnotatedConfigurableProperty property) {
        stack.push(getHash("nothing"));
    }

    @Override
    public void onSubNodeEnd(AnnotatedConfigurableProperty property) {
        byte[] pop = stack.pop();
        addHash(stack.peek(), getHash(property.getAnnotatedName() + javax.xml.bind.DatatypeConverter.printHexBinary(pop)));
    }

    @Override
    public void onListBegin(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
        stack.push(getHash("list"));
    }

    @Override
    public void onListElementBegin() throws ConfigurationException {
        stack.push(getHash("listelem"));

    }

    @Override
    public void onListElementEnd() throws ConfigurationException {
        stack.push(getHash(javax.xml.bind.DatatypeConverter.printHexBinary(stack.pop()) + javax.xml.bind.DatatypeConverter.printHexBinary(stack.pop())));
    }

    @Override
    public void onListEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
        byte[] pop = stack.pop();
        addHash(stack.peek(), getHash(property.getAnnotatedName() + javax.xml.bind.DatatypeConverter.printHexBinary(pop)));
    }

    @Override
    public void onMapBegin(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
        stack.push(getHash("map"));

    }

    @Override
    public void onMapEntryBegin(String key) throws ConfigurationException {
        stack.push(getHash("mapentry"));
    }

    @Override
    public void onMapEntryEnd(String key) throws ConfigurationException {
        byte[] pop = stack.pop();
        addHash(stack.peek(), getHash(key + javax.xml.bind.DatatypeConverter.printHexBinary(pop)));

    }

    @Override
    public void onMapEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
        byte[] pop = stack.pop();
        addHash(stack.peek(), getHash(property.getAnnotatedName() + javax.xml.bind.DatatypeConverter.printHexBinary(pop)));
    }


    
    
    @Override
    public void onNodeBegin(Map<String, Object> node, Class clazz) throws ConfigurationException {
        //TODO: check if optimistic locking is enabled for class 

        // if the class uses optimistic locking, reset counter
        ConfigurableClass clazzAnnotation = (ConfigurableClass) clazz.getAnnotation(ConfigurableClass.class);
        if (clazzAnnotation != null && !clazzAnnotation.optimisticLockPropertyName().equals(ConfigurableClass.NO_LOCK_PROP))
            stack.push(getHash("newNode"));
        
        super.onNodeBegin(node, clazz);
    }

    @Override
    public void onNodeEnd(Map<String, Object> node, Class clazz) throws ConfigurationException {

        // if the class uses optimistic locking, set the olock property to what have been calculated
        ConfigurableClass clazzAnnotation = (ConfigurableClass) clazz.getAnnotation(ConfigurableClass.class);
        if (clazzAnnotation != null && !clazzAnnotation.optimisticLockPropertyName().equals(ConfigurableClass.NO_LOCK_PROP))
            node.put(clazzAnnotation.optimisticLockPropertyName(), javax.xml.bind.DatatypeConverter.printHexBinary(stack.pop()));
        
        super.onNodeEnd(node, clazz);
    }
}
