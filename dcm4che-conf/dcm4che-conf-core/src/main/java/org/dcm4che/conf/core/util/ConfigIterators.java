package org.dcm4che.conf.core.util;


import org.dcm4che3.conf.api.generic.ConfigurableProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

public class ConfigIterators {

    public class AnnotatedProperty {
        Map<Type,Annotation> annotations;
        Type type;
    }

    //Iterator<AnnotatedProperty>
    // traverse all incl. supercalsses
    // grab setters with configProperties
    // grab fields with confoigproperties
    // make list, make iterator
}
