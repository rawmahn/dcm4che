package org.dcm4che.conf.core.adapters;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che3.conf.api.ConfigurationException;

/**
 * For now only supports primitive serialized representation, so no ConfigClass'ed classes as elements
 * @author Roman K
 *
 * @param <T,ST>
 */
public class SetTypeAdapter<T, ST> implements ConfigTypeAdapter<Set<T>, List<ST>> {

    @Override
    public Set<T> fromConfigNode(List<ST> configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Type setElementType = DefaultConfigTypeAdapters.getTypeForGenericsParameter(property, 0);
        ConfigTypeAdapter<T, ST> adapterForGenericsParameter = vitalizer.lookupTypeAdapter(setElementType);
        AnnotatedConfigurableProperty setElementPseudoProperty = new AnnotatedConfigurableProperty(setElementType);

        Set<T> set = new HashSet<T>();
        for (ST s : configNode)
            set.add(adapterForGenericsParameter.fromConfigNode(s, setElementPseudoProperty, vitalizer));
        return set;
    }

    @Override
    public List<ST> toConfigNode(Set<T> object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        List<ST> node = new ArrayList<ST>(object.size());
        for (T element : object) {
            ConfigTypeAdapter adapter = vitalizer.lookupTypeAdapter(element.getClass());
            node.add((ST) adapter.toConfigNode(element, property , vitalizer));
        }
        return node;
    }

    @Override
    public Map<String, Object> getMetadata(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Map<String, Object> metadata =  new HashMap<String, Object>();
        Map<String, Object> elementMetadata =  new HashMap<String, Object>();
        
        metadata.put("type", "Set");

        Type typeForGenericsParameter = DefaultConfigTypeAdapters.getTypeForGenericsParameter(property, 0);
        ConfigTypeAdapter<T,ST> adapter = vitalizer.lookupTypeAdapter(typeForGenericsParameter);

        elementMetadata.putAll(adapter.getMetadata(new AnnotatedConfigurableProperty(typeForGenericsParameter), vitalizer));
        metadata.put("elementMetadata", elementMetadata);
        
        return metadata;
    }

}
