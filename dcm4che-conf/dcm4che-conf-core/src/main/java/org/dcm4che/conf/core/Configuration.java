package org.dcm4che.conf.core;

import org.dcm4che3.conf.api.ConfigurationException;

import java.util.List;
import java.util.Map;

/**
 * Denotes a configuration source. Can be used by BeanVitalizer that creates POJOs, or configuration administration app that provides UI to edit configuration.
 * <br/> <br/>
 * Configuration node is either a String or Map of objects where each object is a configuration node (single map can have values of multiple types, therefore Map&lt;String,Object&gt;).
 *
 * Clustering?
 */
public interface Configuration {

    /**
     * The returned node must not be modified directly (only through persistNode/removeNode).
     * Thread-safe.
     *
     * @return configuration tree
     * @throws ConfigurationException
     */
    Map<String, Object> getConfigurationRoot() throws ConfigurationException;

    Object getConfigurationNode(String path) throws ConfigurationException;

    /**
     * Returns the class that was used to persist the node using persistNode
     * @param path
     * @return
     * @throws ConfigurationException
     */
    Class getConfigurationNodeClass(String path) throws ConfigurationException, ClassNotFoundException;

    boolean nodeExists(String path) throws ConfigurationException;

    /**
     * Persists the configuration node to the specified path. The path specifies the container (must exist). The property is overwritten/created.
     * <br/>
     * <p><h2>Defaults:</h2>
     * The property values that are equal to default values are be filtered, i.e. not persisted.
     *
     * </p>
     *
     * @param path path to the container where the property will be set
     * @param configNode new configuration to persist as a value of this property
     * @param configurableClass class annotated with ConfigurableClass, ConfigurableProperty and ConfigurableField annotations that corresponds to this node.
 *                          This parameter is required e.g., by LDAP backend to provide additional metadata like ObjectClasses and LDAP node hierarchy relations.
     *                          configurableClass is persisted and can be retrieved by getConfigurationNodeClass
     *
     */
    void persistNode(String path, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException;

    /**
     * Invalidates any state, so next time the corresponding part of configuration is accessed with getConfigurationNode, it is be re-loaded from the backend.
     * It is imperative that some other nodes might be also refreshed during the operation.
     * @param path
     */
    void refreshNode(String path) throws ConfigurationException;

    /**
     * Removes a configuration node permanently
     * @param path
     */
    void removeNode(String path) throws ConfigurationException;

    /**
     * Returns configNodes
     * @param liteXPathExpression Must be absolute path, no double slashes, no @attributes (only [attr=val] or [attr<>val])
     */
    java.util.Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException;
}
