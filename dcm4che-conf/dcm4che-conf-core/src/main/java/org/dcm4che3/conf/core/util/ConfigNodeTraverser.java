/*
 * **** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *  Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 *  The Initial Developer of the Original Code is
 *  Agfa Healthcare.
 *  Portions created by the Initial Developer are Copyright (C) 2014
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *  See @authors listed below
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 2 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 *
 *  ***** END LICENSE BLOCK *****
 */
package org.dcm4che3.conf.core.util;

import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.conf.core.api.internal.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.api.internal.ConfigIterators;
import org.dcm4che3.conf.core.normalization.DefaultsAndNullFilterDecorator;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("unchecked")
public class ConfigNodeTraverser {

    public interface ConfigNodeTypesafeFilter {
        boolean beforeNode(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException;
    }

    public static class AConfigNodeFilter {
        public void beforeNodeElement(Map<String, Object> containerNode, String key, Object value) {
        }

        public void afterNodeElement(Map<String, Object> containerNode, String key, Object value) {
        }

        public void beforeNode(Map<String, Object> node) {
        }

        public void afterNode(Map<String, Object> node) {
        }

        public void beforeList(Collection list) {
        }

        public void beforeListElement(Collection list, Object element) {
        }

        public void afterListElement(Collection list, Object element) {
        }

        public void afterList(Collection list) {
        }

        /**
         * Fired for Boolean,String,Number,null
         */
        public void onPrimitiveNodeElement(Map<String, Object> containerNode, String key, Object value) {
        }

        /**
         * Fired for Boolean,String,Number,null
         */
        public void onPrimitiveListElement(Collection list, Object element) {
        }
    }

    public static class ADualNodeFilter {
        public void beforeNode(Map<String, Object> node1, Map<String, Object> node2) {

        }

        public void afterNode(Map<String, Object> node1, Map<String, Object> node2) {

        }

        public void afterNodeProperty(String key) {

        }

        public void beforeNodeProperty(String key) {

        }

        public void beforeCollectionElement(int index) {

        }

        public void afterCollectionElement(int index) {

        }
    }

    public static void traverseNodeTypesafe(Object node, Class nodeClass, ConfigNodeTypesafeFilter filter, List<Class> allExtensionClasses) throws ConfigurationException {

        // if because of any reason this is not a map (e.g. a reference or a custom adapter for a configurableclass),
        // we don't care about defaults
        if (!(node instanceof Map)) return;

        Map<String, Object> containerNode = (Map<String, Object>) node;

        List<AnnotatedConfigurableProperty> properties = ConfigIterators.getAllConfigurableFieldsAndSetterParameters(nodeClass);
        for (AnnotatedConfigurableProperty property : properties) {
            Object childNode = containerNode.get(property.getAnnotatedName());

            if (filter.beforeNode(containerNode, property)) continue;

            if (childNode == null) continue;

            // if the property is a configclass
            if (property.isConfObject()) {
                traverseNodeTypesafe(childNode, property.getRawClass(), filter, allExtensionClasses);
                continue;
            }

            // collection, where a generics parameter is a configurable class or it is an array with comp type of configurableClass
            if (property.isCollectionOfConfObjects() || property.isArrayOfConfObjects()) {

                Collection collection = (Collection) childNode;

                for (Object object : collection) {
                    traverseNodeTypesafe(object, property.getPseudoPropertyForConfigClassCollectionElement().getRawClass(), filter, allExtensionClasses);
                }

                continue;
            }

            // map, where a value generics parameter is a configurable class
            if (property.isMapOfConfObjects()) {

                try {
                    Map<String, Object> collection = (Map<String, Object>) childNode;

                    for (Object object : collection.values())
                        traverseNodeTypesafe(object, property.getPseudoPropertyForConfigClassCollectionElement().getRawClass(), filter, allExtensionClasses);

                } catch (ClassCastException e) {
                    DefaultsAndNullFilterDecorator.log.warn("Map is malformed", e);
                }

                continue;
            }

            // extensions map
            if (property.isExtensionsProperty()) {

                try {
                    Map<String, Object> extensionsMap = (Map<String, Object>) childNode;

                    for (Entry<String, Object> entry : extensionsMap.entrySet()) {
                        try {
                            traverseNodeTypesafe(entry.getValue(), Extensions.getExtensionClassBySimpleName(entry.getKey(), allExtensionClasses), filter, allExtensionClasses);
                        } catch (ClassNotFoundException e) {
                            // noop
                            DefaultsAndNullFilterDecorator.log.warn("Extension class {} not found", entry.getKey());
                        }
                    }

                } catch (ClassCastException e) {
                    DefaultsAndNullFilterDecorator.log.warn("Extensions are malformed", e);
                }

            }


        }
    }


    public static void traverseMapNode(Object node, AConfigNodeFilter filter) {

        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;

            filter.beforeNode(map);
            for (Entry<String, Object> stringObjectEntry : map.entrySet()) {

                String key = stringObjectEntry.getKey();
                Object value = stringObjectEntry.getValue();

                filter.beforeNodeElement(map, key, value);

                if (ConfigNodeUtil.isPrimitive(value))
                    filter.onPrimitiveNodeElement(map, key, value);
                else if (value instanceof Map) traverseMapNode(value, filter);
                else if (value instanceof Collection) {
                    Collection collection = (Collection) value;

                    filter.beforeList(collection);
                    for (Object o : collection) {
                        filter.beforeListElement(collection, o);

                        if (ConfigNodeUtil.isPrimitive(o))
                            filter.onPrimitiveListElement(collection, o);
                        else if (o instanceof Map) traverseMapNode(o, filter);
                        else throw new IllegalArgumentException("List of lists is not allowed");

                        filter.afterListElement(collection, o);
                    }
                    filter.afterList(collection);
                }
                filter.afterNodeElement(map, key, value);
            }
            filter.afterNode(map);

        } else
            throw new IllegalArgumentException("A composite config node must be a Map<String,Object>");
    }


    /**
     * Traverses with in-depth search and applies dual filter.
     *
     * @param node1
     * @param node2
     */
    public static void dualTraverseMapNodes(Map<String, Object> node1, Map<String, Object> node2, ADualNodeFilter filter) {

        filter.beforeNode(node1, node2);

        if (node1 != null && node2 != null)
            for (Map.Entry<String, Object> objectEntry : node1.entrySet())
                if (node2.containsKey(objectEntry.getKey())) {

                    filter.beforeNodeProperty(objectEntry.getKey());

                    Object node1El = objectEntry.getValue();
                    Object node2El = node2.get(objectEntry.getKey());
                    dualTraverseProperty(node1El, node2El, filter);

                    filter.afterNodeProperty(objectEntry.getKey());
                }

        filter.afterNode(node1, node2);
    }

    private static void dualTraverseProperty(Object node1El, Object node2El, ADualNodeFilter filter) {
        if (node2El == null && node1El == null) return;

        if (node1El instanceof Collection) {

            if (!(node2El instanceof Collection)) return;

            Iterator node1i = ((Collection) node1El).iterator();
            Iterator node2i = ((Collection) node2El).iterator();

            int i = 0;

            while (node1i.hasNext() && node2i.hasNext()) {

                filter.beforeCollectionElement(i);

                try {
                    Object i1 = node1i.next();
                    Object i2 = node2i.next();

                    if (!(i1 instanceof Map) || !(i2 instanceof Map)) break;

                    dualTraverseMapNodes((Map) i1, (Map) i2, filter);
                } finally {
                    filter.afterCollectionElement(i);
                }

                i++;
            }
        }

        if (node1El instanceof Map) {

            if (!(node2El instanceof Map)) return;

            dualTraverseMapNodes((Map) node1El, (Map) node2El, filter);
        }

    }


}
