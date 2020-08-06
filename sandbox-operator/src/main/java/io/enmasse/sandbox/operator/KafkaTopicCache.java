/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.CustomResources;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.api.kafka.KafkaTopicList;
import io.strimzi.api.kafka.model.DoneableKafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopic;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class KafkaTopicCache {
    private final SyncedCache<KafkaTopic> cache;

    public KafkaTopicCache(KubernetesClient kubernetesClient, SharedInformerFactoryProvider sharedInformerFactoryProvider) {
        this.cache = new SyncedCache<>(sharedInformerFactoryProvider.getSharedInformerFactory().sharedIndexInformerForCustomResource(CustomResources.getKafkaTopicCrdContext(), KafkaTopic.class, KafkaTopicList.class, TimeUnit.MINUTES.toMillis(1)),
                kubernetesClient.customResources(CustomResources.getKafkaTopicCrdContext(), KafkaTopic.class, KafkaTopicList.class, DoneableKafkaTopic.class).inAnyNamespace().list().getItems());
    }

    public void registerListener(SyncedCache.Listener listener) {
        this.cache.registerListener(listener);
    }

    public List<KafkaTopic> list() {
        return cache.list();
    }
}
