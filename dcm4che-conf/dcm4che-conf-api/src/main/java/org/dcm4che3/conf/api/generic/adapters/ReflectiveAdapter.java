package org.dcm4che3.conf.api.generic.adapters;

import java.lang.reflect.Field;

import javax.naming.NamingException;

import org.apache.commons.beanutils.PropertyUtils;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigNode;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigReader;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigTypeAdapter;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigWriter;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.DiffWriter;

/**
 * Reflective adapter that handles classes with ConfigClass annotations.<br/>
 * <br/>
 * 
 * <b>field</b> argument is not actually used in the methods, the class must
 * be set in the constructor.
 * 
 * User has to use 2 arg constructor and initialize providedConfObj when the
 * already created conf object should be used instead of instantiating one in deserialize method,
 * as, e.g. in ReflectiveConfig.readConfig
 * 
 */
public class ReflectiveAdapter<T> implements ConfigTypeAdapter<T, ConfigNode> {

    private Class<T> clazz;

    /**
     * Initialized only when doing first level parsing, e.g. in
     * ReflectiveConfig.readConfig
     */
    private T providedConfObj;

    public ReflectiveAdapter(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    public ReflectiveAdapter(Class<T> clazz, T providedConfObj) {
        super();
        this.clazz = clazz;
        this.providedConfObj = providedConfObj;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void write(ConfigNode serialized, ReflectiveConfig config, ConfigWriter writer, Field field) throws ConfigurationException {

        for (Field classField : clazz.getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) classField.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            // find typeadapter
            ConfigTypeAdapter customRep = config.lookupTypeAdapter(classField.getType());

            if (customRep != null) {
                customRep.write(serialized.attributes.get(fieldAnno.name()), config, writer, classField);
            } else {
                throw new ConfigurationException("Corresponding 'writer' was not found for field" + fieldAnno.name());
            }
        }
        
        // do actual store
        writer.flush();

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ConfigNode serialize(T obj, ReflectiveConfig config, Field field) throws ConfigurationException {

        ConfigNode cnode = new ConfigNode();
        for (Field classField : clazz.getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) classField.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            // read a configuration value using its getter
            Object value;

            try {
                value = PropertyUtils.getSimpleProperty(obj, classField.getName());
            } catch (Exception e) {
                throw new ConfigurationException("Error while writing configuration field " + fieldAnno.name(), e);
            }

            // find typeadapter
            ConfigTypeAdapter customRep = config.lookupTypeAdapter(classField.getType());

            if (customRep != null) {
                Object serialized = customRep.serialize(value, config, classField);
                cnode.attributes.put(fieldAnno.name(), serialized);
            } else {
                throw new ConfigurationException("Corresponding 'writer' was not found for field" + fieldAnno.name());
            }

        }
        return cnode;

    }

    @SuppressWarnings("rawtypes")
    @Override
    public ConfigNode read(ReflectiveConfig config, ConfigReader reader, Field field) throws ConfigurationException, NamingException {

        ConfigNode cnode = new ConfigNode();
        for (Field classField : clazz.getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) classField.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            // find typeadapter
            ConfigTypeAdapter customRep = config.lookupTypeAdapter(classField.getType());

            if (customRep != null) {

                Object value = customRep.read(config, reader, classField);
                cnode.attributes.put(fieldAnno.name(), value);

            } else
                throw new ConfigurationException("Corresponding 'reader' was not found for field " + fieldAnno.name());

        }

        return cnode;

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public T deserialize(ConfigNode serialized, ReflectiveConfig config, Field field) throws ConfigurationException {

        T confObj;

        // create instance or use provided when it was created earlier,
        // e.g., in other config extensions
        if (providedConfObj == null) {
            try {
                confObj = (T) clazz.newInstance();
            } catch (Exception e) {
                throw new ConfigurationException("Error while instantiating config class " + clazz.getSimpleName(), e);
            }
        } else
            confObj = providedConfObj;

        for (Field classField : clazz.getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) classField.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            // find typeadapter
            ConfigTypeAdapter customRep = config.lookupTypeAdapter(classField.getType());

            if (customRep != null) {
                try {
                    Object value = customRep.deserialize(serialized.attributes.get(fieldAnno.name()), config, classField);

                    // set using a setter
                    PropertyUtils.setSimpleProperty(confObj, classField.getName(), value);
                } catch (Exception e) {
                    throw new ConfigurationException("Error while reading configuration field " + fieldAnno.name(), e);
                }

            } else
                throw new ConfigurationException("Corresponding 'reader' was not found for field " + fieldAnno.name());

        }

        return confObj;

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void merge(T prev, T curr, ReflectiveConfig config, DiffWriter diffwriter, Field field) throws ConfigurationException {
        // look through all fields of the config class, not including
        // superclass fields
        for (Field classField : clazz.getDeclaredFields()) {

            // if field is not annotated, skip it
            ConfigField fieldAnno = (ConfigField) classField.getAnnotation(ConfigField.class);
            if (fieldAnno == null)
                continue;

            try {

                Object prevProp = PropertyUtils.getSimpleProperty(prev, classField.getName());
                Object currProp = PropertyUtils.getSimpleProperty(curr, classField.getName());

                // find adapter
                ConfigTypeAdapter customRep = config.lookupTypeAdapter(classField.getType());

                customRep.merge(prevProp, currProp, config, diffwriter, classField);

            } catch (Exception e) {
                throw new ConfigurationException("Cannot store diff for field " + fieldAnno.name());
            }

        }
        
        // do actual merge
        diffwriter.flushDiffs();

    }

}