#!/usr/bin/env bash
set -ex

mvn package

docker build -f sandbox-api/src/main/docker/Dockerfile.jvm -t sandbox-api:latest sandbox-api
docker build -f sandbox-operator/src/main/docker/Dockerfile.jvm -t sandbox-operator:latest sandbox-operator
docker build -f sandbox-app/Dockerfile -t sandbox-app:latest sandbox-app
