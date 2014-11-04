package org.dcm4che3.conf.core.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by aprvf on 13/10/2014.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.FIELD})
public @interface LDAP {

    String distinguishingField() default "cn";

    /**
     * Can be used on classes and on fields
     * @return
     */
    boolean doNotCreateChildNode() default false;


    String[] objectClasses() default {};


    // maps mapping, these


    String mapKeyAttribute() default "";

    // needed if map entry value is a primitive
    String mapValueAttribute() default "";

    // needed if map entry value is a primitive
    String mapEntryObjectClass() default "";
}
