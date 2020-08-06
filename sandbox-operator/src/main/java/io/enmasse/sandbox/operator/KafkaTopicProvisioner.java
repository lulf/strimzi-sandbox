/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.CustomResources;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.KafkaTopicList;
import io.strimzi.api.kafka.model.DoneableKafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopicBuilder;
import io.strimzi.api.kafka.model.KafkaTopicSpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class KafkaTopicProvisioner implements SyncedCache.Listener {
    private static final Logger log = LoggerFactory.getLogger(KafkaTopicProvisioner.class);

    @ConfigProperty(name = "enmasse.sandbox.strimzi-infra", defaultValue = "strimzi-infra")
    String strimziInfra;

    @ConfigProperty(name = "enmasse.sandbox.kafka-cluster", defaultValue = "sandbox")
    String kafkaCluster;

    private final KubernetesClient kubernetesClient;
    private final KafkaTopicCache topicCache;
    private volatile List<KafkaTopic> currentTopics = new ArrayList<>();

    KafkaTopicProvisioner(KubernetesClient kubernetesClient, KafkaTopicCache topicCache) {
        this.kubernetesClient = kubernetesClient;
        this.topicCache = topicCache;
        log.info("Registering topic cache listener");
        topicCache.registerListener(this);
        log.info("Registering topic cache listener done");
    }

    @Override
    public synchronized void cacheChanged() {
        currentTopics = topicCache.list();
        List<KafkaTopic> topics = currentTopics.stream()
                .filter(t -> !strimziInfra.equals(t.getMetadata().getNamespace()))
                .collect(Collectors.toList());

        List<KafkaTopic> infraTopics = currentTopics.stream()
                .filter(t -> strimziInfra.equals(t.getMetadata().getNamespace()))
                .collect(Collectors.toList());

        MixedOperation<KafkaTopic, KafkaTopicList, DoneableKafkaTopic, Resource<KafkaTopic, DoneableKafkaTopic>> op = kubernetesClient.customResources(CustomResources.getKafkaTopicCrdContext(), KafkaTopic.class, KafkaTopicList.class, DoneableKafkaTopic.class);
        log.info("Syncing topics. Have {} infra topics, {} tenant topics", infraTopics.size(), topics.size());
        for (KafkaTopic topic : topics) {
            KafkaTopic infraTopic = null;
            for (KafkaTopic existing : infraTopics) {
                String topicName = String.format("%s.%s", topic.getMetadata().getNamespace(), topic.getMetadata().getName());
                if (existing.getMetadata().getName().equals(topicName)) {
                    infraTopic = existing;
                    break;
                }
            }
            if (infraTopic == null) {
                String topicName = String.format("%s.%s", topic.getMetadata().getNamespace(), topic.getMetadata().getName());
                KafkaTopic newTopic = new KafkaTopicBuilder(topic)
                        .withNewMetadata()
                        .withNamespace(strimziInfra)
                        .withName(topicName)
                        .addToLabels("strimzi.io/cluster", kafkaCluster)
                        .endMetadata()
                        .editOrNewSpec()
                        .withTopicName(topicName)
                        .withPartitions(Optional.ofNullable(topic.getSpec()).map(KafkaTopicSpec::getPartitions).orElse(1))
                        .withReplicas(Optional.ofNullable(topic.getSpec()).map(KafkaTopicSpec::getReplicas).orElse(1))
                        .endSpec()
                        .build();
                log.info("Creating topic {}/{}", newTopic.getMetadata().getNamespace(), newTopic.getMetadata().getName());
                op.inNamespace(strimziInfra).createOrReplace(newTopic);
            } else {
                // Sync back status and spec
                if (infraTopic.getStatus() != null && !infraTopic.getStatus().equals(topic.getStatus())) {
                    topic.setStatus(infraTopic.getStatus());
                    op.inNamespace(topic.getMetadata().getNamespace()).updateStatus(topic);
                }

                if (infraTopic.getSpec() != null && !infraTopic.getSpec().equals(topic.getSpec())) {
                    topic.setSpec(infraTopic.getSpec());
                    op.inNamespace(topic.getMetadata().getNamespace()).createOrReplace(topic);
                }
            }
        }

        for (KafkaTopic existing : infraTopics) {
            boolean found = false;
            for (KafkaTopic topic : topics) {
                String topicName = String.format("%s.%s", topic.getMetadata().getNamespace(), topic.getMetadata().getName());
                if (existing.getMetadata().getName().equals(topicName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.info("Deleting topic {}/{}", existing.getMetadata().getNamespace(), existing.getMetadata().getName());
                op.inNamespace(strimziInfra).delete(existing);
            }
        }
    }

    @Gauge(name = "active_topics_total", unit = MetricUnits.NONE, description = "Number of active topics registered.")
    public long getNumActiveTopics() {
        return currentTopics.stream().filter(t -> strimziInfra.equals(t.getMetadata().getNamespace())).count();
    }
}
