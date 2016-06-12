package org.dcm4che3.conf.core.context;

import org.dcm4che3.conf.core.api.TypeSafeConfiguration;
import org.dcm4che3.conf.core.api.internal.BeanVitalizer;

/**
 * @author rawmahn
 */
public class SavingContext extends ProcessingContext {

    public SavingContext(BeanVitalizer vitalizer, TypeSafeConfiguration typeSafeConfiguration) {
        super(vitalizer, typeSafeConfiguration);
    }
}
