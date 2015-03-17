package org.dcm4che3.conf.core;

/**
 * @author Roman K
 */
public @interface Migration {

    String NOVERSION = "---";

    String fromVersion() default NOVERSION;
    String toVersion();
}
