package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.DelegatingConfiguration;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.util.NodeTraverser;
import org.dcm4che3.conf.core.util.PathPattern;
import org.dcm4che3.conf.dicom.util.DicomNodeTraverser;
import org.dcm4che3.net.Device;

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
    List<Class<?>> allExtensionClasses;

    public OptimisticLockingDecorator(Configuration delegate, List<PathPattern> allowedPersistPaths, List<Class<?>> allExtensionClasses) {
        super(delegate);
        this.allowedPersistPaths = allowedPersistPaths;
        this.allExtensionClasses = allExtensionClasses;

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


        //TODO: acquire pessimistic lock

        // get existing node from storage
        Map<String, Object> nodeInStorage = null;
        try {
            nodeInStorage = (Map<String, Object>) delegate.getConfigurationNode(path, configurableClass);
        } catch (ClassCastException e) {
            super.persistNode(path, configNode, configurableClass);
            return;
        }

        // calculate olock hashes in existing node
        DicomNodeTraverser dicomNodeTraverser = new DicomNodeTraverser(allExtensionClasses);
        dicomNodeTraverser.traverseTree(nodeInStorage, configurableClass, new OLockHashCalcFilter());

        // extract in '_olock' in existing node
        dicomNodeTraverser.traverseTree(nodeInStorage, configurableClass, new OLockExtractingFilter("_olock"));

        // extract in '_old_olock' in node being persisted
        dicomNodeTraverser.traverseTree(configNode, configurableClass, new OLockExtractingFilter("_old_olock"));

        // recalculate new olock hashes in the node being persisted
        dicomNodeTraverser.traverseTree(configNode, configurableClass, new OLockHashCalcFilter());

        // extract in '_new_olock' in node being persisted
        dicomNodeTraverser.traverseTree(configNode, configurableClass, new OLockExtractingFilter("_new_olock"));


        ////// merge the object /////
        merge(nodeInStorage, configNode);
        
        
        // filter the _olock clutter out
        dicomNodeTraverser.traverseTree(nodeInStorage, configurableClass, new OLockCleanupFilter("_new_olock", "_old_olock", "_olock"));
        
        super.persistNode(path,nodeInStorage,configurableClass);
        
        //TODO:release pessimistic lock

    }

    public void merge(Map<String, Object> oldNode, Map<String, Object> newNode) {

        // if this node is olock-enabled, apply olock logic
        if (newNode.containsKey("_new_olock"))

            // if olock has changed in new node, then either merge the new node in, 
            // or throw an exception if someone else has also changed it in the meantime in the oldnode 
            if (!newNode.get("_old_olock").equals(newNode.get("_new_olock"))) {
                if (newNode.get("_old_olock").equals(oldNode.get("_olock"))) {

                    // do merge, i.e. replace all subnodes of old with new
                    oldNode.clear();
                    for (Map.Entry<String, Object> objectEntry : newNode.entrySet())
                        oldNode.put(objectEntry.getKey(), objectEntry.getValue());

                    return;
                } else
                    throw new RuntimeException("Optimistic lock exception");
            }


        // if olock has not changed or its not there, traverse further to check nested locks
        for (Map.Entry<String, Object> objectEntry : oldNode.entrySet()) {

            Object oldValue = objectEntry.getValue();
            Object newValue = newNode.get(objectEntry.getKey());

            if (oldValue instanceof Collection) {

                Iterator newi = ((Collection) newValue).iterator();
                Iterator oldi = ((Collection) oldValue).iterator();
                while (oldi.hasNext()) {

                    Object o = oldi.next();
                    Object n = newi.next();

                    if (!(o instanceof Map)) break;

                    merge((Map) o, (Map) n);
                }
            }

            if (oldValue instanceof Map) {

                Iterator<Map.Entry> oldi = ((Map) oldValue).entrySet().iterator();
                Iterator<Map.Entry> newi = ((Map) newValue).entrySet().iterator();

                while (oldi.hasNext()) {

                    Object o = oldi.next().getValue();
                    Object n = newi.next().getValue();

                    if (!(o instanceof Map)) break;

                    merge((Map) o, (Map) n);
                }
                
            }

        }

    }


    @Override
    public Object getConfigurationNode(String path, Class configurableClass) throws ConfigurationException {

        // calculate olock hashes
        Object configurationNode = super.getConfigurationNode(path, configurableClass);
        DicomNodeTraverser dicomNodeTraverser = new DicomNodeTraverser(allExtensionClasses);
        dicomNodeTraverser.traverseTree(configurationNode, configurableClass, new OLockHashCalcFilter());

        return configurationNode;
    }
}
