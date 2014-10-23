package org.dcm4che3.conf.core.misc;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.node.Visit;
import org.junit.Assert;

public class DeepEqualsDiffer {
    public static void assertDeepEquals(String message, Object expected, Object actual) {
        DiffNode root = ObjectDifferBuilder.buildDefault().compare(actual, expected);
        final StringBuilder builder = new StringBuilder("");
        root.visitChildren(new DiffNode.Visitor() {
            @Override
            public void node(DiffNode diffNode, Visit visit) {
                builder.append(diffNode.getPath().toString() + "\n");
            }
        });
        String diff = builder.toString();
        Assert.assertTrue(message + " | Difference:\n" + diff, diff.equals(""));
    }
}