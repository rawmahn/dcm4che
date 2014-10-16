package org.dcm4che.conf.core.adapters;

import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che.conf.core.BeanVitalizer;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationUnserializableException;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Array
 */
public class ArrayTypeAdapter implements ConfigTypeAdapter<Object, Object> {

    private static final Map<Class,Class> PRIMITIVE_TO_WRAPPER;

    static {

        PRIMITIVE_TO_WRAPPER = new HashMap<Class, Class>();
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(void.class, Void.class);
    }
    @Override
    public Object fromConfigNode(Object configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Class<?> componentType = ((Class) property.getType()).getComponentType();

        // handle null
        if (configNode == null) return Array.newInstance(componentType, 0);

        // if it is a collection, create an array with proper component type
        if (Collection.class.isAssignableFrom(configNode.getClass())) {
            Collection l = ((Collection) configNode);
            Object arr = Array.newInstance(componentType, l.size());
            int i = 0;
            for (Object el : l) {
                // deserialize element
                ConfigTypeAdapter elementTypeAdapter = vitalizer.lookupTypeAdapter(componentType);
                el = elementTypeAdapter.fromConfigNode(el, new AnnotatedConfigurableProperty(componentType), vitalizer);

                // push to array
                try {
                    Array.set(arr, i++, el);
                } catch (IllegalArgumentException e){
                    throw new ConfigurationException("Element type in the supplied collection does not match the target array's component type ( "+el.getClass().getName()+" vs "+componentType.getName()+" )", e);
                }
            }
            return arr;
        } else
            throw new ConfigurationException("Object of unexpected type (" + configNode.getClass().getName() + ") supplied for conversion into an array. Must be a collection.");
    }

    @Override
    public Object toConfigNode(Object object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationUnserializableException {
        return arrayToList(object);
    }

    /**
     * Converts an array to a List, casts primitives to wrappers
     * @param object
     * @return
     */
    private Object arrayToList(Object object) {
        Class<?> componentType = object.getClass().getComponentType();
        Class wrapperClass = PRIMITIVE_TO_WRAPPER.get(componentType);

        ArrayList list = new ArrayList();
        for (int i = 0; i < Array.getLength(object); i++) {
            list.add(wrapperClass != null ? wrapperClass.cast(Array.get(object,i)) : Array.get(object,i));
        }
        return list;
    }

    @Override
    public Map<String, Object> getMetadata(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("type", "Array");

        Class<?> componentType = ((Class) property.getType()).getComponentType();
        metadata.put("elementMetadata", vitalizer.lookupTypeAdapter(componentType).getMetadata(new AnnotatedConfigurableProperty(componentType), vitalizer));

        return metadata;
    }

}
