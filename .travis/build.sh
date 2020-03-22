#!/usr/bin/env bash
set -ex

# Setup container build env
. /etc/os-release
sudo sh -c "echo 'deb http://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_${VERSION_ID}/ /' > /etc/apt/sources.list.d/devel:kubic:libcontainers:stable.list"
wget -nv https://download.opensuse.org/repositories/devel:kubic:libcontainers:stable/xUbuntu_${VERSION_ID}/Release.key -O- | sudo apt-key add -
sudo apt-get update -qq
sudo apt-get -qq -y install podman

mvn package

podman build -f src/main/docker/Dockerfile.jvm -t sandbox-api:latest sandbox-api
podman build -f src/main/docker/Dockerfile.jvm -t sandbox-operator:latest sandbox-operator
