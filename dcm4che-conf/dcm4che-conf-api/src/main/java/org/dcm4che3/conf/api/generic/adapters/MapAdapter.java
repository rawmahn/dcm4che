package org.dcm4che3.conf.api.generic.adapters;

import java.lang.reflect.Field;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigNode;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigReader;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigTypeAdapter;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigWriter;
import org.dcm4che3.net.TransferCapability;

/**
 * Map<br>
 * 
 * Only String keys are supported so far!
 * 
 * Ldap only will support ConfigClass'ed classes as values
 * 
 * Serialized representation is Map&lt;String, Object&gt;, Object can be ConfigNode
 */

public class MapAdapter<V> implements ConfigTypeAdapter<Map<String, V>, ConfigNode> {

    @Override
    public boolean isWritingChildren() {
        return true;
    }

    private ConfigTypeAdapter<V, ?> getValueAdapter(Field field, ReflectiveConfig config) {
        ParameterizedType pt = (ParameterizedType) field.getGenericType();

        Type[] ptypes = pt.getActualTypeArguments();

        // there must be only 2 parameterized types
        if (ptypes.length != 2)
            throw new MalformedParameterizedTypeException();

        // figure out the classes of declared generic parameters and
        // get value adapter
        return config.lookupTypeAdapter((Class<V>) ptypes[1]);

    }
    

    @SuppressWarnings("unchecked")
    @Override
    public ConfigNode read(ReflectiveConfig config, ConfigReader reader, Field field) throws ConfigurationException, NamingException {
        ConfigField fieldAnno = field.getAnnotation(ConfigField.class);

        ConfigNode cnode = new ConfigNode();

        // read collection
        ConfigReader collectionReader = reader.getChildReader(getCollectionName(fieldAnno));
        Map<String, ConfigReader> map = collectionReader.readCollection(fieldAnno.mapKey());

        // getValueAdapter
        ConfigTypeAdapter<V, Object> valueAdapter = (ConfigTypeAdapter<V, Object>) getValueAdapter(field, config);

        // for each element, read it using the value adapter
        for (Entry<String, ConfigReader> e : map.entrySet()) {
            cnode.attributes.put(e.getKey(), valueAdapter.read(config, e.getValue(), field));
        }
        return cnode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, V> deserialize(ConfigNode serialized, ReflectiveConfig config, Field field) throws ConfigurationException {

        // getValueAdapter
        ConfigTypeAdapter<V, Object> valueAdapter = (ConfigTypeAdapter<V, Object>) getValueAdapter(field, config);

        // deserialize entries
        Map<String, V> map = new HashMap<String, V>();
        for (Entry<String, Object> e : serialized.attributes.entrySet()) {
            map.put((String) e.getKey(), valueAdapter.deserialize(e.getValue(), config, field));
        }

        return map;
    }

    @Override
    public ConfigNode serialize(Map<String, V> obj, ReflectiveConfig config, Field field) throws ConfigurationException {

        ConfigNode cnode = new ConfigNode();

        // getValueAdapter
        ConfigTypeAdapter<V, Object> valueAdapter = (ConfigTypeAdapter<V, Object>) getValueAdapter(field, config);

        for (Entry<String, V> e : obj.entrySet()) {
            cnode.attributes.put((String) e.getKey(), valueAdapter.serialize(e.getValue(), config, field));
        }

        return cnode;
    }

    private String getCollectionName(ConfigField fieldAnno) {
        return (fieldAnno.alternativeCollectionName().equals("N/A") ? fieldAnno.name() : fieldAnno.alternativeCollectionName());
    }

    @Override
    public void write(ConfigNode serialized, ReflectiveConfig config, ConfigWriter writer, Field field) throws ConfigurationException {
        ConfigField fieldAnno = field.getAnnotation(ConfigField.class);

        // getValueAdapter
        ConfigTypeAdapter<V, Object> valueAdapter = (ConfigTypeAdapter<V, Object>) getValueAdapter(field, config);

        ConfigWriter collectionWriter = writer.createChild(getCollectionName(fieldAnno));

        for (Entry<String, Object> e : serialized.attributes.entrySet()) {

            ConfigWriter elementWriter = collectionWriter.getCollectionElementWriter(fieldAnno.mapKey(), e.getKey(), field);
            valueAdapter.write(e.getValue(), config, elementWriter, field);
        }
    }

    @Override
    public void merge(Map<String, V> prev, Map<String, V> curr, ReflectiveConfig config, ConfigWriter diffwriter, Field field) throws ConfigurationException {
        ConfigField fieldAnno = field.getAnnotation(ConfigField.class);

        // getValueAdapter
        ConfigTypeAdapter<V, Object> valueAdapter = (ConfigTypeAdapter<V, Object>) getValueAdapter(field, config);
        
        ConfigWriter collectionWriter = diffwriter.getChildDiffWriter(getCollectionName(fieldAnno));
        
        // remove nodes that were deleted since prev
        for (Entry<String,V> e : prev.entrySet())
            if (curr.get(e.getKey()) == null) 
                collectionWriter.removeCollectionElement(fieldAnno.mapKey(), e.getKey());
        
        // add new nodes and merge existing
        for (Entry<String,V> e : curr.entrySet()) {
            
            // if new node
            if (prev.get(e.getKey()) == null) {
                ConfigWriter elementWriter = collectionWriter.getCollectionElementWriter(fieldAnno.mapKey(), e.getKey(), field);
                // serialize
                Object serialized = valueAdapter.serialize(e.getValue(), config, field);
                valueAdapter.write(serialized, config, elementWriter, field);
            } 
            // existing node
            else {
                ConfigWriter elementWriter = collectionWriter.getCollectionElementDiffWriter(fieldAnno.mapKey(), e.getKey());
                valueAdapter.merge(prev.get(e.getKey()), e.getValue(), config, elementWriter, field);
            }
        }
    }
}