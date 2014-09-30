package org.dcm4che.conf.core;

import java.util.Map;

/**
 * Created by aprvf on 29/09/2014.
 */
public abstract class Configuration {

    ConfigurationStorageBackend storageBackend;


    public class ConfigNode {

    }

    /**
     * Map of primitives and maps
     * @return
     */
    abstract Map<String, Object> getRoot();

    /**
     * Loads fully the config tree from the backend
     */
    abstract void load();

    /**
     * Updates the tree to the backend
     */
    abstract void persist();

    abstract void refresh();

    abstract void search();

    // load partialsubtree
    // unfold map
    // unfold



}
