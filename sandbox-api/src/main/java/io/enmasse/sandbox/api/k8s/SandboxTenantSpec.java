package io.enmasse.sandbox.api.k8s;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
        )
)
public class SandboxTenantSpec {
    private long provisionedTimestamp = 0;

    public long getProvisionedTimestamp() {
        return provisionedTimestamp;
    }

    public void setProvisionedTimestamp(long provisionedTimestamp) {
        this.provisionedTimestamp = provisionedTimestamp;
    }
}
