/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class SandboxOperator {
    private static final Logger log = LoggerFactory.getLogger(SandboxOperator.class);
    private final SandboxTenantProvisioner tenantProvisioner;
    private final KafkaTopicProvisioner topicProvisioner;
    private final SharedInformerFactoryProvider sharedInformerFactoryProvider;

    public SandboxOperator(SandboxTenantProvisioner tenantProvisioner, KafkaTopicProvisioner topicProvisioner, SharedInformerFactoryProvider sharedInformerFactoryProvider) {
        this.tenantProvisioner = tenantProvisioner;
        this.topicProvisioner = topicProvisioner;
        this.sharedInformerFactoryProvider = sharedInformerFactoryProvider;
    }

    public void onStart(@Observes StartupEvent event) {
        log.info("Starting informers");
        this.sharedInformerFactoryProvider.getSharedInformerFactory().startAllRegisteredInformers();
    }
}
