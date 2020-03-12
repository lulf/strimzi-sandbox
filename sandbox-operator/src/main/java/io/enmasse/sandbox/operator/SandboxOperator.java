package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.CustomResources;
import io.enmasse.sandbox.model.DoneableSandboxTenant;
import io.enmasse.sandbox.model.SandboxTenant;
import io.enmasse.sandbox.model.SandboxTenantList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SandboxOperator {
    private static final Logger log = LoggerFactory.getLogger(SandboxOperator.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    SandboxProvisioner sandboxProvisioner;

    private volatile Watch watch = null;

    @Scheduled(every = "10s")
    synchronized void checkWatcher() {
        if (watch == null) {
            watch = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class)
                    .inAnyNamespace()
                    .watch(new Watcher<SandboxTenant>() {
                        @Override
                        public void eventReceived(Action action, SandboxTenant resource) {
                            switch (action) {
                                case ADDED:
                                    sandboxProvisioner.processTenants();
                                    break;
                                case DELETED:
                                case MODIFIED:
                                    break;
                                case ERROR:
                                    log.error("Error event in watch");
                                    break;
                            }
                        }

                        @Override
                        public void onClose(KubernetesClientException cause) {
                            if (cause != null) {
                                log.warn("Watch closed with error", cause);
                            } else {
                                log.info("Watch closed");
                            }
                            watch = null;
                        }
                    });
        }
    }

    /*
    TODO: Cannot use informers due to bug in kubernetes-client
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
