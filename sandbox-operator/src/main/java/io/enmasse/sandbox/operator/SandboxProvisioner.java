/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import com.google.common.hash.Hashing;
import io.enmasse.sandbox.model.CustomResources;
import io.enmasse.sandbox.model.DoneableSandboxTenant;
import io.enmasse.sandbox.model.SandboxTenant;
import io.enmasse.sandbox.model.SandboxTenantList;
import io.enmasse.sandbox.model.SandboxTenantStatus;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.scheduler.Scheduled;
import io.strimzi.api.kafka.KafkaTopicList;
import io.strimzi.api.kafka.KafkaUserList;
import io.strimzi.api.kafka.model.DoneableKafkaTopic;
import io.strimzi.api.kafka.model.DoneableKafkaUser;
import io.strimzi.api.kafka.model.KafkaTopic;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.KafkaUserTlsClientAuthenticationBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SandboxProvisioner {
    private static final Logger log = LoggerFactory.getLogger(SandboxProvisioner.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "enmasse.sandbox.strimzi-infra", defaultValue = "strimzi-infra")
    String strimziInfra;

    @ConfigProperty(name = "enmasse.sandbox.maxtenants", defaultValue = "20")
    int maxTenants;

    @ConfigProperty(name = "enmasse.sandbox.expiration-time", defaultValue = "3h")
    Duration expirationTime;

    @Gauge(name = "tenants_total", unit = MetricUnits.NONE, description = "Number of sandbox tenants registered.")
    public Long getNumTenants() {
        return (long) currentTenants.size();
    }

    @Gauge(name = "tenants_provisioned_total", unit = MetricUnits.NONE, description = "Number of currently provisioned sandbox tenants.")
    public Long getNumProvisionedTenants() {
        return (long) (int) currentTenants.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().getProvisionTimestamp() != null && t.getStatus().getExpirationTimestamp() != null)
                .count();
    }

    @Gauge(name = "tenants_provisioning_latency_seconds", unit = MetricUnits.SECONDS, description = "Average time from creation of tenant until it gets provisioned.")
    public Double getAverageProvisioningTime() {
        return currentTenants.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().getProvisionTimestamp() != null)
                .mapToLong(t -> {
                    LocalDateTime creation = LocalDateTime.from(dateTimeFormatter.parse(t.getMetadata().getCreationTimestamp()));
                    LocalDateTime provisioning = LocalDateTime.from(dateTimeFormatter.parse(t.getStatus().getProvisionTimestamp()));
                    return Duration.between(creation, provisioning).getSeconds();
                })
                .average()
                .orElse(0.0);
    }

    private volatile List<SandboxTenant> currentTenants = new ArrayList<>();

    @Scheduled(every = "1m")
    public void refreshTenants() {
        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
        currentTenants = new ArrayList<>(op.inAnyNamespace().list().getItems());
    }

    @Scheduled(every = "30s")
    public synchronized void processTenants() {

        // Order tenants by creation time so that we provision the oldest one first
        List<SandboxTenant> tenantsByCreationTime = currentTenants.stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = LocalDateTime.from(dateTimeFormatter.parse(a.getMetadata().getCreationTimestamp()));
                    LocalDateTime dateB = LocalDateTime.from(dateTimeFormatter.parse(b.getMetadata().getCreationTimestamp()));
                    return dateA.compareTo(dateB);
                }).collect(Collectors.toList());

        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
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
            status.setBootstrap("bootstrap.strimzi-sandbox.enmasse.io:443");
            status.setBrokers(Collections.singletonList("broker-0.strimzi-sandbox.enmasse.io:443"));
            unprovisioned.setStatus(status);
            op.updateStatus(unprovisioned);

            /*
            MixedOperation<KafkaUser, KafkaUserList, DoneableKafkaUser, Resource<KafkaUser, DoneableKafkaUser>> userOp = kubernetesClient.customResources(CustomResources.getKafkaUserCrd(), KafkaUser.class, KafkaUserList.class, DoneableKafkaUser.class);
            userOp.inNamespace(strimziInfra).createNew()
                    .editOrNewMetadata()
                    .withName(ns)
                    .withNamespace(strimziInfra)
                    .endMetadata()
                    .editOrNewSpec()
                    .withAutho
                    .withNewKafkaUserScramSha512ClientAuthentication()
                    .endKafkaUserScramSha512ClientAuthentication()
                    .buildAuthentication()
                    .wi
                    .withAuthentication(new KafkaUserAuthentication() {
                        @Override
                        public String getType() {
                            return null;
                        }
                    }
                    .endSpec()
                    .done();
            List<KafkaUser> infraTopics = op.inNamespace().list().getItems();
             */

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
            }
        }
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

        kubernetesClient.resourceQuotas().inNamespace(namespace).withName("topic-quota").createOrReplaceWithNew()
                .editOrNewMetadata()
                .withName("topic-quota")
                .withNamespace(namespace)
                .endMetadata()
                .editOrNewSpec()
                .addToHard("count/kafkatopics.kafka.strimzi.io", new Quantity("2"))
                .endSpec()
                .done();
    }
}
