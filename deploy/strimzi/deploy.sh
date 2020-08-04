kubectl create namespace strimzi-infra
kubectl create clusterrolebinding strimzi-cluster-operator-namespaced --clusterrole=strimzi-cluster-operator-namespaced --serviceaccount strimzi-infra:strimzi-cluster-operator
kubectl create clusterrolebinding strimzi-cluster-operator-entity-operator-delegation --clusterrole=strimzi-entity-operator --serviceaccount strimzi-infra:strimzi-cluster-operator
kubectl create clusterrolebinding strimzi-cluster-operator-topic-operator-delegation --clusterrole=strimzi-topic-operator --serviceaccount strimzi-infra:strimzi-cluster-operator
kubectl apply -f cluster-operator -n strimzi-infra
