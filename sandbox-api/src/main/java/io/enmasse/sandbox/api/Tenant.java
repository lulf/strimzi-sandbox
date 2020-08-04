/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.api;

import java.util.List;

public class Tenant {
    private String name;
    private String subject;
    private String creationTimestamp;
    private String provisionTimestamp;
    private String expirationTimestamp;
    private String namespace;
    private Integer placeInQueue;
    private String estimatedProvisionTime;
    private String bootstrap;
    private List<String> brokers;

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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getEstimatedProvisionTime() {
        return estimatedProvisionTime;
    }

    public void setEstimatedProvisionTime(String estimatedProvisionTime) {
        this.estimatedProvisionTime = estimatedProvisionTime;
    }

    public Integer getPlaceInQueue() {
        return placeInQueue;
    }

    public void setPlaceInQueue(Integer placeInQueue) {
        this.placeInQueue = placeInQueue;
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
    }

    public List<String> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<String> brokers) {
        this.brokers = brokers;
    }
}
