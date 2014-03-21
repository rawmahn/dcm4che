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
package org.dcm4che3.conf.api.generic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.naming.NamingException;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigReader;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigWriter;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.CustomConfigTypeAdapter;
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
    public abstract static class PrimitiveAbstractTypeAdapter<T> implements CustomConfigTypeAdapter<T, T> {

        @Override
        public T deserialize(T serialized, ReflectiveConfig config, Field field) throws ConfigurationException {
            return serialized;
        }

        @Override
        public T serialize(T obj, ReflectiveConfig config, Field field) throws ConfigurationException {
            return obj;
        }

        @Override
        public void write(T serialized, ReflectiveConfig config, ConfigWriter writer, Field field)
                throws ConfigurationException {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);

            if (!fieldAnno.def().equals("N/A"))
                writer.storeNotDef(fieldAnno.name(), serialized, fieldAnno.def());
            else
                writer.storeNotNull(fieldAnno.name(), serialized);
        }

    }

    /**
     * Common Read/Write methods for String representation
     */
    public abstract static class CommonAbstractTypeAdapter<T> implements CustomConfigTypeAdapter<T, String> {

        @Override
        public String read(ReflectiveConfig config, ConfigReader reader, Field field) throws NamingException {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);
            return reader.asString(fieldAnno.name(), null);
        }

        @Override
        public void write(String serialized, ReflectiveConfig config, ConfigWriter writer, Field field) {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);
            writer.storeNotNull(fieldAnno.name(), serialized);
        }
    }

    /**
     * String
     */
    public static class StringTypeAdapter extends PrimitiveAbstractTypeAdapter<String> {

        @Override
        public String read(ReflectiveConfig config, ConfigReader reader, Field field) throws ConfigurationException,
                NamingException {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);
            return reader.asString(fieldAnno.name(), (fieldAnno.def().equals("N/A") ? null : fieldAnno.def()));
        }
    }

    /**
     * Integer
     */
    public static class IntegerTypeAdapter extends PrimitiveAbstractTypeAdapter<Integer> {
        @Override
        public Integer read(ReflectiveConfig config, ConfigReader reader, Field field) throws ConfigurationException,
                NamingException {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);
            return reader.asInt(fieldAnno.name(), (fieldAnno.def().equals("N/A") ? "0" : fieldAnno.def()));
        }
    }

    /**
     * Boolean
     */
    public static class BooleanTypeAdapter extends PrimitiveAbstractTypeAdapter<Boolean> {
        @Override
        public Boolean read(ReflectiveConfig config, ConfigReader reader, Field field) throws ConfigurationException,
                NamingException {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);
            return reader.asBoolean(fieldAnno.name(), (fieldAnno.def().equals("N/A") ? "false" : fieldAnno.def()));
        }
    }

    
    /**
     * Array
     */
    public static class ArrayTypeAdapter extends PrimitiveAbstractTypeAdapter<Object> {

        @Override
        public Object read(ReflectiveConfig config, ConfigReader reader, Field field) throws ConfigurationException,
                NamingException {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);

            if (String.class.isAssignableFrom(field.getType().getComponentType()))
                return reader.asStringArray(fieldAnno.name());
            else if (int.class.isAssignableFrom(field.getType().getComponentType()))
                return reader.asIntArray(fieldAnno.name());
            else
                return null;
        }

        @Override
        public void write(Object serialized, ReflectiveConfig config, ConfigWriter writer, Field field)
                throws ConfigurationException {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);
            writer.storeNotEmpty(fieldAnno.name(), serialized);
        }

    }

    /**
     * AttributesFormat
     */
    public static class AttributeFormatTypeAdapter extends CommonAbstractTypeAdapter<AttributesFormat> {

        @Override
        public AttributesFormat deserialize(String serialized, ReflectiveConfig config, Field field)
                throws ConfigurationException {
            return AttributesFormat.valueOf(serialized);
        }

        @Override
        public String serialize(AttributesFormat obj, ReflectiveConfig config, Field field) {
            return (obj == null ? null : obj.toString());
        }

    }

    /**
     * Device by name
     */
    public static class DeviceRepresentation extends CommonAbstractTypeAdapter<Device> {

        @Override
        public Device deserialize(String serialized, ReflectiveConfig config, Field field)
                throws ConfigurationException {
            return ((config == null || serialized == null) ? null : config.getDicomConfiguration().findDevice(
                    serialized));
        }

        @Override
        public String serialize(Device obj, ReflectiveConfig config, Field field) throws ConfigurationException {
            return (obj == null ? null : obj.getDeviceName());
        }

    }

    
    /**
     * Generic serialized representation of a config 'node' that has properties and children nodes
     * @author Roman K
     *
     */
    public static class ConfigNode {
        Map<String,Object> properties;
        Map<String,ConfigNode> children;
        
        @Override
        public String toString() {
            // TODO implement nice toString for diffs
            return super.toString();
        }
    }
    
    /**
     * Map
     * 
     * Serialized representation is Map<String, ConfigNode> since an entry corresponds to a child of an element, and the child
     * can have properties/children 
     */

    public static class MapRepresentation<K, V> implements CustomConfigTypeAdapter<Map<K, V>, Map<String, ConfigNode>> {

        
        
        
        @Override
        public Map<String, ConfigNode> read(ReflectiveConfig config, ConfigReader reader, Field field)
                throws ConfigurationException, NamingException {
                ConfigField fieldAnno = field.getAnnotation(ConfigField.class);

                ParameterizedType pt = (ParameterizedType) field.getGenericType();

                Type[] ptypes = pt.getActualTypeArguments();

                // there must be only 2 parameterized types
                if (ptypes.length != 2)
                    throw new MalformedParameterizedTypeException();

                // figure out the classes of declared generic parameters
                Class<K> keyClass = (Class<K>) ptypes[0]; 
                Class<V> valClass = (Class<V>) ptypes[1]; 
                
                /* not supported
                if (keyClass.getAnnotation(ConfigClass.class) != null) {
                }
                */
                
                CustomConfigTypeAdapter keyAdapter = config.lookupCustomTypeAdapter(keyClass);
                
                
                return null;
            }

        @Override
        public void write(Map<String, ConfigNode> serialized, ReflectiveConfig config, ConfigWriter writer, Field field)
                throws ConfigurationException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Map<String, ConfigNode> serialize(Map<K, V> obj, ReflectiveConfig config, Field field)
                throws ConfigurationException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<K, V> deserialize(Map<String, ConfigNode> serialized, ReflectiveConfig config, Field field)
                throws ConfigurationException {
            // TODO Auto-generated method stub
            return null;
        }



    }
     
    
    /*
    @Override
        public Map<String, String> read(ReflectiveConfig config, ConfigReader reader, Field field)
                throws ConfigurationException {
            ConfigField fieldAnno = field.getAnnotation(ConfigField.class);

            String[] entries = reader.asStringArray(fieldAnno.name());

            ParameterizedType pt = (ParameterizedType) field.getGenericType();

            Type[] ptypes = pt.getActualTypeArguments();

            // there must be only 2 parameterized types, and the key must be string
            if (ptypes.length != 2)
                throw new MalformedParameterizedTypeException();
            if (ptypes[0] != String.class)
                throw new MalformedParameterizedTypeException();

            Map<String, Object> res = new HashMap<String, Object>();

            // go through all entries
            for (String e : entries) {

                // split string with delimeter
                String[] keyVal = e.split("\\" + fieldAnno.delimeter(), 2);

                String eKey;
                Object eVal;

                if (keyVal.length == 1) {
                    eKey = fieldAnno.defaultKey();
                    eVal = keyVal[0];
                } else {
                    eKey = keyVal[0];
                    eVal = keyVal[1];
                }

                res.put(eKey, eVal);
            }

            return (Map<K, V>) res;

            return null;
        }

        @Override
        public Map<K, V> deserialize(Map<String, String> serialized, ReflectiveConfig config, Field field)
                throws ConfigurationException {

            ParameterizedType pt = (ParameterizedType) field.getGenericType();

            Type[] ptypes = pt.getActualTypeArguments();

            // check if there is custom representation for val
            CustomConfigTypeAdapter customRep = config.lookupCustomTypeAdapter((Class<?>) ptypes[1]);

            if (customRep != null) {
                eVal = customRep.read((String) eVal, configCtx, null, null);
            }
            return null;
        }

        /*
         *                     String[] serializedMap = mapField2StringArray((Map<String, Object>) value, field);
                    writer.storeNotEmpty(fieldAnno.name(), serializedMap);
    }*/

    public static Map<Class, CustomConfigTypeAdapter> defaultRepresentations;

    static {
        defaultRepresentations = new HashMap<Class, CustomConfigTypeAdapter>();

        defaultRepresentations.put(Array.class, new ArrayTypeAdapter());

        defaultRepresentations.put(String.class, new StringTypeAdapter());

        defaultRepresentations.put(int.class, new IntegerTypeAdapter());
        defaultRepresentations.put(Integer.class, new IntegerTypeAdapter());

        defaultRepresentations.put(Boolean.class, new BooleanTypeAdapter());
        defaultRepresentations.put(boolean.class, new BooleanTypeAdapter());

        defaultRepresentations.put(AttributesFormat.class, new AttributeFormatTypeAdapter());
        defaultRepresentations.put(Device.class, new DeviceRepresentation());

    }

    public static Map<Class, CustomConfigTypeAdapter> get() {
        return defaultRepresentations;
    }

}
