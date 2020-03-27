package io.enmasse.sandbox.model;

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
public class AddressSpaceList implements KubernetesResource, KubernetesResourceList<AddressSpace> {

    private String apiVersion = "enmasse.io/v1beta1";
    private String kind = "AddressSpaceList";
    private ListMeta metadata;
    private List<AddressSpace> items = new ArrayList<>();

    @Override
    public ListMeta getMetadata() {
        return metadata;
    }

    @Override
    public List<AddressSpace> getItems() {
        return items;
    }

    public void setMetadata(ListMeta metadata) {
        this.metadata = metadata;
    }

    public void setItems(List<AddressSpace> items) {
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
