/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.SandboxTenant;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SandboxTenantCache implements ResourceEventHandler<SandboxTenant> {
    private static final Logger log = LoggerFactory.getLogger(SandboxTenantCache.class);

    private final Map<String, SandboxTenant> cache = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new ArrayList<>();

    public void registerListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void onAdd(SandboxTenant obj) {
        log.info("Adding cache entry for tenant {}", obj.getMetadata().getName());
        cache.put(obj.getMetadata().getName(), obj);
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.tenantAdded();
            }
        }
    }


    @Override
    public void onUpdate(SandboxTenant oldObj, SandboxTenant newObj) {
        log.info("Updating cache entry for tenant {}", oldObj.getMetadata().getName());
        cache.put(newObj.getMetadata().getName(), newObj);
    }

    @Override
    public void onDelete(SandboxTenant obj, boolean deletedFinalStateUnknown) {
        log.info("Deleting cache entry for tenant {}", obj.getMetadata().getName());
        cache.remove(obj.getMetadata().getName());
    }

    public List<SandboxTenant> list() {
        synchronized (cache) {
            return new ArrayList<>(cache.values());
        }
    }

    interface Listener {
        void tenantAdded();
    }
}
