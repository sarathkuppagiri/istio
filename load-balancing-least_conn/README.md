# load balancing with least conn

By default, Istio uses a round-robin load balancing policy, where each service instance in the instance pool gets a request in turn. Istio also supports the following models, which you can specify in destination rules for requests to a particular service or service subset.

Random: Requests are forwarded at random to instances in the pool.

Weighted: Requests are forwarded to instances in the pool according to a specific percentage.

Least requests: Requests are forwarded to instances with the least number of requests.

This example walks through the load balancing with Least requests.

Kubernetes supports load balancing for inbound traffic. But what about Kubernetes services inside the cluster?

When in-cluster services communicate, a load balancer called kube-proxy forwards requests to service pods at random. You can use Istio to add more complex load balancing methods, enabled by Envoy.

Envoy supports multiple load balancing methods, including random, round-robin, and least request.

Let's see how to use Istio to add least request load balancing for a service called payments, which processes all transactions for a web frontend. The payments service is backed by three pods.

In this least request algorithm, the client-side Envoy will first choose two instances at random. Then, it will forward the request to the instance with the fewest number of active requests, to help ensure even load balancing across all instances.

```
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: helloworld
spec:
  host: helloworld
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN
```

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
cd istio-1.9.4
```

3. Add the istioctl client to your path (Linux or macOS):
```
export PATH=$PWD/bin:$PATH

```
4. Add a namespace label to instruct Istio to automatically inject Envoy sidecar proxies when you deploy your application later:
```
   Note : Skip this step if you had already created.
   kubectl create namespace demo-istio
   
   kubectl label namespace demo-istio istio-injection=enabled
   
```
## Deploy the sample application

```
kubectl apply -f helloworld.yaml --namespace demo-istio
kubectl apply -f helloworld-gateway.yaml --namespace demo-istio
kubectl apply -f helloworld-dest.yaml --namespace demo-istio

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
  
  
```

