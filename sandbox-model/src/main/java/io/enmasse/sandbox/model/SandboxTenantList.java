package io.enmasse.sandbox.api.k8s;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ListMeta;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
@JsonPropertyOrder({"apiVersion", "kind", "metadata"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SandboxTenantList implements KubernetesResource, KubernetesResourceList<SandboxTenant> {

    private String apiVersion = "sandbox.enmasse.io/v1beta1";
    private String kind = "SandboxTenantList";
    private ListMeta metadata;
    private List<SandboxTenant> items = new ArrayList<>();

    @Override
    public ListMeta getMetadata() {
        return metadata;
    }

    @Override
    public List<SandboxTenant> getItems() {
        return items;
    }

    public void setMetadata(ListMeta metadata) {
        this.metadata = metadata;
    }

    public void setItems(List<SandboxTenant> items) {
        this.items = items;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
