# Egress TLS Origination from sidecar


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

```

4. Create namespace without istio injection enabled and deploy TLS enabledspring boot application
```
   kubectl create namespace mesh-external
   kubectl apply -f ssl-server.yaml
   
```
5. Create a secret.
```
  kubectl create secret generic my-cert --from-file=cert-ssl.crt -n demo-istio

```

6. Create and add a namespace label to instruct Istio to automatically inject Envoy sidecar proxies when you deploy your application later:
```
   kubectl create namespace demo-istio
   
   kubectl label namespace demo-istio istio-injection=enabled
   
```
## Deploy the client application

```
kubectl apply -f ssl-client.yaml

```

## Deploy the serviceentry and destination rule

```
kubectl apply -f egress-dr.yaml

```

## expose ssl client service through port-forward

```
kubectl port-forward service/ssl-client 8091:8080 -n demo-istio

```

## Test the application

```
  curl http://localhost:8091/test/sslclient
  
```
## Apply destination rule to selected pods(app) using subset labels.
```
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: originate-tls-for-ssl
  namespace: demo-istio
spec:
  host: ssl-server.mesh-external.svc.cluster.local
  subsets:
    - name: sample
      labels:
        app: my-ssl-client
  trafficPolicy:
    loadBalancer:
      simple: ROUND_ROBIN
    portLevelSettings:
    - port:
        number: 443
      tls:
        mode: SIMPLE
        caCertificates: /etc/my-cert/cert-ssl.crt

```

