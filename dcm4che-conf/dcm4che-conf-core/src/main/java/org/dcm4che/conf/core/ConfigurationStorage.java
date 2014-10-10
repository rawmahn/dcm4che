package org.dcm4che.conf.core;

import org.dcm4che3.conf.api.ConfigurationException;

import java.util.List;
import java.util.Map;

/**
 * Might support multiple backends, etc.
 * Implementation should take care of instantiating the proper ConfigurationBackend instances (i.e. fetch some global system properties and use them to initialize configuration)
 */
public interface ConfigurationStorage {

    /**
     * Map of primitives and maps.
     * Should not be modified directly (only through persistNode/removeNode).
     * Should be thread-safe.
     * Clustering?
     * @return configuration tree
     */
    Map<String, Object> getConfigurationRoot() throws ConfigurationException;

    Object getConfigurationNode(String path) throws ConfigurationException;

    /**
     * Persists the configuration node to the specified path. The path specifies the container (must exist). The property is overwritten/created.
     * Any cached configuration is updated accordingly.
     * <pre>
     * persistNode("sc/root", "node1", {a:{v:1243},b:2}) results into
     * {sc:{
     *     root: {
     *         node1: {
     *              a:{
     *                  v:1243},
     *              b:2
     *         },
     *         other:123
     *     }
     *  }
     * }
     * </pre>
     * @param path path to the container where the property will be set
     * @param configNode new configuration to persist as a value of this property
     * @param configurableClass class annotated with ConfigurableClass, ConfigurableProperty and ConfigurableField annotations that corresponds to this node.
 *                          This parameter is required e.g., by LDAP backend to provide additional metadata like ObjectClasses and LDAP node hierarchy relations.
     */
    void persistNode(String path, Object configNode, Class configurableClass) throws ConfigurationException;

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
     * @param liteXPathExpression Must be absolute path, no double slashes, no attributes (only [attr=val] or [attr<>val])
     */
    java.util.Iterator search(String liteXPathExpression) throws IllegalArgumentException, ConfigurationException;
}
