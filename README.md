# ServiceMesh
service mesh provides a configurable infrastructure layer that makes service-to-service communication flexible, reliable, and fast.

A service mesh is a tool for adding observability, security, and reliability features to applications by inserting these features at the platform layer 
rather than the application layer.

The service mesh is typically implemented as a scalable set of network proxies deployed alongside application code (a pattern sometimes called a sidecar). These proxies handle the communication between the microservices and also act as a point at which the service mesh features can be introduced. The proxies comprise the service mesh’s data plane, and are controlled as a whole by its control plane.

If you are building applications on Kubernetes, then a service mesh like istio provides critical observability, reliability, and security features with one big advantage: the application doesn’t need to implement these features, or even to be aware that the service mesh is there!

## Istio
Istio makes it easy to create a network of deployed services with load balancing, service-to-service authentication, monitoring, and more, with few or no code changes in service code. You add Istio support to services by deploying a special sidecar proxy throughout your environment that intercepts all network communication between microservices, then configure and manage Istio using its control plane functionality, which includes:

Automatic load balancing for HTTP, gRPC, WebSocket, and TCP traffic.

Fine-grained control of traffic behavior with rich routing rules, retries, failovers, and fault injection.

A pluggable policy layer and configuration API supporting access controls, rate limits and quotas.

Automatic metrics, logs, and traces for all traffic within a cluster, including cluster ingress and egress.

Secure service-to-service communication in a cluster with strong identity-based authentication and authorization.


