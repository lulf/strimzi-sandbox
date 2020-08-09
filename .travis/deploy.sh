#!/usr/bin/env bash
set -e

MAX_ROLLOUT_ATTEMPTS=60
function rollout() {
    local n=${1}
    local d=${2}
    local attempts=0
    local status_cmd="kubectl rollout status deployment/${d} -n ${n}"

    kubectl rollout restart -n ${n} deployment/${d}
    until $status_cmd || [ $attempts -eq $MAX_ROLLOUT_ATTEMPTS ]; do
        $status_cmd
        attempts=$((attempts + 1))
        sleep 10
    done
}

case "$OSTYPE" in
  darwin*)  READLINK=greadlink;;
  *)        READLINK=readlink;;
esac
SCRIPT_DIR=`${READLINK} -f \`dirname $0\``

echo "Retrieving token"
TOKEN=$(curl -k -s "$KEYCLOAK_URL" -d grant_type=password -d response_type=id_token -d scope=openid -d client_id="$KEYCLOAK_CLIENT_ID" -d username="$API_USER" -d password="$API_PASSWORD")
id_token=$(echo "$TOKEN" | jq .id_token -r)
refresh_token=$(echo "$TOKEN" | jq .refresh_token -r)

echo "Producing kubeconfig"
cat $SCRIPT_DIR/kubeconfig-template.json | API_ID_TOKEN=${id_token} API_REFRESH_TOKEN=${refresh_token} envsubst > kubeconfig.json

# Rollout new sandbox components
export KUBECONFIG=$PWD/kubeconfig.json

echo "Current deployments: "
kubectl get deployments -n sandbox

echo "Starting rolling restart"
#kubectl rollout restart -n sandbox deployment/sandbox-operator
#kubectl rollout restart -n sandbox deployment/sandbox-api
#kubectl rollout restart -n sandbox deployment/sandbox-app
rollout sandbox sandbox-operator
rollout sandbox sandbox-api
rollout sandbox sandbox-app
