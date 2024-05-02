# Egress TLS Origination from egress gateway


## Install minikube
```
brew install minikube

```

## Start your cluster

```
minikube start

```

## install istio

1. Go to the Istio release page to download the installation file for your OS, or download and extract the latest release automatically (Linux or macOS):

```
curl -L https://istio.io/downloadIstio | sh -

```

2. Move to the Istio package directory. For example, if the package is istio-1.9.4:
```
cd istio-1.21.2
```

3. Add the istioctl client to your path (Linux or macOS):
```
export PATH=$PWD/bin:$PATH

istioctl install --set profile=demo -y --set "components.egressGateways[0].name=istio-egressgateway" --set "components.egressGateways[0].enabled=true" --set meshConfig.accessLogFile=/dev/stdout

```

4. Create namespace without istio injection enabled and deploy TLS enabled spring boot application
```
   kubectl create namespace mesh-external
   kubectl apply -f ssl-server.yaml
   
```

5. Create and add a namespace label to instruct Istio to automatically inject Envoy sidecar proxies when you deploy your application later:
```
   kubectl create namespace demo-istio
   
   kubectl label namespace demo-istio istio-injection=enabled
   
```
6. Create a secret.
```
  kubectl create secret generic client-credential-cacert --from-file=ca.crt=cert-ssl.crt -n istio-system

```

## Deploy the client application

```
kubectl apply -f ssl-client.yaml

```

## Deploy gateway, virtualservice and destination rules.

```
kubectl apply -f egress-gateway.yaml

```

## expose ssl client service through port-forward

```
kubectl port-forward service/ssl-client 8091:8080 -n demo-istio

```

## Test the application

```
  curl http://localhost:8091/test/sslclient
  curl http://ssl-server.mesh-external.svc.cluster.local/server/ssl
  
```

## Update secret

```
kubectl edit secrets/client-credential-cacert -n istio-system

```

