package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SandboxProvisioner {
    private static final Logger log = LoggerFactory.getLogger(SandboxProvisioner.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));

    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "io.enmasse.sandbox.maxtenants", defaultValue = "2")
    int maxTenants;

    @ConfigProperty(name = "io.enmasse.sandbox.maxtenants", defaultValue = "300s")
    Duration expirationTime;

    @Scheduled(every = "1m")
    public void processTenants() {

        // TODO: Use SandboxTenantCache instead
        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
        List<SandboxTenant> cache = op.list().getItems();

        List<SandboxTenant> tenantsByCreationTime = cache.stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = LocalDateTime.from(dateTimeFormatter.parse(a.getMetadata().getCreationTimestamp()));
                    LocalDateTime dateB = LocalDateTime.from(dateTimeFormatter.parse(b.getMetadata().getCreationTimestamp()));
                    return dateA.compareTo(dateB);
                }).collect(Collectors.toList());

        log.info("Tenants by creation time {}", tenantsByCreationTime);

        List<SandboxTenant> provisionedTenants = tenantsByCreationTime.stream()
                .filter(sandboxTenant -> sandboxTenant.getStatus() != null && sandboxTenant.getStatus().getProvisionTimestamp() != null)
                .collect(Collectors.toList());

        Iterator<SandboxTenant> unProvisionedTenants = tenantsByCreationTime.stream()
                .filter(sandboxTenant -> sandboxTenant.getStatus() == null || sandboxTenant.getStatus().getProvisionTimestamp() == null)
                .iterator();

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        int numProvisioned = provisionedTenants.size();
        while (numProvisioned < maxTenants && unProvisionedTenants.hasNext()) {
            SandboxTenant unprovisioned = unProvisionedTenants.next();
            provisionTenant(unprovisioned);

            SandboxTenantStatus status = new SandboxTenantStatus();
            status.setProvisionTimestamp(dateTimeFormatter.format(now));
            status.setExpirationTimestamp(dateTimeFormatter.format(now.plus(expirationTime)));
            unprovisioned.setStatus(status);
            op.updateStatus(unprovisioned);

            numProvisioned++;
        }

        for (SandboxTenant sandboxTenant : provisionedTenants) {
            LocalDateTime expiration = LocalDateTime.from(dateTimeFormatter.parse(sandboxTenant.getStatus().getExpirationTimestamp()));
            if (expiration.isBefore(now)) {
                log.info("Deleting tenant {}", sandboxTenant.getMetadata().getName());
                op.withName(sandboxTenant.getMetadata().getName()).delete();
            }
        }
    }

    private void provisionTenant(SandboxTenant obj) {
        log.info("Creating resources for tenant {}", obj.getMetadata().getName());

        kubernetesClient.namespaces().createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(obj.getMetadata().getName())
                .addNewOwnerReference()
                .withApiVersion(obj.getApiVersion())
                .withKind(obj.getKind())
                .withUid(obj.getMetadata().getUid())
                .withController(true)
                .withName(obj.getMetadata().getName())
                .endOwnerReference()
                .endMetadata()
                .done();

        String roleName = "sandbox-tenant";
        kubernetesClient.rbac().roleBindings().inNamespace(obj.getMetadata().getName()).withName(roleName).createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(roleName)
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
}
