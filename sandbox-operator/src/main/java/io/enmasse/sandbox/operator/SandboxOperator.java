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
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class SandboxOperator implements ResourceEventHandler<SandboxTenant> {

    private static final Logger log = LoggerFactory.getLogger(SandboxOperator.class);

    @Inject
    private KubernetesClient kubernetesClient;

    void onStartup(@Observes StartupEvent ev) {
        SharedIndexInformer<SandboxTenant> informer = kubernetesClient.informers().sharedIndexInformerForCustomResource(CustomResources.getSandboxCrdContext(), SandboxTenant.class, SandboxTenantList.class, TimeUnit.MINUTES.toMillis(5));
        informer.addEventHandler(this);
        kubernetesClient.informers().startAllRegisteredInformers();
    }

    @Override
    public void onAdd(SandboxTenant obj) {
        log.info("Creating resources for tenant {}", obj.getMetadata().getName());
        kubernetesClient.namespaces().createNew()
                .editOrNewMetadata()
                .withName(obj.getMetadata().getName())
                .addNewOwnerReference()
                .withApiVersion(obj.getApiVersion())
                .withKind(obj.getKind())
                .withController(true)
                .withName(obj.getMetadata().getName())
                .endOwnerReference()
                .endMetadata()
                .done();
        kubernetesClient.rbac().roleBindings().createNew()
                .editOrNewMetadata()
                .withName("sandbox-tenant")
                .withNamespace(obj.getMetadata().getName())
                .endMetadata()
                .editOrNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName("sandbox-tenant")
                .endRoleRef()
                .addNewSubject()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("User")
                .withName(obj.getSpec().getSubject())
                .withNamespace(obj.getMetadata().getName())
                .endSubject()
                .done();
    }

    @Override
    public void onUpdate(SandboxTenant oldObj, SandboxTenant newObj) {
        log.info("Updating resources for tenant {}", oldObj.getMetadata().getName());
    }

    @Override
    public void onDelete(SandboxTenant obj, boolean deletedFinalStateUnknown) {
        log.info("Deleting resources for tenant {}", obj.getMetadata().getName());
    }
}
