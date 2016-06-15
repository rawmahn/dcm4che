package org.dcm4che3.conf.core.context;

import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.conf.core.api.TypeSafeConfiguration;
import org.dcm4che3.conf.core.api.internal.BeanVitalizer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

/**
 * @author rawmahn
 */
public class LoadingContext extends ProcessingContext {


    /**
     * A map of UUID to a loaded (being loaded) object. Used to close the circular references loop instead of creating clones, and for "optimistic" reference resolution.
     *
     * @return
     */
    private final ConcurrentHashMap<String, Future<Object>> referables = new ConcurrentHashMap<String, Future<Object>>();

    public LoadingContext(BeanVitalizer vitalizer, TypeSafeConfiguration typeSafeConfiguration) {
        super(vitalizer, typeSafeConfiguration);
    }


    /**
     * Behaves like {@link ConcurrentMap#putIfAbsent}
     * @param uuid config object uuid
     * @param o the candidate future
     * @return
     */
    public Future<Object> registerConfigObjectFutureIfAbsent(String uuid, Future<Object> o) {
        return referables.putIfAbsent(uuid, o);
    }






    /**
     * Either
     * <ul>
     * <li> uses the existing instance with this UUID from the pool </li>
     * <li> creates new instance </li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public <T> T findOrCreateConfigurableInstanceByUUID(String uuid, Class<T> clazz) {

        // try to get from the pool
        if (uuid != null) {
            Object fromThePool = referables.get(uuid);

            if (fromThePool != null) {
                return castFromPool(fromThePool, clazz);
            }
        }

        T confObj;
        try {
            confObj = vitalizer.newInstance(clazz);
        } catch (Exception e) {
            throw new ConfigurationException("Error while instantiating config class " + clazz.getSimpleName()
                    + ". Check whether null-arg constructor exists.", e);
        }

        // if uuid is defined, put into pool
        if (uuid != null) {
            // need to add this fresh instance to the pool
            Object prev = referables.putIfAbsent(uuid, confObj);

            // This should not happen in 99% cases.
            // But if it happens, then it's a race condition, so just use the object that the other guy inserted.
            if (prev != null) {
                return castFromPool(prev, clazz);
            }
        }

        return confObj;
    }

    @SuppressWarnings("unchecked")
    private <T> T castFromPool(Object fromThePool, Class<T> clazz) {
        try {
            return (T) fromThePool;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Expected instance of class " + clazz.getName()
                    + " but the pool contained an instance of class " + fromThePool.getClass().getName(), e);
        }
    }


}
