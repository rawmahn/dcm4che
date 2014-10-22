package org.dcm4che.conf.core.adapters;

import java.lang.reflect.Type;
import java.util.*;

import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che3.conf.api.ConfigurationException;

/**
 * For now only supports primitive serialized representation, so no ConfigClass'ed classes as elements
 * @author Roman K
 *
 * @param <T,ST>
 */
public class SetTypeAdapter<T, ST> implements ConfigTypeAdapter<Set<T>, Collection<ST>> {

    @Override
    public Set<T> fromConfigNode(Collection<ST> configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Type setElementType = DefaultConfigTypeAdapters.getTypeForGenericsParameter(property, 0);
        ConfigTypeAdapter<T, ST> adapterForGenericsParameter = vitalizer.lookupTypeAdapter(setElementType);
        AnnotatedConfigurableProperty setElementPseudoProperty = new AnnotatedConfigurableProperty(setElementType);

        Set<T> set = new HashSet<T>();
        for (ST s : configNode)
            set.add(adapterForGenericsParameter.fromConfigNode(s, setElementPseudoProperty, vitalizer));
        return set;
    }

    @Override
    public Collection<ST> toConfigNode(Set<T> object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        HashSet<ST> node = new HashSet<ST>(object.size());
        for (T element : object) {
            ConfigTypeAdapter adapter = vitalizer.lookupTypeAdapter(element.getClass());
            node.add((ST) adapter.toConfigNode(element, property , vitalizer));
        }
        return node;
    }

    @Override
    public Map<String, Object> getSchema(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Map<String, Object> metadata =  new HashMap<String, Object>();
        Map<String, Object> elementMetadata =  new HashMap<String, Object>();
        
        metadata.put("type", "array");

        Type typeForGenericsParameter = DefaultConfigTypeAdapters.getTypeForGenericsParameter(property, 0);
        ConfigTypeAdapter<T,ST> adapter = vitalizer.lookupTypeAdapter(typeForGenericsParameter);

        elementMetadata.putAll(adapter.getSchema(new AnnotatedConfigurableProperty(typeForGenericsParameter), vitalizer));
        metadata.put("items", elementMetadata);
        
        return metadata;
    }

}
