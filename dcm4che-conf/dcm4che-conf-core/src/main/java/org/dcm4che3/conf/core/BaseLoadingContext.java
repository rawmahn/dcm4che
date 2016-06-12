package org.dcm4che3.conf.core;

import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.conf.core.api.LoadingContext;
import org.dcm4che3.conf.core.api.TypeSafeConfiguration;
import org.dcm4che3.conf.core.api.internal.BeanVitalizer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author rawmahn
 */
public class BaseLoadingContext implements LoadingContext {

    private final BeanVitalizer vitalizer;
    private final TypeSafeConfiguration typeSafeConfiguration;
    private final ConcurrentMap<String, Object> referables = new ConcurrentHashMap<String, Object>();

    public BaseLoadingContext(BeanVitalizer vitalizer, TypeSafeConfiguration typeSafeConfiguration) {
        this.vitalizer = vitalizer;
        this.typeSafeConfiguration = typeSafeConfiguration;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T getRelevantConfigurableInstanceByUUID(String uuid, Class<T> clazz) {

        // try to get from the pool
        if (uuid != null) {
            Object fromThePool = referables.get(uuid);

            if (fromThePool != null) {
                return castFromPool(fromThePool, clazz);
            }
        }

        T confObj;
        try {
            confObj = getVitalizer().newInstance(clazz);
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

    @Override
    public ConcurrentMap<String, Object> getLoadedReferablesByUUID() {
        return referables;
    }

    @Override
    public BeanVitalizer getVitalizer() {
        return vitalizer;
    }

    @Override
    public TypeSafeConfiguration getTypeSafeConfiguration() {
        return typeSafeConfiguration;
    }
}
