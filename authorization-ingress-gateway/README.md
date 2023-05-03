# Authorization on Ingress Gateway

This is a simple java spring boot helloworld service that returns simple response like 'hello world!!!' when called.

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
4. Run below command.
```
kubectl apply -f 01-httpbin-deployment.yaml

```
## Running

```
$ curl -s 'https://api.ipify.org?format=json'
$ export CLIENT_IP=$(curl -s 'https://api.ipify.org?format=text')

$ sed "s/<<CLIENT_IP>>/$CLIENT_IP/" kubernetes/hello-istio-gateway-policy-deny.yaml | kubectl apply -f -
$ curl -H "Host:httpbin.org" http://127.0.0.1:80/get

$ sed "s/<<CLIENT_IP>>/$CLIENT_IP/" kubernetes/hello-istio-gateway-policy-allow.yaml | kubectl apply -f -
$ curl -H "Host:httpbin.org" http://127.0.0.1:80/get

# do some cleanup
$ kubectl delete AuthorizationPolicy -n istio-system --all

```
## Authorization for HTTP traffic

```

kubectl apply -f hello-message-http-policy.yaml
curl -H "Host:httpbin.org" http://127.0.0.1:80/get


```
