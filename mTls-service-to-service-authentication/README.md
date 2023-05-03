# TRAFFIC ENCRYPTION USING MTLS 

Mutual TLS can be enabled on 3 levels:

Service: Enable mTLS for a subset of services. It can be a service on the edge that communicate with the external world and need an encrypted communication.

Namespace: Enable mTLS for a specific namespace. Services within the namespace will have mTLS installed and communicate using TLS.

Mesh: Enable mTLS for the entire mesh network.

## How to Enable Automatic mTLS in Istio?

In PeerAuthentication, you can specify the mTLS mode that will be used for the target workload. Peer authentication is supported in the following modes:

PERMISSIVE: The workload’s default setting that allows it to accept either mTLS or plain text traffic.
STRICT: The workload accepts only mTLS traffic.
DISABLE: Disable mTLS. From a security perspective, mTLS should not be disabled unless you have your own security solution.
UNSET: Inherited from the parent, with the following priority: service specific > namespace scope > mesh scope setting.

Istio’s peer authentication uses PERMISSIVE mode by default, automatically sending mTLS traffic to these workloads and clear text traffic to workloads without a sidecar. After including Kubernetes services in the Istio mesh, we can use PERMISSIVE mode first to prevent services from failing mTLS. We can use one of two ways to enable strict mTLS mode for certain services:

Use PeerAuthentication to define how traffic is transferred between sidecars.
Use DestinationRule to define the TLS settings in the traffic routing policy.

## Setup service-to-service communication using mTLS:

1. Create a namespace called chapter6 with Istio injection enabled and deploy the httpbin service:

```
kubectl apply -f 01-httpbin-deployment.yaml

```

Most of the config in this deployment is the usual, except that we have also created a default Kubernetes service account called httpbin in the demo namespace:

```
apiVersion: v1
kind: ServiceAccount
metadata:
  name: httpbin
  namespace: demo
```

The httpbin identity is then assigned to an httpbin Pod by following these specs:

```
	Spec:
      serviceAccountName: httpbin
      containers:
      - image: docker.io/kennethreitz/httpbin
        imagePullPolicy: IfNotPresent
        name: httpbin
        ports:
        - containerPort: 80

```

2. Next, we will create a client in the form of a curl Pod to access the httpbin service. Create a utilities namespace with Istio injection disabled, and create a curl Deployment with its own service account:

```
kubectl apply -f 01-curl-deployment.yaml

```
Make sure the istio-injection label is not applied. If it is, you can remove it using the following command:

```
 kubectl label ns utilities istio-injection-

```

3. From the curl Pod, try to access the httpbin Pod and you should get a response back:

```

$ kubectl exec -it curl -n utilities – curl -v http://httpbin.demo.svc.cluster.local:8000/get
{
  "args": {},
  "headers": {
    "Accept": "*/*",
    "Host": "httpbin.chapter6.svc.cluster.local:8000",
    "User-Agent": "curl/7.87.0-DEV",
    "X-B3-Sampled": "1",
    "X-B3-Spanid": "a00a50536c3ec2f5",
    "X-B3-Traceid": "49b6942c85c7c1f2a00a50536c3ec2f5"
  },
  "origin": "127.0.0.6",
  "url": "http://httpbin.chapter6.svc.cluster.local:8000/get"

```

4. So far, we have the httpbin Pod running in the mesh, but by default, in permissive TLS mode. We will now create a PeerAuthentication policy to enforce STRICT mTLS. The PeerAuthentication policy defines how traffic will be tunneled via sidecars:

```

apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: "httpbin-strict-tls"
  namespace: demo
spec:
  mtls:
    mode: STRICT
  selector:
    matchLabels:
      app: httpbin
```

The policy enforces string mTLS for all workloads that have a label of app=httpbin. The configuration is available at 02-httpbin-strictTLS.yaml.

5. Apply the changes via the following command:

```

$ kubectl apply -f 02-httpbin-strictTLS.yaml
peerauthentication.security.istio.io/httpbin-strict-tls created

```

6. Now try to connect to the httpbin service from the curl Pod:

```
$ kubectl exec -it curl -n utilities – curl -v http://httpbin.demo.svc.cluster.local:8000/get
* Connected to httpbin.demo.svc.cluster.local (172.20.147.104) port 8000 (#0)
> GET /get HTTP/1.1
> Host: httpbin.demo.svc.cluster.local:8000
> User-Agent: curl/7.87.0-DEV
> Accept: */*
>
* Recv failure: Connection reset by peer
* Closing connection 0
curl: (56) Recv failure: Connection reset by peer
command terminated with exit code 56

```

curl is not able to connect because the curl Pod is running in a namespace with Istio injection disabled, whereas the httpbin Pod is running in the mesh with the PeerAuthentication policy enforcing STRICT mTLS. One option is to manually establish an mTLS connection, which is equivalent to modifying your application code to perform mTLS. In this case, as we are trying to simulate service communication within the mesh, we can simply turn on Istio injection and let Istio take care of client-side mTLS as well.

7. Enable Istio injection for the curl Pod using the following steps:

- Delete the resource created by 01-curl-deployment.yaml.
- Modify the value of Istio injection to be enabled.
- Apply the updated configuration.

Once the curl Pod is in the RUNNING state, along with the istio-proxy sidecar, you can perform curl on the httpbin service and you will see the following output:

```
$ kubectl exec -it curl -n utilities -- curl -s http://httpbin.demo.svc.cluster.local:8000/get
{
  "args": {},
  "headers": {
    "Accept": "*/*",
    "Host": "httpbin.demo.svc.cluster.local:8000",
    "User-Agent": "curl/7.85.0-DEV",
    "X-B3-Parentspanid": "a35412ed46b7ec46",
    "X-B3-Sampled": "1",
    "X-B3-Spanid": "0728b578e88b72fb",
    "X-B3-Traceid": "830ed3d5d867a460a35412ed46b7ec46",
    "X-Envoy-Attempt-Count": "1",
    "X-Forwarded-Client-Cert": "By=spiffe://cluster.local/ns/demo/sa/
httpbin;Hash=b1b88fe241c557bd1281324b458503274eec3f04b1d439758508842d6d5b7018;Subject=\"\";URI=spiffe://cluster.local/ns/utilities/sa/curl"
  },
  "origin": "127.0.0.6",
  "url": "http://httpbin.demo.svc.cluster.local:8000/get"
}

```

In the response from the httpbin service, you will notice all the headers that were received by the httpbin Pod. The most interesting header is X-Forwarded-Client-Cert, also called XFCC. There are two parts of the XFCC header value that shed light on mTLS:

- By: This is filled with the SAN, which is the SPIFFE ID of the istio-proxy’s client certificate of the httpbin Pod (spiffe://cluster.local/ns/chapter6/sa/httpbin)

- URI: This contains the SAN, which is the SPIFFE ID of the curl Pod’s client certificate presented during mTLS (spiffe://cluster.local/ns/utilities/sa/curl)

You can selectively apply mTLS configuration at the port level also. In the following configuration, we are implying that mTLS is enforced strictly for all ports except port 8080, which should allow permissive connections. The configuration is available at Chapter6/03-httpbin-strictTLSwithException.yaml:


```
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: "httpbin-strict-tls"
  namespace: demo
spec:
  portLevelMtls:
    8080:
      mode: PERMISSIVE
    8000:
      mode: STRICT
  selector:
    matchLabels:
      app: httpbin

```

So, in this section, we learned how to perform mTLS between services inside the mesh. mTLS can be enabled at the service level as well as at the port level.



