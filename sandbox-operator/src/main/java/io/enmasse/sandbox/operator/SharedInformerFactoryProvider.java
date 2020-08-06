/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SharedInformerFactoryProvider {
    private final SharedInformerFactory sharedInformerFactory;

    SharedInformerFactoryProvider(KubernetesClient kubernetesClient) {
        sharedInformerFactory = kubernetesClient.informers();
    }

    public SharedInformerFactory getSharedInformerFactory() {
        return sharedInformerFactory;
    }
}
