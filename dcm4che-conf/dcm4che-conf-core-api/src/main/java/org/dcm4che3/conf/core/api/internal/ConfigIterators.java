/*
 * **** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *  Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 *  The Initial Developer of the Original Code is
 *  Agfa Healthcare.
 *  Portions created by the Initial Developer are Copyright (C) 2014
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *  See @authors listed below
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 2 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 *
 *  ***** END LICENSE BLOCK *****
 */
package org.dcm4che3.conf.core.api.internal;


import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableClassExtension;
import org.dcm4che3.conf.core.api.ConfigurableProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * This class shall NOT be referenced externally, it will be removed/renamed/refactored without notice.
 *
 * @author Roman K
 */
public class ConfigIterators {

    /*
     * Caches to overcome slow reflection access
     */


    private static final Map<Class, List<AnnotatedConfigurableProperty>> configurableFieldsCache = Collections.synchronizedMap(new HashMap<Class, List<AnnotatedConfigurableProperty>>());
    private static final Map<Class, Boolean> isClassConfigurable = Collections.synchronizedMap(new HashMap<Class, Boolean>());


    public static List<AnnotatedConfigurableProperty> getAllConfigurableFields(Class clazz) {

        //check cache
        List<AnnotatedConfigurableProperty> l = configurableFieldsCache.get(clazz);
        if (l != null) return l;
        processAndCacheAnnotationsForClass(clazz);

        return configurableFieldsCache.get(clazz);

    }

    public static boolean isConfigurableClass(Class clazz) {

        if (isClassConfigurable.containsKey(clazz))
            return isClassConfigurable.get(clazz);

        boolean isItForReal = clazz.getAnnotation(ConfigurableClass.class) != null;
        isClassConfigurable.put(clazz, isItForReal);

        return isItForReal;
    }

    public static AnnotatedConfigurableProperty getUUIDPropertyForClass(Class clazz) {


        for (AnnotatedConfigurableProperty annotatedConfigurableProperty : getAllConfigurableFields(clazz)) {
            if (annotatedConfigurableProperty.isUuid())
                return annotatedConfigurableProperty;
        }
        return null;
    }

    private static void processAndCacheAnnotationsForClass(Class clazz) {
        processAnnotatedProperties(clazz);


        ConfigurableClass configClassAnno = (ConfigurableClass) clazz.getAnnotation(ConfigurableClass.class);
        if (configClassAnno == null)
            throw new IllegalArgumentException("Class '" + clazz.getName() + "' is not a configurable class. Make sure the a dependency to org.dcm4che.conf.core-api exists.");

        //// safeguards

        // refs vs extensions
        if (configClassAnno.referable() && ConfigurableClassExtension.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("A configurable extension class MUST NOT be referable - violated by class " + clazz.getName());

        // make sure there is at most one UUID and olockHash
        int uuidProps = 0;
        int olockProps = 0;


        for (AnnotatedConfigurableProperty annotatedConfigurableProperty : getAllConfigurableFields(clazz)) {
            if (annotatedConfigurableProperty.isUuid()) uuidProps++;
            if (annotatedConfigurableProperty.isOlockHash()) olockProps++;
        }
        if (uuidProps > 1)
            throw new IllegalArgumentException("A configurable class MUST NOT have more than one UUID field - violated by class " + clazz.getName());
        if (olockProps > 1)
            throw new IllegalArgumentException("A configurable class MUST NOT have more than one optimistic locking hash field - violated by class " + clazz.getName());


    }


    private static List<AnnotatedConfigurableProperty> processAnnotatedProperties(Class clazz) {
        List<AnnotatedConfigurableProperty> l;
        l = new ArrayList<AnnotatedConfigurableProperty>();

        // scan all fields from this class and superclasses
        for (Field field : getAllFields(clazz)) {
            if (field.getAnnotation(ConfigurableProperty.class) != null) {

                AnnotatedConfigurableProperty ap = new AnnotatedConfigurableProperty(
                        annotationsArrayToMap(field.getAnnotations()),
                        field.getName(),
                        field.getGenericType()
                );
                l.add(ap);
            }
        }

        configurableFieldsCache.put(clazz, l);
        return l;
    }

    public static Map<Type, Annotation> annotationsArrayToMap(Annotation[] annos) {
        HashMap<Type, Annotation> annotations = new HashMap<Type, Annotation>();
        for (Annotation anno : annos)
            annotations.put(anno.annotationType(), anno);
        return annotations;
    }

    /**
     * Gets all the fields for the class and it's superclass(es)
     */
    public static List<Field> getAllFields(Class<?> clazz) {

        List<Field> fields = new ArrayList<Field>();

        // get all fields of the current class (includes public, protected, default, and private fields)
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        // go to the parent recursively
        Class<?> parent = clazz.getSuperclass();
        if (parent != null)
            fields.addAll(getAllFields(parent));

        return fields;
    }

}
