package org.dcm4che.conf.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author Roman K
 */
public class AnnotatedConfigurableProperty {
    private Map<Type, Annotation> annotations;
    private Type type;
    private String name;

    public AnnotatedConfigurableProperty() {
    }

    public AnnotatedConfigurableProperty(Type componentType) {
        setType(componentType);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAnnotation(Class<T> annotationType) {
        return (T) annotations.get(annotationType);
    }

    public void setAnnotations(Map<Type, Annotation> annotations) {
        this.annotations = annotations;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
