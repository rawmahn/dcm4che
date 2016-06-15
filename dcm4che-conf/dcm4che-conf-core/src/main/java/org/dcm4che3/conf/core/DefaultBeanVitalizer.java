/*
 * **** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *  Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 *  The Initial Developer of the Original Code is
 *  Agfa Healthcare.
 *  Portions created by the Initial Developer are Copyright (C) 2014
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *  See @authors listed below
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 2 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 *
 *  ***** END LICENSE BLOCK *****
 */
package org.dcm4che3.conf.core;

/**
 * @author Roman K
 */

import org.dcm4che3.conf.core.adapters.*;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.conf.core.api.TypeSafeConfiguration;
import org.dcm4che3.conf.core.api.internal.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.api.internal.BeanVitalizer;
import org.dcm4che3.conf.core.api.internal.ConfigIterators;
import org.dcm4che3.conf.core.api.internal.ConfigTypeAdapter;
import org.dcm4che3.conf.core.context.ContextFactory;
import org.dcm4che3.conf.core.context.LoadingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main class that is used to initialize annotated Java objects with settings fetched from a configuration backend.
 * These are mostly low-level access methods that should be used to build the API for configuration functionality of an end product
 */
public class DefaultBeanVitalizer implements BeanVitalizer {

    private final Map<Class, ConfigTypeAdapter> customConfigTypeAdapters = new HashMap<Class, ConfigTypeAdapter>();
    private int loadingTimeoutSec = 5;
    private ConfigTypeAdapter referenceTypeAdapter;

    private final ArrayTypeAdapter arrayTypeAdapter = new ArrayTypeAdapter();

    /**
     * Can be null!
     * TODO change to Optional when migrated to java 8
     */
    private final TypeSafeConfiguration typeSafeConfiguration;
    private final ContextFactory contextFactory;

    /**
     * "Standalone" vitalizer. This should only be used for e.g. tests.
     * To be able to handle references, custom context factories, etc, vitalizer must be bound to typeSafeConfiguration
     */
    public DefaultBeanVitalizer() {
        typeSafeConfiguration = null;
        contextFactory = new ContextFactory(null, this);
    }

    public DefaultBeanVitalizer(TypeSafeConfiguration typeSafeConfiguration) {
        this.typeSafeConfiguration = typeSafeConfiguration;
        this.contextFactory = typeSafeConfiguration.getContextFactory();
    }

    /**
     * Sets the timeout for resolving futures (objs being loaded by other threads) while loading the config.
     * <p/>
     * This is rather a defence-against-ourselves measure, the config futures should always get resolved/fail with an exception at some point.
     *
     * @param loadingTimeoutSec timeout. If <b>0</b> is passed, timeout is disabled.
     */
    public void setLoadingTimeoutSec(int loadingTimeoutSec) {
        this.loadingTimeoutSec = loadingTimeoutSec;
    }

    /**
     * Needed for avoiding infinite loops and 'optimistic' reference resolution (when we create the target instance before we actually find it during deserialization)
     */
    private final ThreadLocal<Map<String, Object>> currentlyLoadedReferableLocal = new ThreadLocal<Map<String, Object>>();


    public void setReferenceTypeAdapter(ConfigTypeAdapter referenceTypeAdapter) {
        this.referenceTypeAdapter = referenceTypeAdapter;
    }

    @Override
    public ConfigTypeAdapter getReferenceTypeAdapter() {
        return referenceTypeAdapter;
    }


    @Override
    public Object resolveFutureOrFail(String uuid, Future<Object> f) {
        try {

            if (loadingTimeoutSec == 0) {
                return f.get();
            } else {
                return f.get(loadingTimeoutSec, TimeUnit.SECONDS);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConfigurationException("Loading of configuration unexpectedly interrupted", e);

        } catch (ExecutionException e) {
            if (e.getCause() instanceof ConfigurationException) {
                throw (ConfigurationException) e.getCause();
            } else {
                throw new ConfigurationException("Error while loading configuration", e.getCause());
            }

        } catch (TimeoutException e) {
            throw new ConfigurationException("Time-out while waiting for the object [uuid=" + uuid + "] to be loaded", e);
        }
    }


    @Override
    public <T> T newConfiguredInstance(Map<String, Object> configNode, Class<T> clazz) throws ConfigurationException {
        boolean doCleanUpReferableLocal = false;

        // init reference threadlocal for this run if it's the first call (subsequent recursive calls will re-use the threadLocal)
        if (currentlyLoadedReferableLocal.get() == null) {
            currentlyLoadedReferableLocal.set(new HashMap<String, Object>());
            doCleanUpReferableLocal = true;
        }
        try {
            return new ReflectiveAdapter<T>().fromConfigNode(configNode, new AnnotatedConfigurableProperty(clazz), contextFactory.newLoadingContext(), null);

        } finally {
            if (doCleanUpReferableLocal)
                currentlyLoadedReferableLocal.remove();
        }
    }

    @Override
    public <T> T newConfiguredInstance(Map<String, Object> configurationNode, Class<T> clazz, LoadingContext ctx) {


        // TODO: use ctx for circ ref handling
        // TODO: add custom factories for objects (i.e. Groups tc )
        return newConfiguredInstance(configurationNode, clazz);
    }

    /**
     * Creates a new instance.
     *
     * @param clazz
     * @param <T>
     * @return
     * @throws ConfigurationException
     */
    @Override
    public <T> T newInstance(Class<T> clazz) throws ConfigurationException {
        try {

            return clazz.newInstance();

        } catch (InstantiationException e) {
            throw new ConfigurationException(e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Scans for annotations in <i>object</i> and initializes all its properties from the provided configuration node.
     *
     * @param <T>
     * @param object
     * @param configNode
     * @return
     */
    private <T> void configureInstance(T object, Map<String, Object> configNode, Class configurableClass) throws ConfigurationException {

        boolean doCleanUpReferableLocal = false;

        // init reference threadlocal for this run if it's the first call (subsequent recursive calls will re-use the threadLocal)
        if (currentlyLoadedReferableLocal.get() == null) {
            currentlyLoadedReferableLocal.set(new HashMap<String, Object>());
            doCleanUpReferableLocal = true;
        }
        try {

            new ReflectiveAdapter<T>(object).fromConfigNode(configNode, new AnnotatedConfigurableProperty(configurableClass), contextFactory.newLoadingContext(), null);
        } finally {
            if (doCleanUpReferableLocal)
                currentlyLoadedReferableLocal.remove();
        }
    }

    @Override
    public <T> T getInstanceFromThreadLocalPoolByUUID(String uuid, Class<T> expectedClazz) {
        if (currentlyLoadedReferableLocal.get() == null) return null;
        try {
            return (T) currentlyLoadedReferableLocal.get().get(uuid);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Expected instance of class " + expectedClazz.getName()
                    + " but the pool contained an instance of class " + currentlyLoadedReferableLocal.get().get(uuid).getClass().getName(), e);
        }
    }

    @Override
    public void registerInstanceInThreadLocalPool(String uuid, Object instance) {
        if (currentlyLoadedReferableLocal.get() == null) return;
        currentlyLoadedReferableLocal.get().put(uuid, instance);
    }

    @Override
    public <T> Map<String, Object> createConfigNodeFromInstance(T object) throws ConfigurationException {
        return createConfigNodeFromInstance(object, object.getClass());
    }


    @Override
    public <T> Map<String, Object> createConfigNodeFromInstance(T object, Class configurableClass) throws ConfigurationException {
        LoadingContext result;

        contextFactory.newLoadingContext();
        return (Map<String, Object>) lookupDefaultTypeAdapter(configurableClass).toConfigNode(object, new AnnotatedConfigurableProperty(configurableClass), contextFactory.newSavingContext());
    }


    @Override
    @SuppressWarnings("unchecked")
    public ConfigTypeAdapter lookupTypeAdapter(AnnotatedConfigurableProperty property) throws ConfigurationException {

        Class clazz = property.getRawClass();

        // first check for a custom adapter
        ConfigTypeAdapter typeAdapter = customConfigTypeAdapters.get(clazz);
        if (typeAdapter != null) return typeAdapter;

        // check if it is a reference
        if (property.getAnnotation(ConfigurableProperty.class) != null && property.isReference())
            return getReferenceTypeAdapter();

        // check if it is an extensions map
        if (property.getAnnotation(ConfigurableProperty.class) != null && property.isExtensionsProperty())
            return new NullToNullDecorator(new ExtensionTypeAdaptor());

        // delegate to default otherwise
        return lookupDefaultTypeAdapter(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigTypeAdapter lookupDefaultTypeAdapter(Class clazz) throws ConfigurationException {

        ConfigTypeAdapter adapter = null;

        // if it is a config class, use reflective adapter
        if (ConfigIterators.isConfigurableClass(clazz))
            adapter = new ReflectiveAdapter();
        else if (clazz.isArray())
            adapter = arrayTypeAdapter;
        else if (clazz.isEnum())
            adapter = DefaultConfigTypeAdapters.get(Enum.class);
        else
            adapter = DefaultConfigTypeAdapters.get(clazz);

        if (adapter == null)
            throw new ConfigurationException("TypeAdapter not found for class " + clazz.getName());

        return adapter;
    }

    /**
     * Registers a custom type adapter for configurable properties for the specified class
     *
     * @param clazz
     * @param typeAdapter
     */
    @Override
    public void registerCustomConfigTypeAdapter(Class clazz, ConfigTypeAdapter typeAdapter) {
        customConfigTypeAdapters.put(clazz, typeAdapter);
    }
}
