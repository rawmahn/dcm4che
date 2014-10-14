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
        private Map<Type, Annotation> annotations;
        private List<AnnotatedConfigurableProperty> parameters;
        private Method method;

        public Map<Type, Annotation> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(Map<Type, Annotation> annotations) {
            this.annotations = annotations;
        }

        public <T> T getAnnotation(Class<T> annotationType) {
            return (T) annotations.get(annotationType);
        }

        public List<AnnotatedConfigurableProperty> getParameters() {
            return parameters;
        }

        public void setParameters(List<AnnotatedConfigurableProperty> parameters) {
            this.parameters = parameters;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }
    }

    public static List<AnnotatedConfigurableProperty> getAllConfigurableFieldsAndSetterParameters(Class clazz) {
        List<AnnotatedConfigurableProperty> fields = getAllConfigurableFields(clazz);
        for (AnnotatedSetter s : getAllConfigurableSetters(clazz)) fields.addAll(s.getParameters());
        return fields;
    }

    public static List<AnnotatedSetter> getAllConfigurableSetters(Class clazz) {
        List<AnnotatedSetter> list = new ArrayList<AnnotatedSetter>();

        // scan all methods including superclasses, assume each is a config-setter
        for (Method m : clazz.getMethods()) {
            AnnotatedSetter annotatedSetter = new AnnotatedSetter();
            annotatedSetter.setParameters(new ArrayList<AnnotatedConfigurableProperty>());

            Annotation[][] parameterAnnotations = m.getParameterAnnotations();
            Type[] genericParameterTypes = m.getGenericParameterTypes();

            // if method is no-arg, then it is not a setter
            boolean thisMethodIsNotASetter = true;

            for (int i = 0; i < parameterAnnotations.length; i++) {

                thisMethodIsNotASetter = false;

                AnnotatedConfigurableProperty property = new AnnotatedConfigurableProperty();
                property.setAnnotations(annotationsArrayToMap(parameterAnnotations[i]));
                property.setType(genericParameterTypes[i]);

                annotatedSetter.getParameters().add(property);

                // make sure all the parameters of this setter-wannabe are annotated
                if (property.getAnnotation(ConfigurableProperty.class) == null) {
                    thisMethodIsNotASetter = true;
                    break;
                }
            }

            // filter out non-setters
            if (thisMethodIsNotASetter) continue;

            list.add(annotatedSetter);
            annotatedSetter.setAnnotations(annotationsArrayToMap(m.getAnnotations()));
            annotatedSetter.setMethod(m);
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
            annotations.put(anno.annotationType(), anno);
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
