package io.enmasse.sandbox.api;

public class Tenant {
    private String name;
    private String subject;
    private String creationTimestamp;
    private String provisionTimestamp;
    private String expirationTimestamp;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getProvisionTimestamp() {
        return provisionTimestamp;
    }

    public void setProvisionTimestamp(String provisionTimestamp) {
        this.provisionTimestamp = provisionTimestamp;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public void setExpirationTimestamp(String expirationTimestamp) {
        this.expirationTimestamp = expirationTimestamp;
    }
}
