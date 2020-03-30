/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SandboxTenantStatus {
    private String provisionTimestamp;
    private String expirationTimestamp;
    private String consoleUrl;
    private String messagingUrl;
    private String messagingWssUrl;
    private String namespace;

    public String getProvisionTimestamp() {
        return provisionTimestamp;
    }

    public void setProvisionTimestamp(String provisionTimestamp) {
        this.provisionTimestamp = provisionTimestamp;
    }

    public String getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public void setExpirationTimestamp(String expirationTimestamp) {
        this.expirationTimestamp = expirationTimestamp;
    }

    public String getConsoleUrl() {
        return consoleUrl;
    }

    public void setConsoleUrl(String consoleUrl) {
        this.consoleUrl = consoleUrl;
    }

    public String getMessagingUrl() {
        return messagingUrl;
    }

    public void setMessagingUrl(String messagingUrl) {
        this.messagingUrl = messagingUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        return "SandboxTenantStatus{" +
                "provisionTimestamp='" + provisionTimestamp + '\'' +
                ", expirationTimestamp='" + expirationTimestamp + '\'' +
                ", consoleUrl='" + consoleUrl + '\'' +
                ", messagingUrl='" + messagingUrl + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }

    public String getMessagingWssUrl() {
        return messagingWssUrl;
    }

    public void setMessagingWssUrl(String messagingWssUrl) {
        this.messagingWssUrl = messagingWssUrl;
    }
}
