package org.dcm4che3.conf.core.storage;

import org.dcm4che3.conf.core.DelegatingConfiguration;
import org.dcm4che3.conf.core.Nodes;
import org.dcm4che3.conf.core.api.Configuration;
import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.conf.core.api.Path;
import org.dcm4che3.conf.core.util.ConfigNodeTraverser;
import org.dcm4che3.conf.core.util.PathPattern;
import org.dcm4che3.conf.core.util.PathTrackingConfigNodeFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps an index of objects referable by uuids to allow fast lookup by uuid
 *
 * @author Roman K <roman.khazankin@gmail.com>
 */
public class ReferenceIndexingDecorator extends DelegatingConfiguration {

    static PathPattern referencePattern = new PathPattern(Configuration.REFERENCE_BY_UUID_PATTERN);

    protected HashMap<String, Path> uuidToReferableIndex;

    public ReferenceIndexingDecorator(Configuration delegate, HashMap<String, Path> uuidToSimplePathCache) {
        super(delegate);
        uuidToReferableIndex = uuidToSimplePathCache;
    }

    private void removeOldReferablesFromIndex(Object oldConfigurationNode) {
        if (oldConfigurationNode instanceof Map)
            ConfigNodeTraverser.traverseMapNode(oldConfigurationNode, new ConfigNodeTraverser.AConfigNodeFilter() {
                @Override
                public void onPrimitiveNodeElement(Map<String, Object> containerNode, String key, Object value) {
                    if (Configuration.UUID_KEY.equals(key)) uuidToReferableIndex.remove(value);
                }
            });
    }

    private void addReferablesToIndex(List<String> pathItems, Object configNode) {
        if (configNode instanceof Map)
            ConfigNodeTraverser.traverseMapNode(configNode, new PathTrackingConfigNodeFilter(pathItems) {
                @Override
                public void onPrimitiveNodeElement(Map<String, Object> containerNode, String key, Object value) {
                    if (Configuration.UUID_KEY.equals(key)) {
                        String last = path.pop();
                        uuidToReferableIndex.put((String) value, new Path(path.descendingIterator()));
                        path.push(last);
                    }
                }
            });
    }

    @Override
    public Object getConfigurationNode(String path, Class configurableClass) throws ConfigurationException {
        PathPattern.PathParser pathParser = referencePattern.parseIfMatches(path);
        if (pathParser != null) {
            System.out.println("hit! uuid "+path);

            return super.getConfigurationNode(uuidToReferableIndex.get(pathParser.getParam("uuid")).toSimpleEscapedXPath(), configurableClass);
        }

        return super.getConfigurationNode(path, configurableClass);
    }

    @Override
    public void persistNode(String path, final Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {

        // remove the overwritten referables from index
        removeOldReferablesFromIndex(super.getConfigurationNode(path, null));

        // add newcomer referables to index
        addReferablesToIndex(Nodes.simpleOrPersistablePathToPathItemsOrNull(path), configNode);

        super.persistNode(path, configNode, configurableClass);
    }

    @Override
    public void refreshNode(String path) throws ConfigurationException {
        removeOldReferablesFromIndex(super.getConfigurationNode(path, null));
        super.refreshNode(path);
        addReferablesToIndex(Nodes.simpleOrPersistablePathToPathItemsOrNull(path), super.getConfigurationNode(path, null));
    }

    @Override
    public void removeNode(String path) throws ConfigurationException {
        removeOldReferablesFromIndex(super.getConfigurationNode(path, null));
        super.removeNode(path);
    }

    @Override
    public boolean nodeExists(String path) throws ConfigurationException {
        PathPattern.PathParser pathParser = referencePattern.parseIfMatches(path);
        if (pathParser != null) {
            return uuidToReferableIndex.containsKey(pathParser.getParam("uuid"));
        }

        return super.nodeExists(path);
    }
}
