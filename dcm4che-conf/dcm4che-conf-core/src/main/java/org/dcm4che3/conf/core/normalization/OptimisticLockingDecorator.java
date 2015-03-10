package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.DelegatingConfiguration;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.util.NodeTraverser;
import org.dcm4che3.conf.core.util.PathPattern;
import org.dcm4che3.conf.dicom.util.DicomNodeTraverser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by player on 08-Mar-15.
 */
@SuppressWarnings("unchecked")
public class OptimisticLockingDecorator extends DelegatingConfiguration {

    /**
     * Temporary solution to prevent any inconsistency. 
     * Should contains all path for which persistNode can be called.
     * In the long term, a write to a subnode should just trigger a 
     * full persist of a closest parent with an optimistic lock  
     */
    private List<PathPattern> allowedPersistPaths;
    
    public OptimisticLockingDecorator(Configuration delegate, List<PathPattern> allowedPersistPaths) {
        super(delegate);
        this.allowedPersistPaths = allowedPersistPaths;
    }

    


    @Override
    public void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {

        // validate path
        boolean pathAllowed = false;
        for (PathPattern allowedPersistPath : allowedPersistPaths)
            try {
                allowedPersistPath.parse(path);
                pathAllowed = true;
            } catch (Exception e) {
            }
        
        if (!pathAllowed)
            throw new IllegalArgumentException("Path " + path + " is not allowed by optimistic lock handler.");


        Map<String, Object> nodeInStorage = null;
        try {
            nodeInStorage = (Map<String, Object>) delegate.getConfigurationNode(path, configurableClass);
        } catch (ClassCastException e) {
            super.persistNode(path, configNode, configurableClass);
            return;
        }


    }

    public static NodeTraverser.NoopFilter showOlockNum() {

        final Deque<byte[]> stack = new ArrayDeque<byte[]>();
        stack.push(getHash("nothing"));

        return new NodeTraverser.NoopFilter() {
            @Override
            public void applyFilter(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {

                Object o = containerNode.get(property.getAnnotatedName());
                byte[] hash;

                // primitive map
                if (o instanceof Map) {
                    hash = getHash(property.getAnnotatedName());
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) o).entrySet()) {
                        addHash(hash, getHash(entry.getKey() + entry.getValue()));
                    }
                } else {
                    hash = getHash(property.getAnnotatedName()+o);
                }

                // add up to hash for all but optimistic lock props
                if (!property.getTags().contains(ConfigurableProperty.Tag.OPTIMISTIC_LOCK_IGNORE))
                    addHash(stack.peekLast(), hash);

            }

            @Override
            public void applyRefNodeFilter(Object node, Class nodeClass) {
                addHash(stack.peekLast(), getHash(node.toString()));
            }

            @Override
            public void onSubNodeBegin(AnnotatedConfigurableProperty property) {
                stack.push(getHash("nothing"));
            }

            @Override
            public void onSubNodeEnd(AnnotatedConfigurableProperty property) {
                byte[] pop = stack.pop();
                addHash(stack.peekLast(), getHash(property.getAnnotatedName()+javax.xml.bind.DatatypeConverter.printHexBinary(pop)));
            }

            @Override
            public void onListBegin(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
                stack.push(getHash("list"));
            }

            @Override
            public void onListElementBegin(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
                stack.push(getHash("listelem"));

            }

            @Override
            public void onListElementEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
                stack.push(getHash(javax.xml.bind.DatatypeConverter.printHexBinary(stack.pop())+javax.xml.bind.DatatypeConverter.printHexBinary(stack.pop())));
            }

            @Override
            public void onListEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
                byte[] pop = stack.pop();
                addHash(stack.peekLast(), getHash(property.getAnnotatedName()+javax.xml.bind.DatatypeConverter.printHexBinary(pop)));
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
                addHash(stack.peekLast(), getHash(key+javax.xml.bind.DatatypeConverter.printHexBinary(pop)));

            }

            @Override
            public void onMapEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
                byte[] pop = stack.pop();
                addHash(stack.peekLast(), getHash(property.getAnnotatedName()+javax.xml.bind.DatatypeConverter.printHexBinary(pop)));
            }


        };


    }
    
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

    @Override
    public Object getConfigurationNode(String path, Class configurableClass) throws ConfigurationException {
        return super.getConfigurationNode(path, configurableClass);
    }
}
