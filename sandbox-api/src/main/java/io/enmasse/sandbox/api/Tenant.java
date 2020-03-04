package io.enmasse.sandbox.api;

public class Tenant {
    private String name;
    private long creationTimestamp = 0;
    private long provisionedTimestamp = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public long getProvisionedTimestamp() {
        return provisionedTimestamp;
    }

    public void setProvisionedTimestamp(long provisionedTimestamp) {
        this.provisionedTimestamp = provisionedTimestamp;
    }
}
