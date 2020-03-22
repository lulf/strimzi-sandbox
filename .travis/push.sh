#!/usr/bin/env bash
set -ex

podman login -u $REGISTRY_USER -p $REGISTRY_PASS $IMAGE_REGISTRY
podman tag sandbox-app:latest $IMAGE_REGISTRY/enmasse/sandbox-app:latest
podman tag sandbox-api:latest $IMAGE_REGISTRY/enmasse/sandbox-api:latest
podman tag sandbox-operator:latest $IMAGE_REGISTRY/enmasse/sandbox-operator:latest

podman push $IMAGE_REGISTRY/enmasse/sandbox-app:latest
podman push $IMAGE_REGISTRY/enmasse/sandbox-api:latest
podman push $IMAGE_REGISTRY/enmasse/sandbox-operator:latest
