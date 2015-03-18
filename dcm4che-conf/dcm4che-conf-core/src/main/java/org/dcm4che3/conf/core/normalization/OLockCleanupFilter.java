package org.dcm4che3.conf.core.normalization;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.util.NodeTraverser;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by player on 17-Mar-15.
 */
public class OLockCleanupFilter extends NodeTraverser.NoopFilter{

    List<String> commonOLockPropnames;

    public OLockCleanupFilter(String... commonOLockPropnames) {
        this.commonOLockPropnames = new ArrayList<String>(Arrays.asList(commonOLockPropnames));
    }

    @Override
    public void onNodeBegin(Map<String, Object> node, Class clazz) throws ConfigurationException {

        for (String oLockPropname : commonOLockPropnames) node.remove(oLockPropname);

    }
}
