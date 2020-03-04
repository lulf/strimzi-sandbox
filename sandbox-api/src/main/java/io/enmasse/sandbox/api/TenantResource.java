package io.enmasse.sandbox.api;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/api/tenants")
public class TenantResource {
    @Inject
    SecurityIdentity identity;

    @Inject
    KubernetesClient kubernetesClient;

    private final CustomResourceDefinition crd = new CustomResourceDefinitionBuilder()
            .editOrNewMetadata()
            .withName("sandboxtenant")
            .endMetadata()
            .build();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(Tenant tenant) {
//        kubernetesClient.customResources(crd, u )
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tenant> list() {
        return Collections.emptyList();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{name}")
    public Tenant get(@PathParam("name") String name) {
        if (!name.equals(identity.getPrincipal().getName())) {
            throw new UnauthorizedException("Unknown tenant " + name);
        }

        return null;
    }
}
