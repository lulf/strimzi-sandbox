#!/usr/bin/env bash
set -ex

mvn package

docker build -f src/main/docker/Dockerfile.jvm -t sandbox-api:latest sandbox-api
docker build -f src/main/docker/Dockerfile.jvm -t sandbox-operator:latest sandbox-operator
docker build -f Dockerfile -t sandbox-app:latest sandbox-app
