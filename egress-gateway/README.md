# Managing Egress traffic using Istio


In the Routing traffic to services outside of the cluster section, we discovered how service entries can be used to update the Istio service registry about services external to the mesh and the cluster. Service entries are a way to add additional entries into Istio’s internal service registry for virtual services to be able to route to those entries. An Egress gateway, however, is used for controlling how the traffic for external service leaves the mesh.

To get familiar with Egress gateways, we will first deploy a Pod within the mesh from which we can call an external service:


```
$ kubectl apply -f utilities/curl.yaml

```

The command creates a Pod from which you can perform curl; this mimics a workload running inside the mesh:



```
$ kubectl exec -it curl sh -n chapter4

```

From the shell, access httpbin.org using curl:

```
$ curl -v https://httpbin.org/get

```

Now, we will stop all Egress traffic from the mesh using the following command:


```
$ istioctl install -y --set profile=demo --set meshConfig.outboundTrafficPolicy.mode=REGISTRY_ONLY

```

In the previous command, we are modifying the Istio installation to change the outbound traffic policy from ALLOW_ANY to REGISTRY_ONLY, which enforces that only hosts defined with ServiceEntry resources are part of the mesh service registry.

Go back and try curl again; you will see the following output:

```
$ curl -v https://httpbin.org/get
curl: (35) OpenSSL SSL_connect: SSL_ERROR_SYSCALL in connection to httpbin.org:443

```

Let’s now list httpbin.org in the Istio service registry by creating a service entry as follows:


```
apiVersion: networking.istio.io/v1alpha3
kind: ServiceEntry
metadata:
  name: httpbin-svc
  namespace: chapter4
spec:
  hosts:
  - httpbin.org
  location: MESH_EXTERNAL
  resolution: DNS
  ports:
  - number: 443
    name: https
    protocol: HTTPS
  - number: 80
    name: http
    protocol: HTTP
```

Now, you may go ahead and apply the configuration:

```
$ kubectl apply -f 10-a-istio-egress-gateway.yaml

```

Access https://httpbin.org/get from the curl Pod; this time, you will succeed.

ServiceEntry added httpbin.org to the mesh service registry, and hence we were able to access httpbin.org from the curl Pod.

Though ServiceEntry is great for providing external access, it does not provide any control over how the external endpoints should be accessed. For example, you may want only certain workloads or namespaces to be able to send traffic to an external resource. What if there is a need to verify the authenticity of an external resource by verifying its certificates?

The Egress gateway, along with a combination of virtual services, destination rules, and service entries, provides flexible options to manage and control traffic egressing out of the mesh. So, let’s make configuration changes to route all traffic for httpbin.org to the Egress gateway:


1. Configure the Egress gateway, which is very similar to the Ingress gateway configuration. Please note the Egress gateway is attached to httpbin.org; you can provide other hosts or * to match all hostnames:

```
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: istio-egressgateway
  namespace: chapter4
spec:
  selector:
    istio: egressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - httpbin.org
```

2. Next, configure the virtual service. Here, we are configuring the virtual service to attach to the Egress gateway as well as to the mesh:

```

spec:
  hosts:
  - httpbin.org
  gateways:
  - istio-egressgateway
  - mesh


```

In the following part of the virtual service definition, we are configuring that all traffic originating from within the mesh for the httpbin.org host will be directed to the Egress gateway:

```

http:
  - match:
    - gateways:
      - mesh
      port: 80
    route:
    - destination:
        host: istio-egressgateway.istio-system.svc.cluster.local
        subset: httpbin
        port:
          number: 80
      weight: 100

```

We have configured subset: httpbin to apply destination rules; in this example, the destination rules are empty.

Finally, we will add another rule to route traffic from the Egress gateway to httpbin.org:

```

  - match:
    - gateways:
      - istio-egressgateway
      port: 80
    route:
    - destination:
        host: httpbin.org
        port:
          number: 80
      weight: 100

```

3. Create a placeholder for any destination rules you might want to implement:

```

apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: rules-for-httpbin-egress
  namespace: chapter4
spec:
  host: istio-egressgateway.istio-system.svc.cluster.local
  subsets:
  - name: httpbin


```

4. You also need to add ServiceEntry for httpbin.org, which we discussed in the previous section.
5. Go ahead and apply the changes:

```
kubectl apply -f 10-b-istio-egress-gateway.yaml

```

6. Try accessing httpbin.org from the curl Pod; you will be able to access it now.

Examine the headers in the response, as well as the logs of istio-egressgateway pods. You will find information about the Egress gateway under X-Envoy-Peer-Metadata-Id. You can also see the request in the Egress gateway logs.

You will notice that you are not able to access https://httpbin.org/get, although we have defined https in the service entry. Try enabling https access to httpbin.org; you will find the solution in 10-c-istio-egress-gateway.yaml.

Egress is important to control traffic leaving the mesh.

Revert the authorization policy to allow all outgoing traffic from the mesh without needing an Egress gateway using the following command:

```

$ istioctl install -y --set profile=demo --set meshConfig.outboundTrafficPolicy.mode=ALLOW_ANY


```