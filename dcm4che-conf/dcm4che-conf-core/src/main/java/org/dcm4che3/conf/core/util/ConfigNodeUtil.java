package org.dcm4che3.conf.core.util;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import sun.misc.Regexp;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigNodeUtil {


    public static String concat(String path1,String path2) {
        String res = path1+"/"+path2;
        return res.replace("///","/").replace("//","/");
    }

    public static void replaceNode(Object rootConfigNode, String path, Object replacementConfigNode) {
        JXPathContext.newContext(rootConfigNode).getPointer(path).setValue(replacementConfigNode);
    }

    public static Object getNode(Object rootConfigNode, String path) {
        try {
            return JXPathContext.newContext(rootConfigNode).getValue(path);
        } catch (JXPathNotFoundException e) {
            return null;
        }
    }

    public static boolean nodeExists(Map<String, Object> rootConfigNode, String path) {
        return getNode(rootConfigNode, path) != null;
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

    public static String escape(String str) {
        // TODO: implement escaping
        return str;
    }

        private final static String IDENTIFIER = "([a-zA-Z\\d_]+)";
        private final static String XPREDICATE = "("+IDENTIFIER+"=(('.+?')|(\\-?[\\d\\.]+)|true|false))";
        private final static String XPATHNODE = "/("+IDENTIFIER+"|\\*)(\\["+XPREDICATE+"( and "+XPREDICATE+")*\\])?";
        private final static String XPATH = "("+XPATHNODE+")*";


        private final static Pattern xPathPattern = Pattern.compile(XPATH);
        private final static Pattern xPathNodePattern = Pattern.compile(XPATHNODE);

    /**
     * Returns list of path elements.
     * $name - name
     * key - value
     * @param s
     * @return
     */
    public static List<Map<String, Object>> parseReference(String s) {


        String input = "/dicomConfigurationRoot/dicomDeviceRoot/*[deviceName='Qoute&apos;here']/dicomConnection[dicomPort=101 and dicomHostname='myhl7']";
        if (!xPathPattern.matcher(input).matches()) {
            throw new IllegalArgumentException("Failed to parse provided reference ("+input+")");
        }

        Matcher nodeMatcher = xPathNodePattern.matcher(input);
        while (nodeMatcher.find()) {
            System.out.println(nodeMatcher.group());
        };

        return null;
    }
}
