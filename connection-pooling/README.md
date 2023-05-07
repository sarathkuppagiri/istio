# Connection Pools and Bulk Heading


This example shows you how to injects an HTTP abort fault to test the resiliency of your application.


## Installing minikube
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
cd istio-1.17.2
```

3. Add the istioctl client to your path (Linux or macOS):
```
export PATH=$PWD/bin:$PATH

istioctl install --set profile=demo -y

```
4. Add a namespace label to instruct Istio to automatically inject Envoy sidecar proxies when you deploy your application later:
```
   Note : Skip this step if you had already created.
   kubectl create namespace demo-istio
   
   kubectl label namespace demo-istio istio-injection=enabled
   
```

5. Next, Add the below connection pool configuration to helloworld-connectionpool-gateway.yaml.

```
 trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100             # maximum number of TCP conns
        connectTimeout: 5s              # TCP connection timeout
        tcpKeepalive:                   # keep alive settings
          time: 3600s
          interval: 60s
      http:
        maxRequestsPerConnection: 25    # max request per keep-alive
        http2MaxRequests: 5             # max number of HTTP2 conns
        http1MaxPendingRequests: 5      # max number of pending reqs
        maxRetries: 3                   # max number of retries
        idleTimeout: 60s                # idle timeout for connection

```

## Deploy the sample application

```
kubectl apply -f helloworld.yaml --namespace demo-istio
kubectl apply -f helloworld-gateway.yaml --namespace demo-istio
kubectl apply -f helloworld-connectionpool-dest.yaml --namespace demo-istio

kubectl get gateway --namespace demo-istio
kubectl get virtualservice --namespace demo-istio
kubectl get pods --namespace demo-istio
kubectl get service --namespace demo-istio
kubectl get deployment --namespace demo-istio

```

## Determining the ingress IP and ports

```
$ minikube tunnel
export INGRESS_HOST=$(minikube ip)
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
```

## Test the application

```
  curl http://$GATEWAY_URL/hello
  
```

