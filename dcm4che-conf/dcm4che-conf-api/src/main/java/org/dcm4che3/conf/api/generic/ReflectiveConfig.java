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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.beanutils.PropertyUtils;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigReader;
import org.dcm4che3.util.AttributesFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic helper class that provides reflective traversing of classes annotated with Config annotations, and
 * storing/reading config from/to such classes
 * 
 * @author Roman K
 * 
 */
@SuppressWarnings("rawtypes")
public class ReflectiveConfig {

    public static final Logger log = LoggerFactory.getLogger(ReflectiveConfig.class);

    public interface CustomConfigTypeAdapter<T, ST> {

        /**
         * Should return string representation of <b>obj</b>.
         * 
         * @param obj
         * @param config
         *            Dicom Configuration object in whose context this writing is performed. <b>Can be <i>null</i>!</b>
         * @param writer
         *            ConfigWriter to use
         * @param field
         *            Config field. Can be used to read additional annotations, check type, etc.
         * @return
         */
        void write(ST serialized, ReflectiveConfig config, ConfigWriter writer, Field field)
                throws ConfigurationException;

        ST serialize(T obj, ReflectiveConfig config, Field field) throws ConfigurationException;

        /**
         * Should construct an object from its string representation.
         * 
         * @param str
         * @param config
         *            Dicom Configuration object in whose context this reading is performed. <b>Can be <i>null</i>!</b>
         * @param writer
         *            ConfigWriter to use
         * @param field
         *            Config field. Can be used to read additional annotations, check type, etc.
         * @return
         * @throws ConfigurationException
         * @throws NamingException
         */
        ST read(ReflectiveConfig config, ConfigReader reader, Field field) throws ConfigurationException,
                NamingException;

        T deserialize(ST serialized, ReflectiveConfig config, Field field) throws ConfigurationException;

    }

    /**
     * Generic serialized representation of a config 'node' that has properties and children nodes
     * @author Roman K
     *
     */
    public static class ConfigNode {
        /**
         * Object can be either serialized representation of a field or a ConfigNode
         */
        Map<String,Object> properties;
        
        @Override
        public String toString() {
            // TODO implement nice toString for diffs
            return super.toString();
        }
    }
    
    /**
     * Used by reflective config writer, should implement storage type-specific methods
     * 
     * @author Roman K
     */
    public interface ConfigWriter {
        void storeNotDef(String propName, Object value, String def);

        void storeNotEmpty(String propName, Object value);

        void storeNotNull(String propName, Object value);
    }

    /**
     * Used by reflective config reader, should implement storage type-specific methods
     * 
     * @author Roman K
     */
    public interface ConfigReader {

        String[] asStringArray(String propName) throws NamingException;

        int[] asIntArray(String propName) throws NamingException;

        int asInt(String propName, String def) throws NamingException;

        String asString(String propName, String def) throws NamingException;

        boolean asBoolean(String propName, String def) throws NamingException;

        Map<String, ConfigReader> readCollection(String propName, String keyName) throws NamingException;

        
    }

    /**
     * Used by reflective config diff writer, should implement storage type-specific methods
     * 
     * @author Roman K
     */
    public interface DiffWriter {
        void storeDiff(String propName, Object prev, Object curr);
    }

    /*
     * Static members
     */

    /**
     * Default singleton instance for simplified usage
     */
    private static final ReflectiveConfig singleton = new ReflectiveConfig(null, null);

    /**
     * Writes the configuration from the properties of the specified configuration object into the config storage using
     * the provided writer.
     * 
     * @param confObj
     *            Configuration object
     * @param writer
     *            Configuration writer
     */
    public static <T> void store(T confObj, ConfigWriter writer) {
        singleton.storeConfig(confObj, writer);
    }

    /**
     * Reads the configuration into the properties of the specified configuration object using the provided reader.
     * 
     * @param confObj
     * @param reader
     */
    public static <T> void read(T confObj, ConfigReader reader) {
        singleton.readConfig(confObj, reader);
    }

    /**
     * Calls store diff methods for all pairs of the annotated fields of prevConfObj and confObj
     * 
     * @param prevConfObj
     * @param confObj
     * @param ldapDiffWriter
     */
    public static <T> void storeAllDiffs(T prevConfObj, T confObj, DiffWriter ldapDiffWriter) {
        singleton.storeConfigDiffs(prevConfObj, confObj, ldapDiffWriter);
    }

    public static class ReflectiveAdapter<T> implements CustomConfigTypeAdapter<T, ConfigNode> {

        private Class clazz; 
        
        
        
        public ReflectiveAdapter(Class clazz) {
            super();
            this.clazz = clazz;
        }

        @Override
        public void write(ConfigNode serialized, ReflectiveConfig config, ConfigWriter writer, Field field)
                throws ConfigurationException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public ConfigNode serialize(T obj, ReflectiveConfig config, Field field) throws ConfigurationException {
            // TODO Auto-generated method stub
            return null;
        }

        
        /**
         * field is not actually used, the class must be set by the constrctor though
         */
        @Override
        public ConfigNode read(ReflectiveConfig config, ConfigReader reader, Field field)
                throws ConfigurationException, NamingException {
            
            ConfigNode cnode = new ConfigNode();
            for (Field classField : clazz.getDeclaredFields()) {

                // if field is not annotated, skip it
                ConfigField fieldAnno = (ConfigField) classField.getAnnotation(ConfigField.class);
                if (fieldAnno == null)
                    continue;

                    Object value = null;
                    Class<?> fieldType = classField.getType();

                    // Determine the class of the field anduse the corresponding method from the provided reader to get
                    // the value

                    // find typeadapter
                    CustomConfigTypeAdapter customRep;

                    // array is special case
                    if (fieldType.isArray())
                        customRep = config.lookupCustomTypeAdapter(Array.class);
                    else
                        customRep = config.lookupCustomTypeAdapter(fieldType);

                    if (customRep != null) {
                        value = customRep.read(config, reader, classField);
                    } else
                        throw new ConfigurationException("Corresponding 'reader' was not found for a field");

                    // put in a map
                    cnode.properties.put(fieldAnno.name(), value);
            }
            return cnode;

            }

        @Override
        public T deserialize(ConfigNode serialized, ReflectiveConfig config, Field field) throws ConfigurationException {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    
    /*
     * Non-static class members
     */

    private Map<Class, CustomConfigTypeAdapter> customRepresentations;
    private DicomConfiguration dicomConfiguration;

    /**
     * Creates an instance of ReflectiveConfig that will use the specified config context and custom representations
     * 
     * @param customRepresentations
     *            Null can be provided. class-representation map for types that should be treated in a special way when
     *            reading/writing the configuration.
     * @param configCtx
     *            Null can be provided. DicomCofiguration that will be forwarded to custom representation
     *            implementations as config context.
     */
    public ReflectiveConfig(Map<Class, CustomConfigTypeAdapter> customRepresentations, DicomConfiguration configCtx) {
        super();
        this.customRepresentations = customRepresentations;
        this.dicomConfiguration = configCtx;
    }

    /**
     * Reads the configuration into the properties of the specified configuration object using the provided reader.
     * 
     * @param confObj
     * @param reader
     */
    @SuppressWarnings("unchecked")
    public <T> void readConfig(T confObj, ConfigReader reader) {
        // look through all fields of the config obj, not including superclass
        // fields

        for (Field field : confObj.getClass().getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            try {

                Object value = null;
                Class<?> fieldType = field.getType();

                // Determine the class of the field anduse the corresponding method from the provided reader to get
                // the value

                // find typeadapter
                CustomConfigTypeAdapter customRep;

                // array is special case
                if (fieldType.isArray())
                    customRep = lookupCustomTypeAdapter(Array.class);
                else
                    customRep = lookupCustomTypeAdapter(fieldType);

                if (customRep != null) {
                    value = customRep.deserialize(customRep.read(this, reader, field), this, field);
                } else
                    throw new ConfigurationException("Corresponding 'reader' was not found for a field");

                // set the property value through its setter
                PropertyUtils.setSimpleProperty(confObj, field.getName(), value);

            } catch (IllegalAccessException e) {
                log.warn("Unable to set configuration field {} in a configuration object", fieldAnno.name());
                log.info("{}", e);
            } catch (InvocationTargetException e) {
                log.warn("Unable to set configuration field {} in a configuration object", fieldAnno.name());
                log.info("{}", e);
            } catch (NoSuchMethodException e) {
                log.warn("Unable to set configuration field {} in a configuration object", fieldAnno.name());
                log.info("{}", e);
            } catch (Exception e) {
                log.warn("Unable to read configuration property {}", fieldAnno.name());
                log.info("{}", e);
            }
        }
    }

    /**
     * Writes the configuration from the properties of the specified configuration object into the config storage using
     * the provided writer.
     * 
     * @param confObj
     * @param writer
     */
    @SuppressWarnings("unchecked")
    public <T> void storeConfig(T confObj, ConfigWriter writer) {

        // look through all fields of the config obj, not including
        // superclass fields
        for (Field field : confObj.getClass().getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            try {

                // read a configuration value using its getter
                Object value = PropertyUtils.getSimpleProperty(confObj, field.getName());

                Class<?> fieldType = field.getType();

                // find typeadapter
                CustomConfigTypeAdapter customRep;
                if (fieldType.isArray())
                    customRep = lookupCustomTypeAdapter(Array.class);
                else
                    customRep = lookupCustomTypeAdapter(fieldType);

                if (customRep != null) {
                    customRep.write(customRep.serialize(value, this, field), this, writer, field);
                } else {
                    throw new ConfigurationException("Corresponding 'writer' was not found for a field");
                }

            } catch (ConfigurationException e) {
                log.warn("Unable to serialize configuration field {}", fieldAnno.name());
                log.info("{}", e);
            } catch (IllegalAccessException e) {
                log.warn("Unable to read configuration field from a configuration object {}", fieldAnno.name());
                log.info("{}", e);
            } catch (InvocationTargetException e) {
                log.warn("Unable to read configuration field from a configuration object {}", fieldAnno.name());
                log.info("{}", e);
            } catch (NoSuchMethodException e) {
                log.warn("Unable to read configuration field from a configuration object {}", fieldAnno.name());
                log.info("{}", e);
            } catch (Exception e) {
                log.warn("Unable to store configuration field {}", fieldAnno.name());
                log.info("{}", e);
            }

        }
    }

    @SuppressWarnings("unchecked")
    public <T> void storeConfigDiffs(T prevConfObj, T confObj, DiffWriter ldapDiffWriter) {

        // look through all fields of the config class, not including
        // superclass fields
        for (Field field : confObj.getClass().getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            try {

                Object prev = PropertyUtils.getSimpleProperty(prevConfObj, field.getName());
                Object curr = PropertyUtils.getSimpleProperty(confObj, field.getName());

                // use serialized form for diffs  
                CustomConfigTypeAdapter customRep = lookupCustomTypeAdapter(field.getType());
                if (customRep != null) {
                    prev = customRep.serialize(prev, this, field);
                    curr = customRep.serialize(curr, this, field);
                }

                ldapDiffWriter.storeDiff(fieldAnno.name(), prev, curr);

            } catch (ConfigurationException e) {
                log.warn("Unable to serialize configuration field {}", fieldAnno.name());
                log.info("{}", e);
            } catch (IllegalAccessException e) {
                log.warn("Unable to read configuration field in a configuration object {}", fieldAnno.name());
                log.info("{}", e);
            } catch (InvocationTargetException e) {
                log.warn("Unable to read configuration field in a configuration object {}", fieldAnno.name());
                log.info("{}", e);
            } catch (NoSuchMethodException e) {
                log.warn("Unable to read configuration field in a configuration object {}", fieldAnno.name());
                log.info("{}", e);
            } catch (Exception e) {
                log.warn("Unable to store diff for a configuration field {}", fieldAnno.name());
                log.info("{}", e);
            }

        }
    }

    @SuppressWarnings("unchecked")
    public <T> CustomConfigTypeAdapter<T, ?> lookupCustomTypeAdapter(Class<T> clazz) {

        CustomConfigTypeAdapter<T, ?> customRep = null;

        // TODO: check if there is a ConfigClass annotation and use reflective 
        
        // try find in defaults
        Map<Class, CustomConfigTypeAdapter> def = DefaultConfigTypeAdapters.get();
        if (def != null)
            customRep = def.get(clazz);

        // try provided
        if (customRep == null && customRepresentations != null)
            customRep = customRepresentations.get(clazz);

        return customRep;

    }

    public DicomConfiguration getDicomConfiguration() {
        return dicomConfiguration;
    }

    public void setDicomConfiguration(DicomConfiguration dicomConfiguration) {
        this.dicomConfiguration = dicomConfiguration;
    }

    public void setCustomRepresentations(Map<Class, CustomConfigTypeAdapter> customRepresentations) {
        this.customRepresentations = customRepresentations;
    }

    /*
     * private String[] mapField2StringArray(Map<String, Object> map, Field field) throws ConfigurationException {
     * 
     * ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
     * 
     * ParameterizedType pt = (ParameterizedType) field.getGenericType();
     * 
     * Type[] ptypes = pt.getActualTypeArguments();
     * 
     * // there must be only 2 parameterized types, and the key must be string if (ptypes.length != 2) throw new
     * MalformedParameterizedTypeException(); if (ptypes[0] != String.class) throw new
     * MalformedParameterizedTypeException();
     * 
     * String[] res = new String[map.size()];
     * 
     * // go through all entries int i = 0; for (Entry<String, Object> e : map.entrySet()) {
     * 
     * // check if there is custom representation for val CustomConfigTypeAdapter customRep =
     * lookupCustomTypeAdapter((Class<?>) ptypes[1]);
     * 
     * String val; if (customRep != null) val = customRep.write(e.getValue(), dicomConfiguration, null, null); else val
     * = e.getValue().toString();
     * 
     * res[i++] = e.getKey() + fieldAnno.delimeter() + val; }
     * 
     * return res;
     * 
     * }
     * 
     * private Object stringArray2MapField(String[] src, Field field) throws ConfigurationException {
     * 
     * ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
     * 
     * ParameterizedType pt = (ParameterizedType) field.getGenericType();
     * 
     * Type[] ptypes = pt.getActualTypeArguments();
     * 
     * // there must be only 2 parameterized types, and the key must be string if (ptypes.length != 2) throw new
     * MalformedParameterizedTypeException(); if (ptypes[0] != String.class) throw new
     * MalformedParameterizedTypeException();
     * 
     * Map<String, Object> res = new HashMap<String, Object>();
     * 
     * // go through all entries for (String e : src) {
     * 
     * // split string with delimeter String[] keyVal = e.split("\\" + fieldAnno.delimeter(), 2);
     * 
     * String eKey; Object eVal;
     * 
     * if (keyVal.length == 1) { eKey = fieldAnno.defaultKey(); eVal = keyVal[0]; } else { eKey = keyVal[0]; eVal =
     * keyVal[1]; }
     * 
     * // check if there is custom representation for val CustomConfigTypeAdapter customRep =
     * lookupCustomTypeAdapter((Class<?>) ptypes[1]);
     * 
     * if (customRep != null) { eVal = customRep.read((String) eVal, dicomConfiguration, null, null); }
     * 
     * res.put(eKey, eVal); }
     * 
     * return res;
     * 
     * }
     */

    /**
     * Walk through the <b>from</b> object and for each field annotated with
     * 
     * @ConfigField, copy the value by using getter/setter to the <b>to</b> object.
     * 
     * @param from
     * @param to
     */
    public static <T> void reconfigure(T from, T to) {

        // look through all fields of the config class, not including
        // superclass fields
        for (Field field : from.getClass().getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            try {

                PropertyUtils.setSimpleProperty(to, field.getName(),
                        PropertyUtils.getSimpleProperty(from, field.getName()));

            } catch (Exception e) {
                log.error("Unable to reconfigure a device: {}", e);
            }

        }

    }

}
