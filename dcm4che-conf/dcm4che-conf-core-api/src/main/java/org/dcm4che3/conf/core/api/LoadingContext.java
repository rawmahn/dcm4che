package org.dcm4che3.conf.core.api;

import java.util.Map;

/**
 * @author rawmahn
 */
public interface LoadingContext extends ProcessingContext {

    /**
     * Either
     * <ul>
     * <li> uses the existing instance with this UUID from the pool </li>
     * <li> creates new instance </li>
     * </ul>
     */
    <T> T getRelevantConfigurableInstanceByUUID(String uuid, Class<T> clazz);

    /**
     * A map of UUID to already loaded object. Used to close the circular references loop instead of creating clones, and for "optimistic" reference resolution.
     * @return
     */
    Map<String, Object> getLoadedReferablesByUUID();


}
