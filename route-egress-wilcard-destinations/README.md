# Routing egress traffic to wildcard destinations

The currently documented egress gateway use-cases rely on the fact that the target of the traffic (the hostname) is statically configured in a VirtualService, telling Envoy in the egress gateway pod where to TCP proxy the matching outbound connections. You can use multiple, and even wildcard, DNS names to match the routing criteria, but you are not able to route the traffic to the exact location specified in the application request. For example you can match traffic for targets *.wikipedia.org, but you then need to forward the traffic to a single final target, e.g., en.wikipedia.org. If there is another service, e.g., anyservice.wikipedia.org, that is not hosted by the same server(s) as en.wikipedia.org, the traffic to that host will fail. This is because, even though the target hostname in the TLS handshake of the HTTP payload contains anyservice.wikipedia.org, the en.wikipedia.org servers will not be able to serve the request.

The solution to this problem at a high level is to inspect the original server name (SNI extension) in the application TLS handshake (which is sent in plain-text, so no TLS termination or other man-in-the-middle operation is needed) in every new gateway connection and use it as the target to dynamically TCP proxy the traffic leaving the gateway.

When restricting egress traffic via egress gateways, we need to lock down the egress gateways so that they can only be used by clients within the mesh. This is achieved by enforcing ISTIO_MUTUAL (mTLS peer authentication) between the application sidecar and the gateway. That means that there will be two layers of TLS on the application L7 payload. One that is the application originated end-to-end TLS session terminated by the final remote target, and another one that is the Istio mTLS session.

Another thing to keep in mind is that in order to mitigate any potential application pod corruption, the application sidecar and the gateway should both perform hostname list checks. This way, any compromised application pod will still only be able to access the allowed targets and nothing more.

## Try it out
```
kubectl apply -f egress.yaml
kubectl apply -f egress-wild-destinations.yaml

kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.21/samples/sleep/sleep.yaml
export SOURCE_POD=$(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name})
export GATEWAY_POD=$(kubectl get pod -n istio-egress -l istio=egressgateway -o jsonpath={.items..metadata.name})

kubectl exec "$SOURCE_POD" -c sleep -- sh -c 'curl -s https://en.wikipedia.org/wiki/Main_Page | grep -o "<title>.*</title>"; curl -s https://de.wikipedia.org/wiki/Wikipedia:Hauptseite | grep -o "<title>.*</title>"'


```


