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

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class NodeTraverser {

    public interface EntryFilter {
        void onPrimitiveNode(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException;

        void onNodeBegin(Map<String, Object> node, Class property) throws ConfigurationException;

        void onNodeEnd(Map<String, Object> node, Class property) throws ConfigurationException;

        void onListBegin(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException;

        void onListElementBegin() throws ConfigurationException;

        void onListElementEnd() throws ConfigurationException;

        void onListEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException;

        void onMapBegin(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException;

        void onMapEntryBegin(String key) throws ConfigurationException;

        void onMapEntryEnd(String key) throws ConfigurationException;

        void onMapEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException;

        void onSubNodeBegin(AnnotatedConfigurableProperty property);

        void onSubNodeEnd(AnnotatedConfigurableProperty property);

        void applyRefNodeFilter(Object node, Class nodeClass);
    }

    ;

    public static class NoopFilter implements EntryFilter {
        @Override
        public void onPrimitiveNode(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {

        }

        @Override
        public void onNodeBegin(Map<String, Object> node, Class clazz) throws ConfigurationException {

        }

        @Override
        public void onNodeEnd(Map<String, Object> node, Class clazz) throws ConfigurationException {

        }

        @Override
        public void onListBegin(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {

        }

        @Override
        public void onListElementBegin() throws ConfigurationException {

        }

        @Override
        public void onListElementEnd() throws ConfigurationException {

        }

        @Override
        public void onListEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {

        }

        @Override
        public void onMapBegin(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {

        }

        @Override
        public void onMapEntryBegin(String key) throws ConfigurationException {

        }

        @Override
        public void onMapEntryEnd(String key) throws ConfigurationException {

        }

        @Override
        public void onMapEnd(Map<String, Object> containerNode, AnnotatedConfigurableProperty property) throws ConfigurationException {

        }

        @Override
        public void onSubNodeBegin(AnnotatedConfigurableProperty property) {

        }

        @Override
        public void onSubNodeEnd(AnnotatedConfigurableProperty property) {

        }

        @Override
        public void applyRefNodeFilter(Object node, Class nodeClass) {

        }
    }

    public void traverseTree(Object node, Class nodeClass, EntryFilter filter) throws ConfigurationException {

        // if because of any reason this is not a map (e.g. a reference or a custom adapter for a configurableclass),
        if (!(node instanceof Map)) {
            if (node != null)
                filter.applyRefNodeFilter(node, nodeClass);
            return;
        }


        Map<String, Object> containerNode = (Map<String, Object>) node;
        filter.onNodeBegin(containerNode, nodeClass);

        List<AnnotatedConfigurableProperty> properties = ConfigIterators.getAllConfigurableFieldsAndSetterParameters(nodeClass);
        for (AnnotatedConfigurableProperty property : properties) {
            Object childNode = containerNode.get(property.getAnnotatedName());


            // if the property is a configclass
            if (property.isConfObject()) {
                filter.onSubNodeBegin(property);
                traverseTree(childNode, property.getRawClass(), filter);
                filter.onSubNodeEnd(property);
                continue;
            }

            // collection, where a generics parameter is a configurable class or it is an array with comp type of configurableClass
            if (property.isCollectionOfConfObjects() || property.isArrayOfConfObjects()) {

                Collection collection = (Collection) childNode;

                filter.onListBegin(containerNode, property);
                if (collection != null)
                    for (Object object : collection) {
                        filter.onListElementBegin();
                        traverseTree(object, property.getPseudoPropertyForConfigClassCollectionElement().getRawClass(), filter);
                        filter.onListElementEnd();
                    }
                filter.onListEnd(containerNode, property);

                continue;
            }

            // map, where a value generics parameter is a configurable class
            if (property.isMapOfConfObjects()) {

                Map<String, Object> collection = (Map<String, Object>) childNode;

                filter.onMapBegin(containerNode, property);

                if (collection != null)
                    for (Map.Entry<String, Object> entry : collection.entrySet()) {
                        filter.onMapEntryBegin(entry.getKey());
                        traverseTree(entry.getValue(), property.getPseudoPropertyForConfigClassCollectionElement().getRawClass(), filter);
                        filter.onMapEntryEnd(entry.getKey());
                    }

                filter.onMapEnd(containerNode, property);

                continue;
            }

            // otherwise
            filter.onPrimitiveNode(containerNode, property);

        }

        filter.onNodeEnd(containerNode, nodeClass);
    }

}
