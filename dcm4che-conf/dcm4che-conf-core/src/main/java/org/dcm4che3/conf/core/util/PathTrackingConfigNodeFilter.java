package org.dcm4che3.conf.core.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks the current path from the root while tracersing
 */
public class PathTrackingConfigNodeFilter extends ConfigNodeTraverser.AConfigNodeFilter {

    final Deque<String> path = new ArrayDeque<String>();

//    @Override
    public void beforeListElement(int index) {
        path.push(Integer.toString(index));
    }

//    @Override
    public void afterListElement(int index1, int index2) {
        path.pop();
    }
}
