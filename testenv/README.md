# Deploy EnMasse Sandbox to test environment

This guide documents how to set up the enmasse sandbox in a minikube environment.

## Initialize the minikube 

```
./oidckube.sh init
```

This will configure minikube and generate the certificates for Keycloak.

## Install enmasse

* Download zip from https://github.com/EnMasseProject/enmasse/ or just use templates from master

```
kubectl create namespace enmasse-infra
kubectl -n enmasse-infra apply -f enmasse-latest/install/bundles/enmasse
```

## Deploy resources

```
kubectl create namespace keycloak
kubectl apply -f ../deploy
```

## Configure /etc/hosts

Run the following commands to add entries to /etc/hosts

```
IP=$(minikube ip)
sudo sh -c "echo '${IP} keycloak.devlocal' >> /etc/hosts"
sudo sh -c "echo '${IP} enmasse.devlocal' >> /etc/hosts"
```

## Create realm and clients in keycloak

* Login to the Keycloak console at `https://keycloak.devlocal`. using username `keycloak` and
  password `keycloak`.
* Create a new realm named 'k8s'
* Create an client of type OIDC named 'kube' with confidential access type. Add '\*' to valid redirects
  and web origins.
* Go to credentials and make note of client secret.
* Edit 'config' and enter the secret in the KEYCLOAK_CLIENT_SECRET field
* Create an client of type OIDC named 'webapp' with public access type. Add '\*' to valid redirects and web
  origins.

## Restart minikube

```
./oidckube.sh stop
./oidckube.sh start
```

## Setup GitHub identity provider

Go to the Keycloak admin UI and create a GitHub identity provider for the k8s realm using client id and secrets from GitHub.

## Configure Authentication flow

Go to Authentication page for the k8s realm and edit the 'Identity Provider Redirector' row.
Configure 'github' as the default identity provider.

## Import Keycloak CA in system JKS 

This step is required because Quarkus does not support changing the truststore path.

```
openssl x509 -outform der -in pki/keycloak-ca.pem -out pki/keycloak-ca.der
keytool -trustcacerts -keystore /etc/pki/java/cacerts -storepass changeit -alias Keycloak -import -file pki/keycloak-ca.der
```

## Configure sandbox-api resources with oidc credentials

Edit the sandbox-api/src/main/resources/application.properties and set the server url and oidc properties

## Configure nginx-ingress-controller to allow passthrough

Edit the `nginx-ingress-controller` deployment in the `kube-system` namespace and add the command
line argument `--enable-ssl-passthrough`.

## Create the secret with oidc credentials

```
kubectl create secret generic oidc-secret --from-literal=client-id=kube --from-literal=client-secret=2687b747-82ee-48cd-bb49-17f8a5041c17
```

