# Authentication with JWT

This example shows you how to route requests based on JWT claims on an Istio ingress gateway using the request authentication and virtual service.



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

## Running

Here, we will apply the JWT authentication and authorization policies to the Istio service mesh. Without the policies, everything should work as expected. 
Once we have applied the policies, all requests without a JWT are denied with a 403 Forbidden. Only requests with the correct JWT bearer token are accepted.


## Deploy the sample application

```
kubectl apply -f helloworld.yaml --namespace demo-istio
kubectl apply -f helloworld-gateway.yaml --namespace demo-istio
kubectl apply -f hello-istio-jwt-authz.yaml --namespace demo-istio

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
  Response : RBAC: access denied
  
  curl --location --request GET 'http://$GATEWAY_URL/hello' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6Il8yNXliQm5LZzgwbFMzQW55X1h4OG0wb0xRUTdNdkxEOGVOTWx4d0RzVjQiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOiJzdHVkZW50cyIsImV4cCI6NDczNzk5MTUyMiwiaWF0IjoxNTg0MzkxNTIyLCJpc3MiOiJwYWNrdHB1YiIsInB1Ymxpc2hlciI6InBhY2t0cHViIiwic3ViIjoiZGVtbyJ9.K47Odv9I3DQ9_bh6P1n8iTRmuaXKGhfqI52C2vdSzrW65tgH6OOUvvLilzjd-t4qqqOqkKF1kR4WIUMsW90g81U3mvXl9iGY3RV1BoSdwmPUfSuDJY9OItVPCCplQKiV7srFqmdQqMhLEk_9HLKTYPj_K0rmxv-d1seDdBTYGmNpiL7YFIFFInH1wu5CtXfZ5uNvkDmeTeocAVMVCJohbc9Lx4AbArOSaoMq0UqaniluEmNNYBb64NnfzQVegHUQTxSGjAg4t-jy7V0TvPP4ZRszgPcAYFwoBBsW0aXWuKZMrd5Od42VCsaJuigwojooVFAFnwQgZAhAa_BiLi16Yg'

  Response : Hello World!!!
  
```

