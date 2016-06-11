package org.dcm4che3.conf.core;

import org.dcm4che3.conf.core.api.LoadingContext;

/**
 * @author rawmahn
 */
public class UUIDReference <T> {

    private transient T resolved;
    /**
     * From here we should get TypeSafeConfig and be able to load stuff
     */
    private final transient LoadingContext ctx;

    private final String UUID;


    public UUIDReference(LoadingContext ctx, String uuid) {
        this.ctx = ctx;
        UUID = uuid;
    }

    T resolve() {
        if (resolved != null) {
            return resolved;
        } else {
            synchronized (this) {
                // TODO go ahead and load usign ctx
                return null;
            }
        }
    }


}
