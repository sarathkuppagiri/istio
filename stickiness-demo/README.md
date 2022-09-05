# Request Affinity with Istio

Consistent Hash-based load balancing can be used to provide soft session affinity based on HTTP headers, cookies or other properties. The affinity to a particular destination host will be lost when one or more hosts are added/removed from the destination service.


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
  You would see 50% of responses from v2 and 50% of responses from v1 versions.

  curl --location --request GET 'http://localhost/hello' --header 'my-header: 123'
  If it returns response from V2 then it always returns response from V2 because of headerName stickiness.


  Consistent Hash-based load balancing can be used to provide soft session affinity based on HTTP headers, cookies or other properties. The affinity to a particular destination host will be lost when one or more hosts are added/removed from the destination service.

  helloworld-dest-with-useSourceIp-stickiness.yaml - Hash based on the source IP address. This is applicable for both TCP and HTTP connections.

  helloworld-dest-with-cookie-stickiness.yaml - Hash based on HTTP cookie.

  helloworld-dest-with-queryparamname-stickiness.yaml - Hash based on a specific HTTP query parameter.
  
```

