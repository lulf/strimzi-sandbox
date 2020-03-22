#!/usr/bin/env bash
set -ex

mvn package

podman build -f src/main/docker/Dockerfile.jvm -t sandbox-api:latest sandbox-api
podman build -f src/main/docker/Dockerfile.jvm -t sandbox-operator:latest sandbox-operator
podman build -f Dockerfile -t sandbox-app:latest sandbox-app
