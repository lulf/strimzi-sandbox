#!/usr/bin/env bash
set -ex

echo "$REGISTRY_PASS" | docker login -u $REGISTRY_USER --password-stdin
docker tag sandbox-app:latest $IMAGE_REGISTRY/$IMAGE_ORG/strimzi-sandbox-app:latest
docker tag sandbox-api:latest $IMAGE_REGISTRY/$IMAGE_ORG/strimzi-sandbox-api:latest
docker tag sandbox-operator:latest $IMAGE_REGISTRY/$IMAGE_ORG/strimzi-sandbox-operator:latest

docker push $IMAGE_REGISTRY/$IMAGE_ORG/strimzi-sandbox-app:latest
docker push $IMAGE_REGISTRY/$IMAGE_ORG/strimzi-sandbox-api:latest
docker push $IMAGE_REGISTRY/$IMAGE_ORG/strimzi-sandbox-operator:latest
