package org.dcm4che.conf.core.util;


import org.dcm4che.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.api.generic.ConfigurableProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class ConfigIterators {

    public static class AnnotatedSetter {
        Map<Type, Annotation> annotations;
        List<AnnotatedConfigurableProperty> parameters;
        String name;
    }

    public static List<AnnotatedConfigurableProperty> getAllConfigurableFieldsAndSetterParameters(Class clazz) {
        List<AnnotatedConfigurableProperty> fields = getAllConfigurableFields(clazz);
        for (AnnotatedSetter s : getAllConfigurableSetters(clazz)) fields.addAll(s.parameters);
        return fields;
    }

    public static List<AnnotatedSetter> getAllConfigurableSetters(Class clazz) {
        List<AnnotatedSetter> list = new ArrayList<AnnotatedSetter>();

        // scan all methods including superclasses, assume each is a config-setter
        for (Method m : clazz.getMethods()) {
            AnnotatedSetter annotatedSetter = new AnnotatedSetter();
            annotatedSetter.parameters = new ArrayList<AnnotatedConfigurableProperty>();

            Annotation[][] parameterAnnotations = m.getParameterAnnotations();
            Type[] genericParameterTypes = m.getGenericParameterTypes();

            boolean thisMethodIsNotASetter = false;
            for (int i = 0; i < parameterAnnotations.length; i++) {

                AnnotatedConfigurableProperty property = new AnnotatedConfigurableProperty();
                property.setAnnotations(annotationsArrayToMap(parameterAnnotations[i]));
                property.setType(genericParameterTypes[i]);

                annotatedSetter.parameters.add(property);

                // make sure all the parameters of this setter-wannabe are annotated
                if (!property.getAnnotations().containsKey(ConfigurableProperty.class)) {
                    thisMethodIsNotASetter = true;
                    break;
                }
            }

            // filter out non-setters
            if (thisMethodIsNotASetter) continue;

            list.add(annotatedSetter);
            annotatedSetter.annotations = annotationsArrayToMap(m.getAnnotations());
            annotatedSetter.name = m.getName();
        }
        return list;
    }

    public static List<AnnotatedConfigurableProperty> getAllConfigurableFields(Class clazz) {
        List<AnnotatedConfigurableProperty> l = new ArrayList<AnnotatedConfigurableProperty>();

        // scan all fields from this class and superclasses
        for (Field field : getFieldsUpTo(clazz, null)) {
            if (field.getAnnotation(ConfigurableProperty.class) != null) {

                AnnotatedConfigurableProperty ap = new AnnotatedConfigurableProperty();
                ap.setAnnotations(annotationsArrayToMap(field.getAnnotations()));
                ap.setType(field.getGenericType());
                ap.setName(field.getName());

                l.add(ap);
            }
        }

        return l;
    }

    public static Map<Type, Annotation> annotationsArrayToMap(Annotation[] annos) {
        HashMap<Type, Annotation> annotations = new HashMap<Type, Annotation>();
        for (Annotation anno : annos)
            annotations.put(anno.getClass(), anno);
        return annotations;
    }

    /**
     * Iterates over the whole hierarchy of classes starting from the startClass.
     * Taken from http://stackoverflow.com/questions/16966629/what-is-the-difference-between-getfields-getdeclaredfields-in-java-reflection
     */
    public static Iterable<Field> getFieldsUpTo(Class<?> startClass,
                                                Class<?> exclusiveParent) {

        List<Field> currentClassFields = new ArrayList<Field>();
        currentClassFields.addAll(Arrays.asList(startClass.getDeclaredFields()));

        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null &&
                (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
            List<Field> parentClassFields =
                    (List<Field>) getFieldsUpTo(parentClass, exclusiveParent);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }

}
