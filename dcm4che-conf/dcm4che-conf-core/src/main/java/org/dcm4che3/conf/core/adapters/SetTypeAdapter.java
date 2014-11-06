package org.dcm4che3.conf.core.adapters;

import java.lang.reflect.Type;
import java.util.*;

import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.api.ConfigurationException;

/**
 * For now only supports primitive serialized representation, so no ConfigClass'ed classes as elements
 * @author Roman K
 *
 * @param <T,ST>
 */
//TODO: transform to collectiontypeadpater
//TODO: handle 'reference' and pass it deeper
public class SetTypeAdapter<T, ST> implements ConfigTypeAdapter<Set<T>, Collection<ST>> {

    @Override
    public Set<T> fromConfigNode(Collection<ST> configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Type setElementType = DefaultConfigTypeAdapters.getTypeForGenericsParameter(property, 0);
        AnnotatedConfigurableProperty setElementPseudoProperty = new AnnotatedConfigurableProperty(setElementType);
        ConfigTypeAdapter<T, ST> adapterForGenericsParameter = vitalizer.lookupTypeAdapter(setElementPseudoProperty);

        Set<T> set = new HashSet<T>();
        for (ST s : configNode)
            set.add(adapterForGenericsParameter.fromConfigNode(s, setElementPseudoProperty, vitalizer));
        return set;
    }

    @Override
    public Collection<ST> toConfigNode(Set<T> object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        HashSet<ST> node = new HashSet<ST>(object.size());
        for (T element : object) {
            AnnotatedConfigurableProperty elemPseudoProperty = new AnnotatedConfigurableProperty(element.getClass());
            ConfigTypeAdapter adapter = vitalizer.lookupTypeAdapter(elemPseudoProperty);
            node.add((ST) adapter.toConfigNode(element, elemPseudoProperty , vitalizer));
        }
        return node;
    }

    @Override
    public Map<String, Object> getSchema(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Map<String, Object> metadata =  new HashMap<String, Object>();
        Map<String, Object> elementMetadata =  new HashMap<String, Object>();
        
        metadata.put("type", "array");

        Type typeForGenericsParameter = DefaultConfigTypeAdapters.getTypeForGenericsParameter(property, 0);
        AnnotatedConfigurableProperty elementPseudoProperty = new AnnotatedConfigurableProperty(typeForGenericsParameter);

        ConfigTypeAdapter<T,ST> adapter = vitalizer.lookupTypeAdapter(elementPseudoProperty);

        elementMetadata.putAll(adapter.getSchema(elementPseudoProperty, vitalizer));
        metadata.put("items", elementMetadata);
        
        return metadata;
    }

    @Override
    public Collection<ST> normalize(Object configNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
        return (Collection<ST>) configNode;
    }
}
