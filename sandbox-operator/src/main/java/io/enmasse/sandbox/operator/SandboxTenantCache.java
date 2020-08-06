/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.CustomResources;
import io.enmasse.sandbox.model.DoneableSandboxTenant;
import io.enmasse.sandbox.model.SandboxTenant;
import io.enmasse.sandbox.model.SandboxTenantList;
import io.fabric8.kubernetes.client.KubernetesClient;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class SandboxTenantCache {
    private final SyncedCache<SandboxTenant> cache;

    public SandboxTenantCache(KubernetesClient kubernetesClient, SharedInformerFactoryProvider sharedInformerFactoryProvider) {
        this.cache = new SyncedCache<>(sharedInformerFactoryProvider.getSharedInformerFactory().sharedIndexInformerForCustomResource(CustomResources.getSandboxCrdContext(), SandboxTenant.class, SandboxTenantList.class, TimeUnit.MINUTES.toMillis(1)),
                kubernetesClient.customResources(CustomResources.getSandboxCrdContext(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class).list().getItems());
    }

    public void registerListener(SyncedCache.Listener listener) {
        this.cache.registerListener(listener);
    }

    public List<SandboxTenant> list() {
        return cache.list();
    }
}
