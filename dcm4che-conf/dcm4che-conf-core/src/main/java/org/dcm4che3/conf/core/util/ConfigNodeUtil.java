package org.dcm4che3.conf.core.util;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigNodeUtil {


    public static String concat(String path1, String path2) {
        String res = path1 + "/" + path2;
        return res.replace("///", "/").replace("//", "/");
    }

    public static void replaceNode(Object rootConfigNode, String path, Object replacementConfigNode) {

        JXPathContext.newContext(rootConfigNode).createPathAndSetValue(path,replacementConfigNode);
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
        // TODO: implement validation, as we do not allow slashes, commas quotes etc
        // TODO: implement escaping
        return str;
    }

    private final static String IDENTIFIER = "[a-zA-Z\\d_]+";
    private static final String VALUE = "(('.+?')|(\\-?[\\d]+)|true|false)";
    private final static String IDENTIFIER_NAMED = "(?<identifier>" + IDENTIFIER + ")";
    private static final String VALUE_NAMED = "(('(?<strvalue>.+?)')|(?<intvalue>\\-?[\\d]+)|(?<boolvalue>true|false))";
    private static final String AND = " and ";
    private static final String APOS = "&apos;";


    private final static String XPREDICATE = "(" + IDENTIFIER + "=" + VALUE + ")";
    private final static String XPREDICATENAMED = "(" + IDENTIFIER_NAMED + "=" + VALUE_NAMED + ")";

    private final static String XPATHNODE = "/(?<nodename>" + IDENTIFIER + "|\\*)(\\[(?<predicates>" + XPREDICATE + "( and " + XPREDICATE + ")*)\\])?";
    private final static String XPATH = "(" + XPATHNODE + ")*";

    public final static Pattern xPathPattern  = Pattern.compile(XPATH);
    public final static Pattern xPathNodePattern = Pattern.compile(XPATHNODE);
    private final static Pattern xPredicatePattern = Pattern.compile(XPREDICATE);
    private final static Pattern xNamedPredicatePattern = Pattern.compile(XPREDICATENAMED);
    private final static Pattern xAndPattern = Pattern.compile(AND);
    private final static Pattern aposPattern = Pattern.compile(APOS);

    /**
     * Returns list of path elements.
     * $name - name
     * key - value
     *
     * @param s
     * @return
     */
    public static List<Map<String, Object>> parseReference(String s) {

        if (!xPathPattern.matcher(s).matches()) {
            throw new IllegalArgumentException("Failed to parse provided reference (" + s + ")");
        }


        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Matcher nodeMatcher = xPathNodePattern.matcher(s);
        while (nodeMatcher.find()) {

            Map<String, Object> propMap = new HashMap<String, Object>();
            list.add(propMap);

            String node = nodeMatcher.group();

            // nodename $name
            String nodeName = nodeMatcher.group("nodename");
            propMap.put("$name", nodeName);

            // now key-value
            String predicatesStr = nodeMatcher.group("predicates");
            if (predicatesStr != null) {
                String[] predicates = xAndPattern.split(predicatesStr);

                for (String p : predicates) {
                    Matcher matcher = xNamedPredicatePattern.matcher(p);
                    if (!matcher.find()) throw new RuntimeException("Unexpected error");


                    String boolvalue = matcher.group("boolvalue");
                    String intvalue = matcher.group("intvalue");
                    String strvalue = matcher.group("strvalue");

                    Object value;
                    if (boolvalue != null)
                        value = Boolean.parseBoolean(boolvalue);
                    else if (intvalue != null)
                        value = Integer.parseInt(intvalue);
                    else if (strvalue != null)
                        value = strvalue.replace(APOS, "'");
                    else throw new RuntimeException("Unexpected error: no value");


                    String identifier = matcher.group("identifier");
                    propMap.put(identifier, value);

                }


            }

        } ;

        return list;
    }

    public static String escapeApos(String name) {
        return name.replace("'", "&apos;");
    }
}
