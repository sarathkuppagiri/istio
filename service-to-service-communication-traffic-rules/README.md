# Service to Service Communication - Virtual Service

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


```

## Determining the ingress IP and ports

```
Execute the following command to determine if your Kubernetes cluster is running in an environment that supports external load balancers:

$ kubectl get svc istio-ingressgateway -n istio-system
NAME                   TYPE           CLUSTER-IP       EXTERNAL-IP      PORT(S)   AGE
istio-ingressgateway   LoadBalancer   172.21.109.129   130.211.10.121   ...       17h

If the EXTERNAL-IP value is set, your environment has an external load balancer that you can use for the ingress gateway. If the EXTERNAL-IP value is <none> (or perpetually <pending>), your environment does not provide an external load balancer for the ingress gateway. In this case, you can access the gateway using the service’s node port.

If you are using minikube, you can easily start an external load balancer (recommended) by running the following command in a different terminal:

$ minikube tunnel
export INGRESS_HOST=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
$ export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')
 export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
 OR
 
export INGRESS_HOST=$(minikube ip)
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
```

## Test the application

```
  curl http://$GATEWAY_URL/demo1

  output : hello1 from demo1 & hello2 from demo2

  curl http://$GATEWAY_URL/demo2

  output: hello2
  
```

Note : Virtual service which uses gateways only for downstream clients and Virtual service without gateways section for service to service routing rules.

## Create a virtual service for service to service routing rules

```

kubectl apply -f demo-svc-vs.yaml -n demo2

apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: demo-svc-vs
spec:
  hosts:
  - "demo2-svc"
  http:
  - match:
    - uri:
        exact: /demo2
    route:
    - destination:
        host: demo2-svc.demo2.svc.cluster.local
        port:
          number: 80
    timeout: 1s



```



