# Dynamic Service Meshes with Envoy Proxy, Java and Spring

## Overview

The repository contains the [latest version of slides](slides/slides.pdf)
for my O'Reilly SACon 2019 talk for a dynamic service mesh using Envoy
Proxy, Java and Spring along with a demo and corresponding source code
under a permissive Apache License 2.0.  It is based on Envoy Proxy
1.12.0, java-control-plane 0.1.19, Spring 5.1.10.RELEASE, Spring Boot
2.1.9.RELEASE and Java 8.
  
The source code includes "echo-service" as an REST-based web service and
"control-plane-impl" as a control plane implementation based on
java-control-plane, providing a TLS gRPC-based xDS API using self-signed
certificates for Envoy Proxy instances that can be managed using a
REST-based web service.

The control plane implementation demonstrates three use cases, namely

* a "HTTP front proxy" setup with the proxy receiving HTTP requests and
  forwarding them to a Echo service instance via HTTP,

* an "Incremental Deployment" setup with the proxy receiving HTTP requests
  and forwarding them to one of two Echo service instances, allowing to
  gradually forward load in an incremental deployment, and

* a "HTTPS front proxy" setup with the proxy terminating a mTLS connection
  and forwarding a HTTPS request to an Echo service instance via HTTP. 

## Preparation

The demo requires the generation of three private key / certificate
pairs, one for the TLS gRPC xDS API, one for the Envoy Proxy instance
connecting to the xDS API, and one for the HTTPS client authenticating in
the mTLS connection. Furthermore, it requires the echo-service and
control-plane-impl Maven projects to be built to generate their executable
jars.

Assuming you have the OpenSSL command line tool available, you can use
the ```prepare.sh``` script to

* generate private key / certificate pairs,
* copy private key / certificate pairs to the demo directories,
* build the Maven projects for echo-service and control-plane-impl,
* copy the executable jars for echo-service and control-plane-impl to the demo
  directories, and
* copies the envoy binary to the demo directories

You can revert everything to the initial state by using the ```clean.sh```
script, which

* removes private key / certificate pairs,
* deletes the executable jars,
* deletes the envoy binary, and
* deletes the logs and
* cleans the Maven projects for echo-service and control-plane-impl

from the demo directories

## Demonstration

Everything for a use case is located in a directory like
```demo/01-http-front-proxy/```; multiple terminals side-by-side work
well for that. After testing a use case, terminate the services before
starting over with another use case, more on that in the Bugs section.

### HTTP Front Proxy

* Start the echo-service in ```01-echo/``` with ```./run.sh```
* Start the control-plane-impl in ```02-control-plane/``` with
  ```./run.sh```
* Start Envoy Proxy in ```03-envoy/``` with ```./run.sh```
* Prepare the use case in ```04-curl-1/``` with ```./prepare.sh``` 
* Show the use case in ```05-curl-2/``` with ```./show.sh```

After the preparation call, the control plane configures Envoy Proxy
to forward HTTP requests to the echo-service, forwarding access logs
and metrics back to the control plane. After echo-service responded
to the request, Envoy Proxy is configured to add a HTTP header to the
response telling which upstream host answered the response.

You can run the ```show.sh``` script multiple times, compare the outputs
and check with the source code of the scripts and the services to see
how everything comes together.

### Incremental Deployment

* Start the echo-service alpha in ```01-echo-alpha/``` with ```./run.sh```
* Start the echo-service beta in ```02-echo-beta/``` with ```./run.sh```
* Start the control-plane-impl in ```03-control-plane/``` with
  ```./run.sh```
* Start Envoy Proxy in ```04-envoy/``` with ```./run.sh```
* Prepare the use case in ```05-curl-1/``` with ```./prepare.sh 0.0``` 
to see all requests forwarded to echo-service alpha, with ```./prepare.sh 0.5```
to see all requests distributed between echo-service alpha and beta, and with
```./prepare.sh 1.0``` to see all requests forwarded to echo-service beta.
* Show the use case in ```06-curl-2/``` with ```./show.sh```

After the preparation call, the control plane configures Envoy Proxy
to forward HTTP requests to echo-service alpha and beta depending upon
a parameter between 0.0 and 1.0 to ```./prepare.sh```; this can be
changed while ```./show.sh``` is running in a separate shell and has
immediate effect.

### HTTPS Front Proxy

* Start the echo-service in ```01-echo/``` with ```./run.sh```
* Start the control-plane-impl in ```02-control-plane/``` with
  ```./run.sh```
* Start Envoy Proxy in ```03-envoy/``` with ```./run.sh```
* Prepare the use case in ```04-curl-1/``` with ```./prepare.sh``` 
* Show the use case in ```05-curl-2/``` with ```./show.sh```

After the preparation call, the control plane generates a private key /
certificate pair, configures Envoy Proxy to forward mTLS-authenticated
HTTPS requests to the echo-service as HTTP, passing on certificate
information in request headers and forwarding access logs and metrics
back to the control plane. After echo-service responded to the request,
Envoy Proxy is configured to add a HTTP header to the response telling
which upstream host answered the response.

## Bugs

There is still bugs hidden somewhere between Envoy Proxy, java-control-plane
and the control-plane-impl demonstrated here, relating to an update of secrets
through ADS:

* Once the HTTPS Front proxy use case is set up, updating the secrets in
  the control plane does not result in a new secret being propagated to
  Envoy Proxy; this can be observed easily by setting up the ADS gRPC
  endpoint without TLS by updating the Envoy configuration and making
  a minor change to the control-plane-impl.

* Removing listeners and clusters in the control-plane does not result
  in them being removed in Envoy Proxy, so the reuse of listener ports
  between demos does not work as originally intended.

If you know more about this, I would be happy to hear from you!

