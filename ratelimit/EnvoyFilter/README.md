# Rate Limit

Rate limiting is a mechanism that allows you to limit the number of requests sent to a service. It specifies the maximum number of requests a client can send to a service in a given period. It's expressed as a number of requests in a period of time - for example, 100 requests per minute or 5 requests per second, and so on.

Note that enabling the rate limiting on workloads inside the cluster or at the edge with ingress/egress gateways is more-or-less the same. The difference is only in the context to which the EnvoyFilter patch gets applied.

Local vs. global rate limiting
Envoy implements rate limiting in two different ways:

global (or distributed) rate limiting
local rate limiting

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
cd istio-1.17.2
```

3. Add the istioctl client to your path (Linux or macOS):
```
export PATH=$PWD/bin:$PATH

istioctl install --set profile=demo -y

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

kubectl apply -f ratelimit.yaml
envoyfilter.networking.istio.io/helloworld-ratelimit created


Run the below commands to check the status of pod,service,deploymet,virtualservice and gateway.
kubectl get gateway --namespace demo-istio
kubectl get virtualservice --namespace demo-istio
kubectl get pods --namespace demo-istio
kubectl get service --namespace demo-istio
kubectl get deployment --namespace demo-istio

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
  seq 1 100 | xargs -n1 -P 5 curl -I "http://127.0.0.1:80/hello"
  
HTTP/1.1 429 Too Many Requests
content-length: 18
content-type: text/plain
x-ratelimit-limit: 50
x-ratelimit-remaining: 0
x-ratelimit-reset: 11
date: Sun, 07 May 2023 19:22:41 GMT
server: istio-envoy
x-envoy-upstream-service-time: 0

curl: (7) Couldn't connect to server
  
```

https://learncloudnative.com/blog/2022-09-08-ratelimit-istio

## Rate limiting at the ingress gateway

We can configure the rate limiter at the ingress gateway as well. We can use the same configuration as before, but we need to apply it to the istio-ingressgateway workload instead. Because there's a differentiation between the configuration for sidecars and gateways, we need to use a different context in the EnvoyFilter resource (GATEWAY).

```
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: ingress-ratelimit
  namespace: istio-system
spec:
  workloadSelector:
    labels:
      istio: ingressgateway
  configPatches:
    - applyTo: HTTP_FILTER
      match:
        context: GATEWAY
        listener:
          filterChain:
            filter:
              name: 'envoy.filters.network.http_connection_manager'
              subFilter:
                name: 'envoy.filters.http.router'
      patch:
        operation: INSERT_BEFORE
        value:
          name: envoy.filters.http.local_ratelimit
          typed_config:
            '@type': type.googleapis.com/udpa.type.v1.TypedStruct
            type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
            value:
              stat_prefix: http_local_rate_limiter
              token_bucket:
                max_tokens: 50
                tokens_per_fill: 10
                fill_interval: 120s
              filter_enabled:
                runtime_key: local_rate_limit_enabled
                default_value:
                  numerator: 100
                  denominator: HUNDRED
              filter_enforced:
                runtime_key: local_rate_limit_enforced
                default_value:
                  numerator: 100
                  denominator: HUNDRED
              response_headers_to_add:
                - append_action: APPEND_IF_EXISTS_OR_ADD
                  header:
                    key: x-rate-limited
                    value: TOO_MANY_REQUESTS
              status:
                code: BadRequest

```

## Rate limiting at the egress gateway

The rate limiter at ingress can protect the mesh from external traffic. However, we can also configure the rate limiter at the egress gateway to protect the external services from the calls inside the mesh. This is useful when we want to limit the number of requests to a specific external service.

```
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: egress-ratelimit
  namespace: istio-system
spec:
  workloadSelector:
    labels:
      istio: egressgateway
  configPatches:
    - applyTo: HTTP_FILTER
      match:
        context: GATEWAY
        listener:
          filterChain:
            filter:
              name: 'envoy.filters.network.http_connection_manager'
      patch:
        operation: INSERT_BEFORE
        value:
          name: envoy.filters.http.local_ratelimit
          typed_config:
            '@type': type.googleapis.com/udpa.type.v1.TypedStruct
            type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
            value:
              stat_prefix: http_local_rate_limiter
    - applyTo: HTTP_ROUTE
      match:
        context: GATEWAY
        routeConfiguration:
          vhost:
            name: 'edition.cnn.com:80'
            route:
              action: ANY
      patch:
        operation: MERGE
        value:
          typed_per_filter_config:
            envoy.filters.http.local_ratelimit:
              '@type': type.googleapis.com/udpa.type.v1.TypedStruct
              type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
              value:
                stat_prefix: http_local_rate_limiter
                token_bucket:
                  max_tokens: 50
                  tokens_per_fill: 10
                  fill_interval: 120s
                filter_enabled:
                  runtime_key: local_rate_limit_enabled
                  default_value:
                    numerator: 100
                    denominator: HUNDRED
                filter_enforced:
                  runtime_key: local_rate_limit_enforced
                  default_value:
                    numerator: 100
                    denominator: HUNDRED
                response_headers_to_add:
                  - append_action: APPEND_IF_EXISTS_OR_ADD
                    header:
                      key: x-rate-limited
                      value: TOO_MANY_REQUESTS
                status:
                  code: BadRequest


```

This time resource looks more complex because we're enabling the rate limiter for a specific virtual host. To do that, we still need to insert the rate limiter filter like before. However, we must also add the typed_per_filter_config to the route configuration. This is where we configure the rate limiter for the specific virtual host. Because we need to target a particular virtual host, we'll provide the routeConfigurationin the match section. Inside that, we're targeting a specific virtual host (edition.cnn.com:80) and all routes on that host (action: ANY). Note that we're using the' MERGE' operation because the virtual host and route configuration already exist.

Let's create the EnvoyFilter, go to the curl Pod, and try to send requests to edition.cnn.com:

```
$ curl -v edition.cnn.com
> GET / HTTP/1.1
> User-Agent: curl/7.35.0
> Host: edition.cnn.com
> Accept: */*
>
< HTTP/1.1 400 Bad Request
< x-rate-limited: TOO_MANY_REQUESTS
< content-length: 18
< content-type: text/plain
< date: Sun, 04 Sep 2022 23:42:52 GMT
< server: envoy
< x-envoy-upstream-service-time: 1
<
local_rate_limited
```

Note that if we sent a request to a different external service, we wouldn't get rate limited as we applied the configuration to only one virtual host.