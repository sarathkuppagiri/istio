# TCP traffic

Istio’s gateway is powerful enough to serve not only HTTP/HTTPS traffic, but any traffic accessible via TCP. For example, we can expose a database (like MongoDB) or a message queue like Kafka through the ingress gateway. When Istio treats the traffic as plain TCP, we do not get as many useful features like retries, request-level circuit breaking, complex routing, etc. This is simply because Istio cannot tell what protocol is being used (unless a specific protocol that Istio understands is used—like MongoDB).

## Exposing TCP ports on the Istio Gateway
The first thing we need to do is create a TCP-based service within our service mesh. For this example, we’ll use the simple echo service from https://github.com/cjimti/go-echo/. This simple TCP service will allow us to login with a simple TCP client like telnet and issue commands that should be displayed back to us.



```
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: echo-tcp-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: tcp-echo
      protocol: TCP
    hosts:
    - "*"
```

we’ve exposed a port on our ingress gateway, we need to route the traffic to the echo service. To do that, we’ll use the VirtualService resource

```
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: tcp-echo-vs-from-gw
spec:
  hosts:
  - "*"
  gateways:
  - echo-tcp-gateway
  tcp:
  - match:
    - port: 80
    route:
    - destination:
        host: tcp-echo-service
        port:
          number: 2701
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
kubectl apply -f echo.yaml --namespace demo-istio
It creates service,deployment and pod.

kubectl apply -f echo-vs.yaml --namespace demo-istio
It creates a virtual service.

kubectl apply -f gateway-tcp.yaml --namespace demo-istio
It creates a gateway.

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
  telnet $INGRESS_HOST $INGRESS_PORT

  > telnet 192.168.64.2 31932
Trying 192.168.64.2...
Connected to 192.168.64.2.
Escape character is '^]'.
Welcome, you are connected to node minikube.
Running on Pod tcp-echo-deployment-cb8cc898b-zg4b9.
In namespace demo-istio.
With IP address 172.17.0.15.
Service default.
hhhh
hhhh
  
```

