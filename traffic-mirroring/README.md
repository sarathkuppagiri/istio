# Perform Traffic Mirroring

Traffic mirroring, also called shadowing, is a powerful concept that allows feature teams to bring changes to production with as little risk as possible. Mirroring sends a copy of live traffic to a mirrored service. The mirrored traffic happens out of band of the critical request path for the primary service.



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

   Next, configure traffic mirroring for the helloworld v2 virtual service.

```yaml
  mirror:
    host: helloworld
    subset: v2
  mirror_percent: 100
```
   
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

  # check the container logs of the helloworld v2 pod
  $ kubectl logs helloworld-v2-6dcc4fff9-hnbxs
  
```

