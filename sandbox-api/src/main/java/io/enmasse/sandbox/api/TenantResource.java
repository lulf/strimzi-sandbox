/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.api;

import io.enmasse.sandbox.model.CustomResources;
import io.enmasse.sandbox.model.DoneableSandboxTenant;
import io.enmasse.sandbox.model.SandboxTenant;
import io.enmasse.sandbox.model.SandboxTenantList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/tenants")
@Authenticated
public class TenantResource {
    private static final Logger log = LoggerFactory.getLogger(TenantResource.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    @Inject
    SecurityIdentity identity;

    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "enmasse.sandbox.expiration-time", defaultValue = "3h")
    Duration expirationTime;

    @ConfigProperty(name = "enmasse.sandbox.maxtenants", defaultValue = "3")
    int maxTenants;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed(name = "tenant_create_latency_seconds", unit = MetricUnits.SECONDS, description = "A measure of how long it takes to create a tenant.")
    public void create(Tenant tenant) {
        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
        SandboxTenant sandboxTenant = op.withName(tenant.getName()).get();
        if (sandboxTenant != null) {
            throw new WebApplicationException("Tenant already exists", 409);
        }
        log.info("Creating tenant {}", tenant.getName());
        op.withName(tenant.getName()).createNew()
                .editOrNewMetadata()
                .withName(tenant.getName())
                .endMetadata()
                .editOrNewSpec()
                .withSubject(tenant.getSubject())
                .endSpec()
                .done();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{name}")
    @Timed(name = "tenant_get_latency_seconds", unit = MetricUnits.SECONDS, description = "A measure of how long it takes to get a tenant.")
    public Tenant get(@PathParam("name") String name) {
        if (!name.equals(identity.getPrincipal().getName())) {
            throw new UnauthorizedException("Unknown tenant " + name);
        }

        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
        List<SandboxTenant> tenants = op.list().getItems();
        SandboxTenant sandboxTenant = tenants.stream()
                .filter(t -> t.getMetadata().getName().equals(name))
                .findAny()
                .orElse(null);
        if (sandboxTenant == null) {
            throw new NotFoundException("Unknown tenant " + name);
        }

        Tenant tenant = new Tenant();
        tenant.setName(sandboxTenant.getMetadata().getName());
        tenant.setSubject(sandboxTenant.getSpec().getSubject());
        tenant.setCreationTimestamp(sandboxTenant.getMetadata().getCreationTimestamp());
        if (sandboxTenant.getStatus() != null) {
            if (sandboxTenant.getStatus().getProvisionTimestamp() != null) {
                tenant.setProvisionTimestamp(sandboxTenant.getStatus().getProvisionTimestamp());
            }
            if (sandboxTenant.getStatus().getExpirationTimestamp() != null) {
                tenant.setExpirationTimestamp(sandboxTenant.getStatus().getExpirationTimestamp());
            }
            if (sandboxTenant.getStatus().getNamespace() != null) {
                tenant.setNamespace(sandboxTenant.getStatus().getNamespace());
            }
            if (sandboxTenant.getStatus().getBootstrap() != null) {
                tenant.setBootstrap(sandboxTenant.getStatus().getBootstrap());
            }
            if (sandboxTenant.getStatus().getBrokers() != null) {
                tenant.setBrokers(new ArrayList<>(sandboxTenant.getStatus().getBrokers()));
            }
        } else {
            setEstimates(tenant, tenants);
        }
        return tenant;
    }

    private void setEstimates(Tenant tenant, List<SandboxTenant> tenants) {
        List<SandboxTenant> tenantsByExpiration = tenants.stream()
                .filter(sandboxTenant -> sandboxTenant.getStatus() != null && sandboxTenant.getStatus().getExpirationTimestamp() != null)
                .sorted((a, b) -> {
                    LocalDateTime dateA = LocalDateTime.from(dateTimeFormatter.parse(a.getStatus().getExpirationTimestamp()));
                    LocalDateTime dateB = LocalDateTime.from(dateTimeFormatter.parse(b.getStatus().getExpirationTimestamp()));
                    return dateA.compareTo(dateB);
                }).collect(Collectors.toList());

        // Locate starting point - either now - or the last expiring tenant.
        LocalDateTime start = LocalDateTime.now(ZoneId.of("UTC"));
        int placeInQueue = 1;
        if (tenantsByExpiration.size() >= maxTenants) {
            if (!tenantsByExpiration.isEmpty()) {
                SandboxTenant lastExpiringTenant = tenantsByExpiration.get(tenantsByExpiration.size() - 1);
                start = LocalDateTime.from(dateTimeFormatter.parse(lastExpiringTenant.getStatus().getExpirationTimestamp()));
            }

            // Iterate over everone before us in the queue and increment estimate
            List<SandboxTenant> unprovisionedByCreationTime = tenants.stream()
                    .filter(sandboxTenant -> sandboxTenant.getStatus() == null || sandboxTenant.getStatus().getProvisionTimestamp() == null)
                    .sorted((a, b) -> {
                        LocalDateTime dateA = LocalDateTime.from(dateTimeFormatter.parse(a.getMetadata().getCreationTimestamp()));
                        LocalDateTime dateB = LocalDateTime.from(dateTimeFormatter.parse(b.getMetadata().getCreationTimestamp()));
                        return dateA.compareTo(dateB);
                    }).collect(Collectors.toList());

            for (SandboxTenant unprovisioned : unprovisionedByCreationTime) {
                if (unprovisioned.getMetadata().getName().equals(tenant.getName())) {
                    break;
                }
                start.plus(expirationTime);
                placeInQueue++;
            }
        }

        tenant.setPlaceInQueue(placeInQueue);
        tenant.setEstimatedProvisionTime(dateTimeFormatter.format(start));
    }

    @ConfigProperty(name = "keycloak.realm.admin.user")
    String adminUser;

    @ConfigProperty(name = "keycloak.realm.admin.password")
    String adminPassword;

    @ConfigProperty(name = "keycloak.url", defaultValue = "https://auth.kafka.lulf.no/auth")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.realm", defaultValue = "k8s")
    String keycloakRealm;

    private Keycloak keycloak;

    private synchronized Keycloak getInstance() {
        if (keycloak == null) {
            keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakUrl)
                    .realm(keycloakRealm)
                    .username(adminUser)
                    .password(adminPassword)
                    .clientId("admin-cli")
                    .build();
        }
        return keycloak;
    }


    @DELETE
    @Path("{name}/unlink")
    public void deleteUser(@PathParam("name") String name) {
        log.info("Unlink user request {} {}", name, identity.getPrincipal().getName());
        if (!name.equals(identity.getPrincipal().getName())) {
            throw new UnauthorizedException("Unknown tenant " + name);
        }

        Keycloak keycloak = getInstance();

        log.info("Listing users...");
        try {
            List<UserRepresentation> users = keycloak.realm("k8s").users().search(name);
            log.info("Found {} users", users.size());
            for (UserRepresentation userRepresentation : users) {
                keycloak.realm("k8s").users().delete(userRepresentation.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @DELETE
    @Path("{name}")
    public void unregister(@PathParam("name") String name) {
        if (!name.equals(identity.getPrincipal().getName())) {
            throw new UnauthorizedException("Unknown tenant " + name);
        }

        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
        SandboxTenant sandboxTenant = op.withName(name).get();
        if (sandboxTenant == null) {
            throw new NotFoundException("Unknown tenant " + name);
        }
        if (!op.withName(name).cascading(true).delete()) {
            throw new InternalServerErrorException("Error deleting tenant");
        }
        log.info("Deleted tenant {}", name);
    }
}
