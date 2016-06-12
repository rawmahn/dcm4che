package org.dcm4che3.conf.core.context;

import org.dcm4che3.conf.core.api.TypeSafeConfiguration;
import org.dcm4che3.conf.core.api.internal.BeanVitalizer;

/**
 * @author rawmahn
 */
public class ContextFactory {

    private final TypeSafeConfiguration typeSafeConfiguration;
    private final BeanVitalizer vitalizer;

    public ContextFactory(TypeSafeConfiguration typeSafeConfiguration, BeanVitalizer vitalizer) {
        this.typeSafeConfiguration = typeSafeConfiguration;
        this.vitalizer = vitalizer;
    }

    public LoadingContext newLoadingContext() {
        return new LoadingContext(vitalizer, typeSafeConfiguration);
    }

    public SavingContext newSavingContext() {
        return new SavingContext(vitalizer, typeSafeConfiguration);
    }

    public ProcessingContext newProcessingContext() {
        return new ProcessingContext(vitalizer, typeSafeConfiguration);
    }

}
