package io.enmasse.sandbox;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/tenants")
@Authenticated
public class TenantResource {
    @Inject
    SecurityIdentity identity;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void create(Tenant tenant) {

    }
}
