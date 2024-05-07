# Perform TLS origination with an egress gateway using SDS with multiple certs

This section describes how to perform the same TLS origination as in the TLS Origination for Egress Traffic example, only this time using an egress gateway. Note that in this case the TLS origination will be done by the egress gateway, as opposed to by the sidecar in the previous example. The egress gateway will use SDS instead of the file-mount to provision client certificates.

Generate CA and server certificates and keys
For this task you can use your favorite tool to generate certificates and keys. The commands below use openssl.

Create a root certificate and private key to sign the certificate for your services:

$ openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -subj '/O=example Inc./CN=example.com' -keyout example.com.key -out example.com.crt

Create a certificate and a private key for my-nginx.mesh-external.svc.cluster.local:

$ openssl req -out my-nginx.mesh-external.svc.cluster.local.csr -newkey rsa:2048 -nodes -keyout my-nginx.mesh-external.svc.cluster.local.key -subj "/CN=my-nginx.mesh-external.svc.cluster.local/O=some organization"
$ openssl x509 -req -days 365 -CA example.com.crt -CAkey example.com.key -set_serial 0 -in my-nginx.mesh-external.svc.cluster.local.csr -out my-nginx.mesh-external.svc.cluster.local.crt

Deploy a simple TLS server
To simulate an actual external service that supports the simple TLS protocol, deploy an NGINX server in your Kubernetes cluster, but running outside of the Istio service mesh, i.e., in a namespace without Istio sidecar proxy injection enabled.

Create a namespace to represent services outside the Istio mesh, namely mesh-external. Note that the sidecar proxy will not be automatically injected into the pods in this namespace since the automatic sidecar injection was not enabled on it.

$ kubectl create namespace mesh-external

Create Kubernetes Secrets to hold the server’s and CA certificates.

$ kubectl create -n mesh-external secret tls nginx-server-certs --key my-nginx.mesh-external.svc.cluster.local.key --cert my-nginx.mesh-external.svc.cluster.local.crt
$ kubectl create -n mesh-external secret generic nginx-ca-certs --from-file=example.com.crt

Create a configuration file for the NGINX server:

$ cat <<\EOF > ./nginx.conf
events {
}

http {
  log_format main '$remote_addr - $remote_user [$time_local]  $status '
  '"$request" $body_bytes_sent "$http_referer" '
  '"$http_user_agent" "$http_x_forwarded_for"';
  access_log /var/log/nginx/access.log main;
  error_log  /var/log/nginx/error.log;

  server {
    listen 443 ssl;

    root /usr/share/nginx/html;
    index index.html;

    server_name my-nginx.mesh-external.svc.cluster.local;
    ssl_certificate /etc/nginx-server-certs/tls.crt;
    ssl_certificate_key /etc/nginx-server-certs/tls.key;
    ssl_client_certificate /etc/nginx-ca-certs/example.com.crt;
    ssl_verify_client off; # In simple TLS, server doesn't verify client's certificate
  }
}
EOF

Create a Kubernetes ConfigMap to hold the configuration of the NGINX server:

$ kubectl create configmap nginx-configmap -n mesh-external --from-file=nginx.conf=./nginx.conf

Deploy the NGINX server:

$ kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: my-nginx
  namespace: mesh-external
  labels:
    run: my-nginx
spec:
  ports:
  - port: 443
    protocol: TCP
  selector:
    run: my-nginx
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
  namespace: mesh-external
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 1
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
      - name: my-nginx
        image: nginx
        ports:
        - containerPort: 443
        volumeMounts:
        - name: nginx-config
          mountPath: /etc/nginx
          readOnly: true
        - name: nginx-server-certs
          mountPath: /etc/nginx-server-certs
          readOnly: true
        - name: nginx-ca-certs
          mountPath: /etc/nginx-ca-certs
          readOnly: true
      volumes:
      - name: nginx-config
        configMap:
          name: nginx-configmap
      - name: nginx-server-certs
        secret:
          secretName: nginx-server-certs
      - name: nginx-ca-certs
        secret:
          secretName: nginx-ca-certs
EOF

Configure simple TLS origination for egress traffic
Create a Kubernetes Secret to hold the CA certificate used by egress gateway to originate TLS connections:

$ kubectl create secret generic client-credential-cacert --from-file=ca.crt=example.com.crt -n istio-system

Note that the secret name for an Istio CA-only certificate must end with -cacert and the secret must be created in the same namespace as Istio is deployed in, istio-system in this case.

The secret name should not begin with istio or prometheus, and the secret should not contain a token field.
Create an egress Gateway for my-nginx.mesh-external.svc.cluster.local, port 443, and destination rules and virtual services to direct the traffic through the egress gateway and from the egress gateway to the external service.

$ kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: istio-egressgateway
spec:
  selector:
    istio: egressgateway
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    hosts:
    - my-nginx.mesh-external.svc.cluster.local
    tls:
      mode: ISTIO_MUTUAL
---
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: egressgateway-for-nginx
spec:
  host: istio-egressgateway.istio-system.svc.cluster.local
  subsets:
  - name: nginx
    trafficPolicy:
      loadBalancer:
        simple: ROUND_ROBIN
      portLevelSettings:
      - port:
          number: 443
        tls:
          mode: ISTIO_MUTUAL
          sni: my-nginx.mesh-external.svc.cluster.local
EOF

Define a VirtualService to direct the traffic through the egress gateway:

$ kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: direct-nginx-through-egress-gateway
spec:
  hosts:
  - my-nginx.mesh-external.svc.cluster.local
  gateways:
  - istio-egressgateway
  - mesh
  http:
  - match:
    - gateways:
      - mesh
      port: 80
    route:
    - destination:
        host: istio-egressgateway.istio-system.svc.cluster.local
        subset: nginx
        port:
          number: 443
      weight: 100
  - match:
    - gateways:
      - istio-egressgateway
      port: 443
    route:
    - destination:
        host: my-nginx.mesh-external.svc.cluster.local
        port:
          number: 443
      weight: 100
EOF

Add a DestinationRule to perform simple TLS origination

$ kubectl apply -n istio-system -f - <<EOF
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: originate-tls-for-nginx
spec:
  host: my-nginx.mesh-external.svc.cluster.local
  trafficPolicy:
    loadBalancer:
      simple: ROUND_ROBIN
    portLevelSettings:
    - port:
        number: 443
      tls:
        mode: SIMPLE
        credentialName: client-credential # this must match the secret created earlier without the "-cacert" suffix
        sni: my-nginx.mesh-external.svc.cluster.local
EOF

Send an HTTP request to http://my-nginx.mesh-external.svc.cluster.local:

$ kubectl exec "$(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name})" -c sleep -- curl -s http://my-nginx.mesh-external.svc.cluster.local
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
...

Check the log of the istio-egressgateway pod for a line corresponding to our request. If Istio is deployed in the istio-system namespace, the command to print the log is:

$ kubectl logs -l istio=egressgateway -n istio-system | grep 'my-nginx.mesh-external.svc.cluster.local' | grep HTTP

You should see a line similar to the following:

[2018-08-19T18:20:40.096Z] "GET / HTTP/1.1" 200 - 0 612 7 5 "172.30.146.114" "curl/7.35.0" "b942b587-fac2-9756-8ec6-303561356204" "my-nginx.mesh-external.svc.cluster.local" "172.21.72.197:443"

Cleanup the TLS origination example
Remove the Istio configuration items you created:

$ kubectl delete destinationrule originate-tls-for-nginx -n istio-system
$ kubectl delete virtualservice direct-nginx-through-egress-gateway
$ kubectl delete destinationrule egressgateway-for-nginx
$ kubectl delete gateway istio-egressgateway
$ kubectl delete secret client-credential-cacert -n istio-system
$ kubectl delete service my-nginx -n mesh-external
$ kubectl delete deployment my-nginx -n mesh-external
$ kubectl delete configmap nginx-configmap -n mesh-external
$ kubectl delete secret nginx-server-certs nginx-ca-certs -n mesh-external
$ kubectl delete namespace mesh-external

Delete the certificates and private keys:

$ rm example.com.crt example.com.key my-nginx.mesh-external.svc.cluster.local.crt my-nginx.mesh-external.svc.cluster.local.key my-nginx.mesh-external.svc.cluster.local.csr

Delete the generated configuration files used in this example:

$ rm ./nginx.conf


```
kubectl apply -f ssl-client.yaml
kubectl apply -f egress-gateway.yaml
kubectl apply -f nginx-vs.yaml
kubectl create secret generic client-credential-nginx-cacert --from-file=ca.crt=example.com.crt -n demo-istio
kubectl create secret generic client-credential-cacert --from-file=ca.crt=cert-ssl.crt -n demo-istio

```
