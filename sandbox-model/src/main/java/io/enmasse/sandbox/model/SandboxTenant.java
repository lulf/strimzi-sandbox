package io.enmasse.sandbox.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(ObjectMeta.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
        )
)
@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
@JsonPropertyOrder({"apiVersion", "kind", "metadata"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SandboxTenant implements HasMetadata {
    private String apiVersion = "sandbox.enmasse.io/v1beta1";
    private String kind = "SandboxTenant";
    private ObjectMeta metadata = new ObjectMeta();

    private SandboxTenantSpec spec;
    private SandboxTenantStatus status;

    public ObjectMeta getMetadata() {
        return metadata;
    }

    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getKind() {
        return kind;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setApiVersion(String version) {
        this.apiVersion = version;
    }

    public SandboxTenantSpec getSpec() {
        return spec;
    }

    public void setSpec(SandboxTenantSpec spec) {
        this.spec = spec;
    }

    public SandboxTenantStatus getStatus() {
        return status;
    }

    public void setStatus(SandboxTenantStatus status) {
        this.status = status;
    }
}
