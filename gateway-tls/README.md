# Ingress Gateway with TLS

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
cd istio-1.9.4
```

3. Add the istioctl client to your path (Linux or macOS):
```
export PATH=$PWD/bin:$PATH

istioctl install --set profile=demo -y

```
4. Create and add a namespace label to instruct Istio to automatically inject Envoy sidecar proxies when you deploy your application later:
```

   
   kubectl label namespace default istio-injection=enabled
   
```
## Using self-signed certificate

In this scenario we are manually creating a self-signed certificate. First thing - pick a domain you want to use - note that to test this, you don’t have to own an actual domain name because we will use a self-signed certificate.

I will use `testistiomtls.dev` as my domain name and I'll use a subdomain called `hello`. So we will configure the gateway with a host called `hello.testistiomtls.dev` and present the self-signed certificate.

Let's store that value in a variable because we will use it throughout this example.

```sh
export DOMAIN_NAME=testistiomtls.dev
```

I will also create a separate folder to hold the root certificate and the private key we will create.

```sh
mkdir -p istio-certs
```

### Creating the cert files

Next we will create the root certificate called `testistiomtls.dev.crt` and the private key used for signing the certificate (file `testistiomtls.dev.key`):

```sh
openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -subj '/O=$DOMAIN_NAME Inc./CN=$DOMAIN_NAME' -keyout $DOMAIN_NAME.key -out $DOMAIN_NAME.crt
```

The next step is to create the certificate signing request and the corresponding key: 

```sh
openssl req -out hello.$DOMAIN_NAME.csr -newkey rsa:2048 -nodes -keyout hello.$DOMAIN_NAME.key -subj "/CN=hello.$DOMAIN_NAME/O=hello world from $DOMAIN_NAME"
```

Finally using the certificate authority and it's key as well as the certificate signing requests, we can create our own self-signed certificate: 

```sh
openssl x509 -req -days 365 -CA $DOMAIN_NAME.crt -CAkey $DOMAIN_NAME.key -set_serial 0 -in hello.$DOMAIN_NAME.csr -out hello.$DOMAIN_NAME.crt
```

### Creating the Kubernetes secret

Now that we have the certificate and the correspondig key we can create a Kubernetes secret to store them in our cluster. 

We will create the secret in the `istio-system` namespace and reference it from the Gateway resource:

```sh
kubectl create -n istio-system secret tls testistiomtls-credential --key=hello.testistiomtls.dev.key --cert=hello.testistiomtls.dev.crt
```
## Deploy the sample application

```
kubectl apply -f demo1.yaml
kubectl apply -f demo2.yaml
It creates service,deployment and pod.

kubectl apply -f virtualservice.yaml
It creates virtualservice and gateway.

Run the below commands to check the status of pod,service,deploymet,virtualservice and gateway.
kubectl get gateway
kubectl get virtualservice
kubectl get pods
kubectl get service
kubectl get deployment

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

## Access jager Ui
istioctl dashboard jaeger

## Access kiali

kubectl port-forward service/kiali 20001 20001 --namespace istio-system 

## Test the application

```
curl -H "Host:hello.testistiomtls.dev" --resolve "hello.testistiomtls.dev:443:$INGRESS_HOST" --cacert /Users/sarathkumarreddy/istio-certs/testistiomtls.dev.crt "https://hello.testistiomtls.dev:443/demo1"  
```

