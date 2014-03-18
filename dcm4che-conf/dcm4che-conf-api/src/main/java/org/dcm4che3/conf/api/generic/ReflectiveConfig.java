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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.commons.beanutils.PropertyUtils;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.util.AttributesFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic helper class that provides reflective traversing of classes annotated
 * with Config annotations, and storing/reading config from/to such classes
 * 
 * @author Roman K
 * 
 */
@SuppressWarnings("rawtypes")
public class ReflectiveConfig {

    public static final Logger log = LoggerFactory.getLogger(ReflectiveConfig.class);

    public interface CustomConfigObjectRepresentation<T> {

        /**
         * Should return string representation of <b>obj</b>.
         * 
         * @param obj
         * @param config
         *            Dicom Configuration object in whose context this writing
         *            is performed. <b>Can be <i>null</i>!</b>
         * @return
         */
        String serialize(T obj, DicomConfiguration config);

        /**
         * Should construct an object from its string representation.
         * 
         * @param str
         * @param config
         *            Dicom Configuration object in whose context this reading
         *            is performed. <b>Can be <i>null</i>!</b>
         * @return
         */
        T unserialize(String str, DicomConfiguration config);

    }

    /**
     * Used by reflective config writer, should implement storage type-specific
     * methods
     * 
     * @author Roman K
     */
    public interface ConfigWriter {
        void storeNotDef(String propName, Object value, String def);

        void storeNotEmpty(String propName, Object value);

        void storeNotNull(String propName, Object value);
    }

    /**
     * Used by reflective config reader, should implement storage type-specific
     * methods
     * 
     * @author Roman K
     */
    public interface ConfigReader {

        String[] asStringArray(String propName) throws NamingException;

        int[] asIntArray(String propName) throws NamingException;

        int asInt(String propName, String def) throws NamingException;

        String asString(String propName, String def) throws NamingException;

        boolean asBoolean(String propName, String def) throws NamingException;

    }

    /**
     * Used by reflective config diff writer, should implement storage
     * type-specific methods
     * 
     * @author Roman K
     */
    public interface DiffWriter {
        void storeDiff(String propName, Object prev, Object curr);
    }

    /**
     * Writes the configuration from the properties of the specified
     * configuration object into the config storage using the provided writer.
     * 
     * @param confObj
     *            Configuration object
     * @param writer
     *            Configuration writer
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static <T> void store(T confObj, ConfigWriter writer) throws IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        store(confObj, writer, null, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> CustomConfigObjectRepresentation<T> lookupCustomRepresentation(
            Map<Class, CustomConfigObjectRepresentation> customRepresentations, Class<T> clazz) {

        CustomConfigObjectRepresentation<T> customRep = null;        
        
        // try find in defaults
        Map<Class, CustomConfigObjectRepresentation> def = DefaultCustomRepresentations.get();
        if (def != null)
            customRep = def.get(clazz);

        // try provided
        if (customRep == null && customRepresentations != null)
            customRep = customRepresentations.get(clazz);

        return customRep;

    }

    /**
     * Writes the configuration from the properties of the specified
     * configuration object into the config storage using the provided writer.
     * 
     * @param confObj
     * @param writer
     * @param customRepresentations
     *            Null can be provided. class-representation map for types that
     *            should be treated in a special way when reading/writing the
     *            configuration.
     * @param configCtx
     *            Null can be provided. DicomCofiguration that will be forwarded
     *            to custom representation impls as config context.
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    @SuppressWarnings("unchecked")
    public static <T> void store(T confObj, ConfigWriter writer,
            Map<Class, CustomConfigObjectRepresentation> customRepresentations, DicomConfiguration configCtx)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // look through all fields of the config obj, not including
        // superclass fields
        for (Field field : confObj.getClass().getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            // read a configuration value using its getter
            Object value = PropertyUtils.getSimpleProperty(confObj, field.getName());

            Class<?> fieldType = field.getType();

            // if custom representation exists, call toString and then store it
            // using storeNotNull
            CustomConfigObjectRepresentation customRep = lookupCustomRepresentation(customRepresentations, fieldType);

            if (customRep != null) {
                writer.storeNotNull(fieldAnno.name(), customRep.serialize(value, configCtx));
                continue;
            }

            // otherwise call appropriate store method based of field type and
            // default value specified

            if (!fieldAnno.def().equals("N/A"))
                writer.storeNotDef(fieldAnno.name(), value, fieldAnno.def());
            else if (fieldType.isArray())
                writer.storeNotEmpty(fieldAnno.name(), value);
            else
                writer.storeNotNull(fieldAnno.name(), value);
        }
    }

    /**
     * Reads the configuration into the properties of the specified
     * configuration object using the provided reader.
     * 
     * @param confObj
     * @param reader
     * @throws NamingException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static <T> void read(T confObj, ConfigReader reader) throws NamingException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        read(confObj, reader, null, null);
    }

    /**
     * Reads the configuration into the properties of the specified
     * configuration object using the provided reader.
     * 
     * @param confObj
     * @param reader
     * @param customRepresentations
     *            Null can be provided. class-representation map for types that
     *            should be treated in a special way when reading/writing the
     *            configuration.
     * @param configCtx
     *            Null can be provided. DicomCofiguration that will be forwarded
     *            to custom representation impls as config context.
     * @throws NamingException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static <T> void read(T confObj, ConfigReader reader,
    Map<Class, CustomConfigObjectRepresentation> customRepresentations, DicomConfiguration configCtx)
            throws NamingException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // look through all fields of the config obj, not including superclass
        // fields

        for (Field field : confObj.getClass().getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            Object value = null;
            Class<?> fieldType = field.getType();

            // Determine the class of the field and
            // use the corresponding method from the provided reader to get the
            // value

            if (fieldType.isArray()) {
                if (String.class.isAssignableFrom(fieldType.getComponentType())) {
                    value = reader.asStringArray(fieldAnno.name());

                } else if (int.class.isAssignableFrom(fieldType.getComponentType())) {
                    value = reader.asIntArray(fieldAnno.name());
                }
            } else if (String.class.isAssignableFrom(fieldType)) {
                value = reader.asString(fieldAnno.name(), (fieldAnno.def().equals("N/A") ? null : fieldAnno.def()));

            } else if (boolean.class.isAssignableFrom(fieldType)) {
                value = reader.asBoolean(fieldAnno.name(), (fieldAnno.def().equals("N/A")? "false" : fieldAnno.def()));

            } else if (int.class.isAssignableFrom(fieldType)) {
                value = reader.asInt(fieldAnno.name(), (fieldAnno.def().equals("N/A")? "0" : fieldAnno.def()));

            } else {

                // if custom representation exists, read as string and use
                // fromString
                CustomConfigObjectRepresentation customRep = lookupCustomRepresentation(customRepresentations,
                        fieldType);

                if (customRep != null) {

                    String str = reader.asString(fieldAnno.name(), null);
                    value = customRep.unserialize(str, configCtx);

                } else
                    log.warn("Unable to read configuration field {}", fieldAnno.name());

            }

            // set the property value through its setter
            PropertyUtils.setSimpleProperty(confObj, field.getName(), value);

        }
    }

    public static <T> void storeAllDiffs(T prevConfObj, T confObj, DiffWriter ldapDiffWriter ) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        storeAllDiffs(prevConfObj, confObj, ldapDiffWriter, null, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> void storeAllDiffs(T prevConfObj, T confObj, DiffWriter ldapDiffWriter, Map<Class, CustomConfigObjectRepresentation> customRepresentations, DicomConfiguration configCtx)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // look through all fields of the config class, not including
        // superclass fields
        for (Field field : confObj.getClass().getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) field.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            Object prev = PropertyUtils.getSimpleProperty(prevConfObj, field.getName());
            Object curr = PropertyUtils.getSimpleProperty(confObj, field.getName());
            
            // if there is a custom representation, use serialized form for diffs
            CustomConfigObjectRepresentation customRep = lookupCustomRepresentation(customRepresentations, field.getType());
            if (customRep != null) {
                prev = customRep.serialize(prev,configCtx);
                curr = customRep.serialize(curr,configCtx);
            }

            ldapDiffWriter.storeDiff(fieldAnno.name(), prev, curr);

        }
    }

    /**
     * Walk through the <b>from</b> object and for each field annotated with
     * 
     * @ConfigField, copy the value by using getter/setter to the <b>to</b>
     *               object.
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
