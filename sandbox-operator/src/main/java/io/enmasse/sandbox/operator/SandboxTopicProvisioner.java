/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.enmasse.sandbox.model.CustomResources;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.scheduler.Scheduled;
import io.strimzi.api.kafka.KafkaTopicList;
import io.strimzi.api.kafka.model.DoneableKafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopicBuilder;
import io.strimzi.api.kafka.model.KafkaTopicSpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class SandboxTopicProvisioner {
    private static final Logger log = LoggerFactory.getLogger(SandboxTopicProvisioner.class);

    @Inject
    KubernetesClient kubernetesClient;

    private volatile List<KafkaTopic> currentTopics = new ArrayList<>();

    @ConfigProperty(name = "enmasse.sandbox.strimzi-infra", defaultValue = "strimzi-infra")
    String strimziInfra;

    @ConfigProperty(name = "enmasse.sandbox.kafka-cluster", defaultValue = "sandbox")
    String kafkaCluster;

    private volatile boolean initialized = false;

    @Scheduled(every = "1m")
    public synchronized void refreshTopics() {
        MixedOperation<KafkaTopic, KafkaTopicList, DoneableKafkaTopic, Resource<KafkaTopic, DoneableKafkaTopic>> op = kubernetesClient.customResources(CustomResources.getKafkaTopicCrd(), KafkaTopic.class, KafkaTopicList.class, DoneableKafkaTopic.class);
        currentTopics = op.inAnyNamespace().list().getItems().stream()
                .filter(t -> !strimziInfra.equals(t.getMetadata().getNamespace())).collect(Collectors.toList());
        initialized = true;
    }

    @Scheduled(every = "10s")
    public synchronized void processTopics() {
        if (!initialized) {
            return;
        }
        List<KafkaTopic> topics = new ArrayList<>(currentTopics);
        MixedOperation<KafkaTopic, KafkaTopicList, DoneableKafkaTopic, Resource<KafkaTopic, DoneableKafkaTopic>> op = kubernetesClient.customResources(CustomResources.getKafkaTopicCrd(), KafkaTopic.class, KafkaTopicList.class, DoneableKafkaTopic.class);
        List<KafkaTopic> infraTopics = op.inNamespace(strimziInfra).list().getItems().stream()
                .filter(t -> t.getMetadata().getName().startsWith("tenant-"))
                .collect(Collectors.toList());

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
}
