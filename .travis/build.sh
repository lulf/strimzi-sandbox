#!/usr/bin/env bash
set -ex

mvn package

${DOCKER} build -f src/main/docker/Dockerfile.jvm -t sandbox-api:latest sandbox-api
${DOCKER} build -f src/main/docker/Dockerfile.jvm -t sandbox-operator:latest sandbox-operator
