/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2014
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4che.conf.core.adapters;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigReader;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigWriter;
import org.dcm4che3.data.Code;
import org.dcm4che3.net.Device;
import org.dcm4che3.util.AttributesFormat;

/**
 * 
 * @author Roman K
 * 
 */
public class DefaultConfigTypeAdapters {

    /**
     * Common Read/Write methods for primitives that have same serialized and deserialized representation, and same
     * write method
     */
    public static class PrimitiveTypeAdapter<T> implements ConfigTypeAdapter<T, T> {

        Map<String, Object> metadata =  new HashMap<String, Object>();

        /**
         * Assign the type for metadata
         * @param type
         */
        protected PrimitiveTypeAdapter(String type) {
            metadata.put("type", type);
        }

        public PrimitiveTypeAdapter() {
        }

        @Override
        public T fromConfigNode(T configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
            return configNode;
        }

        @Override
        public T toConfigNode(T object) throws ConfigurationUnserializableException {
            return object;
        }

        /**
         * Constant metadata
         */
        @Override
        public Map<String, Object> getMetadata(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
            return metadata;
        }
    }

    /**
     * Common Read/Write methods for String representation
     */
    public abstract static class CommonAbstractTypeAdapter<T> implements ConfigTypeAdapter<T, String> {
        Map<String, Object> metadata =  new HashMap<String, Object>();

        protected CommonAbstractTypeAdapter(String type) {
            metadata.put("type", type);
        }

        @Override
        public Map<String, Object> getMetadata(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
            return metadata;
        }
    }
    /**
     * Array
     */
    public static class ArrayTypeAdapter implements ConfigTypeAdapter<Object, Object> {

        @Override
        public Object fromConfigNode(Object configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

            // if it is a collection, create an array with proper component type
            if (Collection.class.isAssignableFrom(configNode.getClass())) {
                Collection l = ((Collection) configNode);
                Class<?> componentType = property.getType().getClass().getComponentType();
                Object arr = Array.newInstance(componentType, l.size());
                int i=0;
                for (Object el : l) {
                    // deserialize element
                    ConfigTypeAdapter elementTypeAdapter = vitalizer.lookupTypeAdapter(componentType);
                    el = elementTypeAdapter.fromConfigNode(el,new AnnotatedConfigurableProperty(componentType), vitalizer);

                    // push to array
                    Array.set(arr, i++, el);
                }
                return arr;
            } else throw new IllegalArgumentException("Object of unexpected type ("+configNode.getClass().getName()+") supplied for conversion into an array. Must be a collection.");
        }

        @Override
        public Object toConfigNode(Object object) throws ConfigurationUnserializableException {
            return Arrays.asList(object);
        }

        @Override
        public Map<String, Object> getMetadata(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

            Map<String, Object> metadata =  new HashMap<String, Object>();
            Map<String, Object> elementMetadata =  new HashMap<String, Object>();

            metadata.put("type", "Array");
            metadata.put("elementMetadata", elementMetadata);

            if (String.class.isAssignableFrom(property.getType().getClass().getComponentType()))
                elementMetadata.put("type", "String");
            else if (int.class.isAssignableFrom(property.getType().getClass().getComponentType()))
                elementMetadata.put("type", "Integer");

            return metadata;
        }

    }

    /**
     * AttributesFormat
     */
    public static class AttributeFormatTypeAdapter extends CommonAbstractTypeAdapter<AttributesFormat> {

        public AttributeFormatTypeAdapter() {
            super("AttributesFormat");
        }

        @Override
        public AttributesFormat fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
            return AttributesFormat.valueOf(configNode);
        }

        @Override
        public String toConfigNode(AttributesFormat object) throws ConfigurationUnserializableException {
            return (obj == null ? null : obj.toString());
        }
    }

    /**
     * Device by name
     */
    public static class DeviceTypeAdapter extends CommonAbstractTypeAdapter<Device> {

        public DeviceTypeAdapter() {
            super("Device");
        }

        @Override
        public Device fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
            vitalizer.getContext(DicomConfiguration.class).findDevice(configNode);

            return ((config == null || serialized == null) ? null : config.getDicomConfiguration().findDevice(serialized));
        }

        @Override
        public String toConfigNode(Device object) throws ConfigurationUnserializableException {
            return null;
        }

        @Override
        public Device deserialize(String serialized, ReflectiveConfig config, Field field) throws ConfigurationException {
            return ((config == null || serialized == null) ? null : config.getDicomConfiguration().findDevice(serialized));
        }

        @Override
        public String serialize(Device obj, ReflectiveConfig config, Field field) throws ConfigurationException {
            return (obj == null ? null : obj.getDeviceName());
        }

    }

    /**
     * AttributesFormat
     */
    public static class EnumTypeAdapter extends CommonAbstractTypeAdapter<Enum<?>> {

        public EnumTypeAdapter() {
            metadata.put("type", "Enum");
        }

        @Override
        public Enum<?> deserialize(String serialized, ReflectiveConfig config, Field field) throws ConfigurationException {
            if (serialized == null) 
                return null;
            try {
                Method method = field.getType().getMethod("valueOf", String.class);
                return (Enum<?>) method.invoke(null, serialized);
            } catch (Exception x) {
                throw new ConfigurationException("Deserialization of Enum failed! field:"+field, x);
            }
        }

        @Override
        public String serialize(Enum<?> obj, ReflectiveConfig config, Field field) {
            return (obj == null ? null : obj.name());
        }

    }

    public static Map<Class, ConfigTypeAdapter> defaultTypeAdapters;

    static {
        defaultTypeAdapters = new HashMap<Class, ConfigTypeAdapter>();

        defaultTypeAdapters.put(String.class, new PrimitiveTypeAdapter("String"));

        PrimitiveTypeAdapter integerAdapter = new PrimitiveTypeAdapter("Integer");
        defaultTypeAdapters.put(int.class, integerAdapter);
        defaultTypeAdapters.put(Integer.class, integerAdapter);

        PrimitiveTypeAdapter booleanAdapter = new PrimitiveTypeAdapter("Boolean");
        defaultTypeAdapters.put(Boolean.class, booleanAdapter);
        defaultTypeAdapters.put(boolean.class, booleanAdapter);

        PrimitiveTypeAdapter doubleAdapter = new PrimitiveTypeAdapter("Double");
        defaultTypeAdapters.put(double.class, doubleAdapter);
        defaultTypeAdapters.put(float.class, doubleAdapter);
        defaultTypeAdapters.put(Double.class, doubleAdapter);
        defaultTypeAdapters.put(Float.class, doubleAdapter);

        defaultTypeAdapters.put(Map.class, new MapTypeAdapter());
        defaultTypeAdapters.put(Set.class, new SetTypeAdapter());

        defaultTypeAdapters.put(Enum.class, new EnumTypeAdapter());

        defaultTypeAdapters.put(AttributesFormat.class, new AttributeFormatTypeAdapter());
        defaultTypeAdapters.put(Device.class, new DeviceTypeAdapter());
    }

    public static ConfigTypeAdapter get(Class clazz) {
        return defaultTypeAdapters.get(clazz);
    }

}
