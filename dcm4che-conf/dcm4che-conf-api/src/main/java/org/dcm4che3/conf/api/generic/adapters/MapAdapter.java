package org.dcm4che3.conf.api.generic.adapters;

import java.lang.reflect.Field;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigNode;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigReader;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigTypeAdapter;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigWriter;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.DiffWriter;

/**
 * Map<br>
 * 
 * Only String keys are supported so far!
 * 
 * Serialized representation is Map&lt;String, Object&gt;, Object can be
 * ConfigNode
 */

public class MapAdapter<K, V> implements ConfigTypeAdapter<Map<K, V>, ConfigNode> {

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
        String collectionName = (fieldAnno.alternativeCollectionName().equals("N/A") ? fieldAnno.name() : fieldAnno.alternativeCollectionName());
        ConfigReader collectionReader = reader.getChild(collectionName);
        Map<String, ConfigReader> map = collectionReader.readCollection(collectionName, fieldAnno.mapKey());

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
    public Map<K, V> deserialize(ConfigNode serialized, ReflectiveConfig config, Field field) throws ConfigurationException {

        // getValueAdapter
        ConfigTypeAdapter<V, Object> valueAdapter = (ConfigTypeAdapter<V, Object>) getValueAdapter(field, config);

        // deserialize entries
        Map<K, V> map = new HashMap<K, V>();
        for (Entry<String, Object> e : serialized.attributes.entrySet()) {
            map.put((K) e.getKey(), valueAdapter.deserialize(e.getValue(), config, field));
        }

        return map;
    }

    @Override
    public ConfigNode serialize(Map<K, V> obj, ReflectiveConfig config, Field field) throws ConfigurationException {

        ConfigNode cnode = new ConfigNode();

        // getValueAdapter
        ConfigTypeAdapter<V, Object> valueAdapter = (ConfigTypeAdapter<V, Object>) getValueAdapter(field, config);

        for (Entry<K, V> e : obj.entrySet()) {
            cnode.attributes.put((String) e.getKey(), valueAdapter.serialize(e.getValue(), config, field));
        }

        return cnode;
    }

    @Override
    public void write(ConfigNode serialized, ReflectiveConfig config, ConfigWriter writer, Field field) throws ConfigurationException {
        ConfigField fieldAnno = field.getAnnotation(ConfigField.class);

        // getValueAdapter
        ConfigTypeAdapter<V, Object> valueAdapter = (ConfigTypeAdapter<V, Object>) getValueAdapter(field, config);

        String collectionName = (fieldAnno.alternativeCollectionName().equals("N/A") ? fieldAnno.name() : fieldAnno.alternativeCollectionName());
        ConfigWriter collectionWriter = writer.createChild(collectionName);
        
        for (Entry<String, Object> e : serialized.attributes.entrySet()) {
            
            ConfigWriter elementWriter = collectionWriter.getCollectionElementWriter( fieldAnno.mapKey(), e.getKey());
            
            valueAdapter.write(e.getValue(), config, elementWriter, field);
            elementWriter.flush();
        }
    }

    @Override
    public void merge(Map<K, V> prev, Map<K, V> curr, ReflectiveConfig config, DiffWriter diffwriter, Field field)
            throws ConfigurationException {

        //TODO
        
    }

}