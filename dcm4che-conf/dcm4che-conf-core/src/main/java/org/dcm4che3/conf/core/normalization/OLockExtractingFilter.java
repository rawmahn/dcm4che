package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.util.NodeTraverser;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Created by player on 17-Mar-15.
 */
public class OLockExtractingFilter extends NodeTraverser.NoopFilter{

    String commonOLockPropname;

    public OLockExtractingFilter(String oLockPropname) {
        this.commonOLockPropname = oLockPropname;
    }

    @Override
    public void onNodeBegin(Map<String, Object> node, Class clazz) throws ConfigurationException {
        Annotation classAnno = clazz.getAnnotation(ConfigurableClass.class);
        if (classAnno == null) return;

        // if there is olock - extract it
        String optimisticLockPropertyName = ((ConfigurableClass) classAnno).optimisticLockPropertyName();
        if (!optimisticLockPropertyName.equals(ConfigurableClass.NO_LOCK_PROP))
            node.put(commonOLockPropname, node.get(optimisticLockPropertyName));
    }
}
