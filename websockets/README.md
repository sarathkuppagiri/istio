# Tornado - Demo Websockets App

This is a sample application that demonstrates the use of an upgraded websockets connection on an ingress traffic when using Istio `VirtualService`.
The `app.yaml` creates a Kubernetes `Service` and a `Deployment` that is based on an existing Docker image for [Hiroakis's Tornado Websocket Example](https://github.com/hiroakis/tornado-websocket-example).

__Notice:__ The addition of websockets upgrade support in v1alpha3 routing rules has only been added after the release of `Istio v0.8.0`.

## Prerequisites

Install Istio by following the [Istio Quick Start](https://istio.io/docs/setup/kubernetes/quick-start.html).

## Installation

1. First install the application service:

    - With automatic sidecar injection

    ```command
    kubectl apply -f app.yaml --namespace demo-istio
    ```

1. Create the Ingress `Gateway` and `VirtualService` that enables the upgrade to Websocket for incoming traffic:

    ```command
    kubectl apply -f route.yaml --namespace demo-istio
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

## Test

- Access <http://$GATEWAY_URL/> with your browser

- The `WebSocket status` should show a green `open` status which means  that a websocket connection to the server has been established.
To see the websocket in action see the instructions in the _REST API examples_ section of the demo app webpage for updating the server-side data and getting the updated data through the open websocket to the table in the webpage (without refreshing).

## Cleanup

```command
kubectl delete -f route.yaml --namespace demo-istio
kubectl delete -f app.yaml --namespace demo-istio
```
