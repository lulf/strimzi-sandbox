package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.CustomResources;
import io.enmasse.sandbox.model.SandboxTenant;
import io.enmasse.sandbox.model.SandboxTenantList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@ApplicationScoped
public class SandboxOperator {
    private static final Logger log = LoggerFactory.getLogger(SandboxOperator.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    SandboxTenantCache tenantCache;

    /*
    TODO: Look into why the informers are not running/watching
    void onStartup(@Observes StartupEvent ev) {
        log.info("Startup operator!");
        SharedIndexInformer<SandboxTenant> informer = kubernetesClient.informers().sharedIndexInformerForCustomResource(CustomResources.getSandboxCrdContext(), SandboxTenant.class, SandboxTenantList.class, TimeUnit.MINUTES.toMillis(1));
        informer.addIndexers(Collections.singletonMap("foo", new Function<SandboxTenant, List<String>>() {
            @Override
            public List<String> apply(SandboxTenant sandboxTenant) {
                log.info("Indexer apply");
                return Collections.singletonList(sandboxTenant.getMetadata().getName());
            }
        }));
        informer.addEventHandler(new ResourceEventHandler<SandboxTenant>() {
            @Override
            public void onAdd(SandboxTenant obj) {
                log.info("onAdd");
            }

            @Override
            public void onUpdate(SandboxTenant oldObj, SandboxTenant newObj) {
                log.info("onUpdate");
            }

            @Override
            public void onDelete(SandboxTenant obj, boolean deletedFinalStateUnknown) {
                log.info("onDelete");
            }
        });
        informer.addEventHandler(tenantCache);
        kubernetesClient.informers().startAllRegisteredInformers();
    }
    */
}
