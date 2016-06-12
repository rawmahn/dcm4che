package org.dcm4che3.conf.core.context;

import org.dcm4che3.conf.core.api.TypeSafeConfiguration;
import org.dcm4che3.conf.core.api.internal.BeanVitalizer;

/**
 * @author rawmahn
 */
public class ProcessingContext {

    protected final BeanVitalizer vitalizer;
    protected final TypeSafeConfiguration typeSafeConfiguration;

    public ProcessingContext(BeanVitalizer vitalizer, TypeSafeConfiguration typeSafeConfiguration) {
        this.vitalizer = vitalizer;
        this.typeSafeConfiguration = typeSafeConfiguration;
    }

    TypeSafeConfiguration getTypeSafeConfiguration() {
        return typeSafeConfiguration;
    }

    BeanVitalizer getVitalizer() {
        return vitalizer;
    }
}
