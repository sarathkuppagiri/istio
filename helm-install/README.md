# Install istio using helm charts

## Step 1 : Install Istio base chart

```
helm install istio-base base -n istio-system --set defaultRevision=default

```

## Step 2 : Install Istio control plane

```
helm install istiod istio-control/istio-discovery  -n istio-system --set meshConfig.accessLogFile=/dev/stdout

```

## Step 3 : Verify Istio base and Istiod deployment status

```
helm ls -n istio-system

```

## Step 4 : Install Ingress Gateway

```
helm install istio-ingressgateway gateways/istio-ingress -n istio-system

```

## Step 5 : Install egress Gateway

```
helm install istio-egressgateway gateways/istio-egress -n istio-system --set gateways.istio-egressgateway.enabled=true

```

## supported releases and release downloads

```
https://istio.io/latest/docs/releases/supported-releases/
https://github.com/istio/istio/releases

```
