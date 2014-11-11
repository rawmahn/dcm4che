package org.dcm4che3.conf.core.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.core.api.ConfigurableProperty;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * For now only supports primitive serialized representation, so no ConfigClass'ed classes as elements
 *
 * @param <T,ST>
 * @author Roman K
 */
//TODO: transform to collectiontypeadpater
//TODO: handle 'reference' and pass it deeper
public class CollectionTypeAdapter<T extends Collection> implements ConfigTypeAdapter<T, T> {

    private Class clazz;

    public CollectionTypeAdapter(Class<? extends T> clazz) {
        this.clazz = clazz;
    }

    private T createCollection(AnnotatedConfigurableProperty property) throws ConfigurationException {
        try {
            return (T) clazz.newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    private Collection createCollectionDeserialized(AnnotatedConfigurableProperty property) throws ConfigurationException {
        if (EnumSet.class.isAssignableFrom(BeanVitalizer.getRawClass(property))) {
            Class enumClass = (Class) DefaultConfigTypeAdapters.getTypeForGenericsParameter(property, 0);
            return EnumSet.noneOf(enumClass);
        } else
            return createCollection(property);
    }

    @Override
    public T fromConfigNode(T configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        AnnotatedConfigurableProperty elementPseudoProperty = DefaultConfigTypeAdapters.getPseudoPropertyForGenericsParamater(property, 0);

        ConfigTypeAdapter elementAdapter;
        if (property.getAnnotation(ConfigurableProperty.class).collectionOfReferences())
            elementAdapter = vitalizer.getReferenceTypeAdapter();
        else
            elementAdapter = vitalizer.lookupTypeAdapter(elementPseudoProperty);

        T collection = (T) createCollectionDeserialized(property);

        for (Object o : collection)
            collection.add(elementAdapter.fromConfigNode((T) o, elementPseudoProperty, vitalizer));

        return collection;
    }

    @Override
    public T toConfigNode(T object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        AnnotatedConfigurableProperty elementPseudoProperty = DefaultConfigTypeAdapters.getPseudoPropertyForGenericsParamater(property, 0);

        ConfigTypeAdapter elementAdapter;
        if (property.getAnnotation(ConfigurableProperty.class).collectionOfReferences())
            elementAdapter = vitalizer.getReferenceTypeAdapter();
        else
            elementAdapter = vitalizer.lookupTypeAdapter(elementPseudoProperty);

        T node = createCollection(property);
        for (Object element : object)
            node.add(elementAdapter.toConfigNode(element, elementPseudoProperty, vitalizer));

        return node;
    }

    @Override
    public Map<String, Object> getSchema(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Map<String, Object> metadata = new HashMap<String, Object>();
        Map<String, Object> elementMetadata = new HashMap<String, Object>();

        metadata.put("type", "array");

        AnnotatedConfigurableProperty elementPseudoProperty = DefaultConfigTypeAdapters.getPseudoPropertyForGenericsParamater(property, 0);

        ConfigTypeAdapter elementAdapter;
        if (property.getAnnotation(ConfigurableProperty.class).collectionOfReferences())
            elementAdapter = vitalizer.getReferenceTypeAdapter();
        else
            elementAdapter = vitalizer.lookupTypeAdapter(elementPseudoProperty);

        elementMetadata.putAll(elementAdapter.getSchema(elementPseudoProperty, vitalizer));
        metadata.put("items", elementMetadata);

        return metadata;
    }

    @Override
    public T normalize(Object configNode, AnnotatedConfigurableProperty property) throws ConfigurationException {
        return (T) configNode;
    }
}
