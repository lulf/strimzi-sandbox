package io.enmasse.sandbox;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/tenants")
@Authenticated
public class TenantResource {
    @Inject
    SecurityIdentity identity;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(Tenant tenant) {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tenant> list() {
    }
}
