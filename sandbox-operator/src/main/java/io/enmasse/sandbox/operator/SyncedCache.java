/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncedCache<T extends HasMetadata> implements ResourceEventHandler<T> {
    private static final Logger log = LoggerFactory.getLogger(SyncedCache.class);

    private final Map<ObjectKey, T> cache = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new ArrayList<>();

    private final SharedIndexInformer<T> informer;

    SyncedCache(SharedIndexInformer<T> informer, List<T> initialList) {
        this.informer = informer;
        informer.addEventHandler(this);
        for (T entry : initialList) {
            cache.put(ObjectKey.fromMetadata(entry), entry);
        }
    }

    public void registerListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void onAdd(T obj) {
        log.debug("Adding cache entry for object {}", obj.getMetadata().getName());
        cache.put(ObjectKey.fromMetadata(obj), obj);
        notifyListeners();
    }

    private void notifyListeners() {
        if (informer.hasSynced()) {
            synchronized (listeners) {
                for (Listener listener : listeners) {
                    listener.cacheChanged();
                }
            }
        }
    }


    @Override
    public void onUpdate(T oldObj, T newObj) {
        log.debug("Updating cache entry for object {}", oldObj.getMetadata().getName());
        cache.put(ObjectKey.fromMetadata(newObj), newObj);
        notifyListeners();
    }

    @Override
    public void onDelete(T obj, boolean deletedFinalStateUnknown) {
        log.debug("Deleting cache entry for object {}", obj.getMetadata().getName());
        cache.remove(ObjectKey.fromMetadata(obj));
        notifyListeners();
    }

    public List<T> list() {
        synchronized (cache) {
            return new ArrayList<>(cache.values());
        }
    }

    interface Listener {
        void cacheChanged();
    }
}
