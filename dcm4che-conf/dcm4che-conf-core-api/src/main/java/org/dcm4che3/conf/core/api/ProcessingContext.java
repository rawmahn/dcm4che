package org.dcm4che3.conf.core.api;

import org.dcm4che3.conf.core.api.internal.BeanVitalizer;

/**
 * @author rawmahn
 */
public interface ProcessingContext {

    TypeSafeConfiguration getTypeSafeConfiguration();

    BeanVitalizer getVitalizer();
}
