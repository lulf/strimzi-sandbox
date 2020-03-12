package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.CustomResources;
import io.enmasse.sandbox.model.DoneableSandboxTenant;
import io.enmasse.sandbox.model.SandboxTenant;
import io.enmasse.sandbox.model.SandboxTenantList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SandboxTenantCache implements ResourceEventHandler<SandboxTenant> {
    private static final Logger log = LoggerFactory.getLogger(SandboxTenantCache.class);

    private final Map<String, SandboxTenant> cache = new ConcurrentHashMap<>();

    @Override
    public void onAdd(SandboxTenant obj) {
        cache.put(obj.getMetadata().getName(), obj);
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
}
