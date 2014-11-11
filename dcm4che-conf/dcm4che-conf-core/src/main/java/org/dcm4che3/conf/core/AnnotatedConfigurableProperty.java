package org.dcm4che3.conf.core;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.api.ConfigurableProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Roman K
 */
public class AnnotatedConfigurableProperty {
    private Map<Type, Annotation> annotations = new HashMap<Type, Annotation>();
    private Type type;
    private String name;

    public AnnotatedConfigurableProperty() {
    }

    public AnnotatedConfigurableProperty(Type type) {
        setType(type);
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

    public String getAnnotatedName() throws ConfigurationException {

        String name = getAnnotation(ConfigurableProperty.class).name();
        if (!name.equals("")) return name;
        name = this.name;
        if (name != null) return name;
        throw new ConfigurationException("Property name not specified");

    }

    public void setName(String name) {
        this.name = name;
    }
}
