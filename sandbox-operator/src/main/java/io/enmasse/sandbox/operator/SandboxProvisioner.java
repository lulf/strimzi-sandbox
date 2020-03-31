/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import com.google.common.hash.Hashing;
import io.enmasse.sandbox.model.*;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.scheduler.Scheduled;
import okhttp3.Address;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SandboxProvisioner {
    private static final Logger log = LoggerFactory.getLogger(SandboxProvisioner.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));
    private static final String enmasseNamespace = "enmasse-infra";

    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "enmasse.sandbox.maxtenants", defaultValue = "3")
    int maxTenants;

    @ConfigProperty(name = "enmasse.sandbox.expiration-time", defaultValue = "3h")
    Duration expirationTime;

    @Scheduled(every = "1m")
    public synchronized void processTenants() {

        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);

        // Order tenants by creation time so that we provision the oldest one first
        List<SandboxTenant> tenantsByCreationTime = op.inAnyNamespace().list().getItems().stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = LocalDateTime.from(dateTimeFormatter.parse(a.getMetadata().getCreationTimestamp()));
                    LocalDateTime dateB = LocalDateTime.from(dateTimeFormatter.parse(b.getMetadata().getCreationTimestamp()));
                    return dateA.compareTo(dateB);
                }).collect(Collectors.toList());

        log.info("Tenants by creation time {}", tenantsByCreationTime.stream().map(t -> String.format("%s:%s", t.getMetadata().getName(), t.getMetadata().getCreationTimestamp())).collect(Collectors.toList()));

        // Those already provisioned will be garbage-collected if they have expired
        List<SandboxTenant> provisionedTenants = tenantsByCreationTime.stream()
                .filter(sandboxTenant -> sandboxTenant.getStatus() != null && sandboxTenant.getStatus().getProvisionTimestamp() != null)
                .collect(Collectors.toList());

        Iterator<SandboxTenant> unProvisionedTenants = tenantsByCreationTime.stream()
                .filter(sandboxTenant -> sandboxTenant.getStatus() == null || sandboxTenant.getStatus().getProvisionTimestamp() == null)
                .iterator();

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        int numProvisioned = provisionedTenants.size();

        // Provision new tenants as long as we have the capacity.
        while (numProvisioned < maxTenants && unProvisionedTenants.hasNext()) {
            SandboxTenant unprovisioned = unProvisionedTenants.next();
            String ns = getNamespace(unprovisioned);
            provisionTenant(unprovisioned, ns);

            // Update tenant status with information
            SandboxTenantStatus status = new SandboxTenantStatus();
            status.setProvisionTimestamp(dateTimeFormatter.format(now));
            status.setExpirationTimestamp(dateTimeFormatter.format(now.plus(expirationTime)));
            status.setNamespace(ns);
            unprovisioned.setStatus(status);
            op.updateStatus(unprovisioned);

            numProvisioned++;
        }

        // Process tenants
        for (SandboxTenant sandboxTenant : provisionedTenants) {
            LocalDateTime expiration = LocalDateTime.from(dateTimeFormatter.parse(sandboxTenant.getStatus().getExpirationTimestamp()));
            String ns = getNamespace(sandboxTenant);
            // Garbage collect expired tenants
            if (expiration.isBefore(now)) {
                log.info("Deleting tenant {}", sandboxTenant.getMetadata().getName());
                op.withName(sandboxTenant.getMetadata().getName()).cascading(true).delete();
            } else {
                MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>> spaceOp =
                        kubernetesClient.customResources(CustomResources.getAddressSpaceCrd(), AddressSpace.class, AddressSpaceList.class, DoneableAddressSpace.class);
                for (AddressSpace addressSpace : spaceOp.inNamespace(ns).list().getItems()) {
                    if (addressSpace.getMetadata().getAnnotations() != null) {
                        String infraUuid = addressSpace.getMetadata().getAnnotations().get("enmasse.io/infra-uuid");
                        if (infraUuid != null) {
                            log.info("Creating ingresses for tenant {}", sandboxTenant.getMetadata().getName());
                            String messagingHost = String.format("%s.messaging.sandbox.enmasse.io", ns);
                            createEndpoint(ns, infraUuid, messagingHost, 5671);

                            String messagingWssHost = String.format("%s.messaging-wss.sandbox.enmasse.io", ns);
                            createEndpoint(ns, infraUuid, messagingWssHost, 443);

                            if (sandboxTenant.getStatus() != null) {
                                boolean changed = false;
                                if (sandboxTenant.getStatus().getMessagingUrl() == null) {
                                    sandboxTenant.getStatus().setMessagingUrl(String.format("amqps://%s:443", messagingHost));
                                    changed = true;
                                }
                                if (sandboxTenant.getStatus().getMessagingWssUrl() == null) {
                                    sandboxTenant.getStatus().setMessagingWssUrl(String.format("wss://%s:443", messagingWssHost));
                                    changed = true;
                                }
                                if (changed) {
                                    op.updateStatus(sandboxTenant);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Remove ingress resources not part of current set of tenants
        String[] currentTenants = tenantsByCreationTime.stream().map(this::getNamespace).toArray(String[]::new);
        kubernetesClient.extensions().ingresses()
                .inNamespace(enmasseNamespace)
                .withLabels(Map.of("app", "sandbox.enmasse.io"))
                .withLabelNotIn("tenant", currentTenants).delete();
    }

    private void createEndpoint(String ns, String infraUuid, String host, int port) {
        kubernetesClient.extensions().ingresses().inNamespace(enmasseNamespace).createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(String.format("%s-%d", ns, port))
                .addToAnnotations("nginx.ingress.kubernetes.io/ssl-passthrough", "true")
                .addToLabels("app", "sandbox.enmasse.io")
                .addToLabels("tenant", ns)
                .endMetadata()
                .editOrNewSpec()
                .addNewRule()
                .withHost(host)
                .withNewHttp()
                .addNewPath()
                .editOrNewBackend()
                .withServiceName(String.format("messaging-%s", infraUuid))
                .withServicePort(new IntOrString(port))
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .addNewTl()
                .withHosts(host)
                .endTl()
                .endSpec()
                .done();

    }

    private String getNamespace(SandboxTenant obj) {
        return "tenant-" + Hashing.sha256().hashString(obj.getMetadata().getName(), StandardCharsets.UTF_8).toString().substring(0, 8);
    }

    private void provisionTenant(SandboxTenant obj, String namespace) {
        log.info("Creating resources for tenant {}", obj.getMetadata().getName());


        kubernetesClient.namespaces().createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(namespace)
                .addNewOwnerReference()
                .withApiVersion(obj.getApiVersion())
                .withKind(obj.getKind())
                .withUid(obj.getMetadata().getUid())
                .withBlockOwnerDeletion(true)
                .withController(true)
                .withName(obj.getMetadata().getName())
                .endOwnerReference()
                .endMetadata()
                .done();


        String roleName = "sandbox-tenant";
        kubernetesClient.rbac().roleBindings().inNamespace(namespace).withName(roleName).createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(roleName)
                .withNamespace(namespace)
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
                .withNamespace(namespace)
                .endSubject()
                .done();

        String clusterRoleName = String.format("%s.%s", roleName, obj.getMetadata().getUid());
        kubernetesClient.rbac().clusterRoles().withName(clusterRoleName).createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(clusterRoleName)
                .addNewOwnerReference()
                .withApiVersion(obj.getApiVersion())
                .withKind(obj.getKind())
                .withUid(obj.getMetadata().getUid())
                .withController(true)
                .withBlockOwnerDeletion(true)
                .withName(obj.getMetadata().getName())
                .endOwnerReference()
                .endMetadata()
                .addNewRule()
                .withApiGroups("")
                .withResources("namespaces")
                .withResourceNames(namespace)
                .withVerbs("get")
                .endRule()
                .done();

        String clusterRoleBindingName = clusterRoleName;
        kubernetesClient.rbac().clusterRoleBindings().withName(clusterRoleBindingName).createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName(clusterRoleBindingName)
                .addNewOwnerReference()
                .withApiVersion(obj.getApiVersion())
                .withKind(obj.getKind())
                .withUid(obj.getMetadata().getUid())
                .withController(true)
                .withBlockOwnerDeletion(true)
                .withName(obj.getMetadata().getName())
                .endOwnerReference()
                .endMetadata()
                .editOrNewRoleRef()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .withName(clusterRoleName)
                .endRoleRef()
                .addNewSubject()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("User")
                .withName(obj.getSpec().getSubject())
                .withNamespace(namespace)
                .endSubject()
                .done();

        kubernetesClient.resourceQuotas().inNamespace(namespace).withName("addressspace-quota").createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName("addressspace-quota")
                .withNamespace(namespace)
                .endMetadata()
                .editOrNewSpec()
                .addToHard("count/addressspaces.enmasse.io", new Quantity("1"))
                .endSpec()
                .done();
    }
}
