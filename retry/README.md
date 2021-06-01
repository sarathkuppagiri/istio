# Retry

Microservices architectures are distributed. This means more requests on the network, increasing the chance of transient failures like network congestion.

Adding retry policies for requests helps build resiliency in a services architecture. Often, this retry logic is built into source code. But with Istio, you can define retry policies with a traffic rule, offloading this logic to your services' sidecar proxies. This can help you standardize around the same policies across many services, protocols, and programming languages.

```
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: helloworld-gateway
spec:
  selector:
    istio: ingressgateway # use istio default controller
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "*"
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: helloworld
spec:
  hosts:
  - "*"
  gateways:
  - helloworld-gateway
  http:
  - match:
    - uri:
        exact: /hello
    route:
    - destination:
        host: helloworld
        port:
          number: 5000
    retries:
      attempts: 3
      retryOn: 5xx
      perTryTimeout: 3s
```

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
cd istio-1.9.4
```

3. Add the istioctl client to your path (Linux or macOS):
```
export PATH=$PWD/bin:$PATH

```
4. Create and add a namespace label to instruct Istio to automatically inject Envoy sidecar proxies when you deploy your application later:
```
   kubectl create namespace demo-istio
   
   kubectl label namespace demo-istio istio-injection=enabled
   
```
## Deploy the sample application

```
kubectl apply -f helloworld.yaml --namespace demo-istio
It creates service,deployment and pod.

kubectl apply -f helloworld-gateway.yaml --namespace demo-istio
It creates virtualservice and gateway.

Run the below commands to check the status of pod,service,deploymet,virtualservice and gateway.
kubectl get gateway --namespace demo-istio
kubectl get virtualservice --namespace demo-istio
kubectl get pods --namespace demo-istio
kubectl get service --namespace demo-istio
kubectl get deployment --namespace demo-istio

```

## Determining the ingress IP and ports

```
export INGRESS_HOST=$(minikube ip)
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
```

## Test the application

```
  curl http://$GATEWAY_URL/hello
  It retries 3 attempts with 3s delay.
  
```

