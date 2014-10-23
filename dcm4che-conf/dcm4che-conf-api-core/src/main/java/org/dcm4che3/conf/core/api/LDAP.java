package org.dcm4che3.conf.core.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by aprvf on 13/10/2014.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LDAP {

    String objectClass() default "";

    String distinguishingField() default "";
}
