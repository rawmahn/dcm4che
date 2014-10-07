package org.dcm4che.conf.core.util;

import org.apache.commons.jxpath.JXPathContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConfigPathUtil {


    public static void replaceNode(Object rootConfigNode, String path, Object replacementConfigNode) {
        JXPathContext.newContext(rootConfigNode).getPointer(path).setValue(replacementConfigNode);
    }

    public static Object getNode(Object rootConfigNode, String path) {
        return JXPathContext.newContext(rootConfigNode).getValue(path);
    }

    public static void removeNode(Map<String, Object> configurationRoot, String path) {
        JXPathContext.newContext(configurationRoot).removePath(path);
    }

    public static Iterator search(Map<String, Object> configurationRoot, String liteXPathExpression) throws IllegalArgumentException {
        return JXPathContext.newContext(configurationRoot).iterate(liteXPathExpression);
    }

    public static boolean validatePath(String path) {
        return true;
    }

    public String[] split(String path) {
        // TODO: support slash as a symbol with escaping?
        return path.split("/");
    }

}
