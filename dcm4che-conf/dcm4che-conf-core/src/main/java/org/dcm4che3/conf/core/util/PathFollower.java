package org.dcm4che3.conf.core.util;

import org.dcm4che3.conf.core.api.Path;
import org.dcm4che3.conf.core.api.internal.ConfigProperty;
import org.dcm4che3.conf.core.api.internal.ConfigReflection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * @author rawmahn
 */
public class PathFollower {


    /**
     * Walk the given path 'through' the class structures and return the 'trace' of all visited properties
     *
     * @return
     */
    public static Deque<ConfigProperty> makeTrace(Class rootConfigurableClazz, Path path) {


        ArrayDeque<ConfigProperty> trace = new ArrayDeque<ConfigProperty>(path.getPathItems().size());

        ConfigProperty current = ConfigReflection.getDummyPropertyForClass(rootConfigurableClazz);

        for (Object i : path.getPathItems()) {

            trace.push(current);

            if ((i instanceof String && !current.isConfObject() && !current.isMap())
                    || (i instanceof Number && !current.isCollection())) {
                throw new IllegalArgumentException(
                        "Unexpected path element " + i + " in path " + path + " for root class " + rootConfigurableClazz + ", corresponding property " + current
                );
            }


            if (current.isConfObject()) {
                List<ConfigProperty> props = ConfigReflection.getAllConfigurableFields(current.getRawClass());
                for (ConfigProperty prop : props) {
                    boolean found = false;
                    if (prop.getAnnotatedName().equals(i)) {
                        current = prop;
                        found = true;
                        break;
                    }

                    if (!found)
                        throw new IllegalArgumentException("Cannot find the property with name " + i + " in class " + current.getRawClass() + " while tracing path " + path);

                }
            }

            if (current.isMap()) {
                current.getPseudoPropertyForCollectionElement()


            }
        }


        follow(rootConfigurableClazz, path, trace);

        return trace;

    }


}
