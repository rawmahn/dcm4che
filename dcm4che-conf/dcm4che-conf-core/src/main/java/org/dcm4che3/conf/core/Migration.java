package org.dcm4che3.conf.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Roman K
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Migration {

    String NOVERSION = "---";

    String fromVersion() default NOVERSION;
    String toVersion();
}
