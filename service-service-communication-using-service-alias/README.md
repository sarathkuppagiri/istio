# Service to Service Communication - Service alias name (External Service)

An ExternalName service is a special case of service that does not have selectors. It does not define any ports or Endpoints. Rather, it serves as a way to return an alias to an external service residing outside the cluster.

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
cd istio-1.22.1
```

3. Add the istioctl client to your path (Linux or macOS):
```
export PATH=$PWD/bin:$PATH

istioctl install --set profile=demo -y

```
4. Create and add a namespace label to instruct Istio to automatically inject Envoy sidecar proxies when you deploy your application later:
```
   kubectl create namespace demo1

   kubectl create namespace demo2
   
   kubectl label namespace demo1 istio-injection=enabled

   kubectl label namespace demo2 istio-injection=enabled
   
```
## Deploy the sample application

```
kubectl apply -f demo1.yaml --namespace demo1
It creates service,deployment and pod.

kubectl apply -f demo2.yaml --namespace demo2
It creates service,deployment and pod.

kubectl apply -f virtualservice.yaml --namespace demo1
It creates virtualservice and gateway.

kubectl apply -f demo2-alias-service.yaml -n demo2


```


## Test the application

```
  curl http://$GATEWAY_URL/demo1

  output : It should return timeout error since i configure timeout as 1s but api takes 3 seconds.
  
```




