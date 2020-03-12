package io.enmasse.sandbox.api;

import io.enmasse.sandbox.model.CustomResources;
import io.enmasse.sandbox.model.DoneableSandboxTenant;
import io.enmasse.sandbox.model.SandboxTenant;
import io.enmasse.sandbox.model.SandboxTenantList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/api/tenants")
@Authenticated
public class TenantResource {
    @Inject
    SecurityIdentity identity;

    @Inject
    KubernetesClient kubernetesClient;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(Tenant tenant) {
        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
        SandboxTenant sandboxTenant = op.withName(tenant.getName()).get();
        if (sandboxTenant != null) {
            throw new WebApplicationException("Tenant already exists", 409);
        }
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
    public Tenant get(@PathParam("name") String name) {
        if (!name.equals(identity.getPrincipal().getName())) {
            throw new UnauthorizedException("Unknown tenant " + name);
        }

        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
        SandboxTenant sandboxTenant = op.withName(name).get();
        if (sandboxTenant == null) {
            throw new NotFoundException("Unknown tenant " + name);
        }
        Tenant tenant = new Tenant();
        tenant.setName(sandboxTenant.getMetadata().getName());
        tenant.setSubject(sandboxTenant.getSpec().getSubject());
        tenant.setCreationTimestamp(sandboxTenant.getMetadata().getCreationTimestamp());
        if (sandboxTenant.getStatus() != null && sandboxTenant.getStatus().getProvisionTimestamp() != null) {
            tenant.setProvisionTimestamp(sandboxTenant.getStatus().getProvisionTimestamp());
        }
        return tenant;
    }

    @DELETE
    @Path("{name}")
    public void delete(@PathParam("name") String name) {
        if (!name.equals(identity.getPrincipal().getName())) {
            throw new UnauthorizedException("Unknown tenant " + name);
        }

        MixedOperation<SandboxTenant, SandboxTenantList, DoneableSandboxTenant, Resource<SandboxTenant, DoneableSandboxTenant>> op = kubernetesClient.customResources(CustomResources.getSandboxCrd(), SandboxTenant.class, SandboxTenantList.class, DoneableSandboxTenant.class);
        SandboxTenant sandboxTenant = op.withName(name).get();
        if (sandboxTenant == null) {
            throw new NotFoundException("Unknown tenant " + name);
        }
        if (!op.withName(name).delete()) {
            throw new InternalServerErrorException("Error deleting tenant");
        }
    }
}
