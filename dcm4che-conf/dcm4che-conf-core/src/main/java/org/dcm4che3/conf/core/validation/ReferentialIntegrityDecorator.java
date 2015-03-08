package org.dcm4che3.conf.core.validation;

import org.dcm4che3.conf.core.Configuration;
import org.dcm4che3.conf.core.DelegatingConfiguration;

/**
 * Created by player on 07-Mar-15.
 */
public class ReferentialIntegrityDecorator extends DelegatingConfiguration {

    public ReferentialIntegrityDecorator(Configuration delegate) {
        super(delegate);
    }
    
    
}
