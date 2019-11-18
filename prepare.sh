#!/bin/bash

# Test for availability of OpenSSL
if ! [ -x "$(command -v openssl)" ]; then
	echo "Error: openssl is not installed or not available on PATH." >&2
	exit 1
fi

# Test for availability of Apache Maven
if ! [ -x "$(command -v mvn)" ]; then
	echo "Error: mvn is not installed or not available on PATH." >&2
	exit 1
fi

echo "Preparing demo..."

# Prepare temporary directory
mkdir tmp

# Prepare private key / self-signed certificate for Control Plane
openssl req -x509 -newkey rsa:4096 -keyout tmp/control-plane.key -out tmp/control-plane.cert -days 365 -nodes -subj '/CN=localhost'
cp tmp/control-plane.* demo/01-http-front-proxy/02-control-plane/etc
cp tmp/control-plane.* demo/02-incremental-deployment/03-control-plane/etc
cp tmp/control-plane.* demo/03-https-front-proxy/02-control-plane/etc
rm tmp/control-plane.key tmp/control-plane.cert

# Prepare private key / self-signed certificate for Envoy Proxy instances
openssl req -x509 -newkey rsa:4096 -keyout tmp/envoy.key -out tmp/envoy.cert -days 365 -nodes -subj '/CN=localhost'
cp tmp/envoy.* demo/01-http-front-proxy/03-envoy/etc
cp tmp/envoy.* demo/02-incremental-deployment/04-envoy/etc
cp tmp/envoy.* demo/03-https-front-proxy/03-envoy/etc
rm tmp/envoy.key tmp/envoy.cert

# Prepare private key / self-signed certificate for HTTPS client
openssl req -x509 -newkey rsa:4096 -keyout tmp/client.key -out tmp/client.cert -days 365 -nodes -subj '/CN=localhost'
cp tmp/client.* demo/03-https-front-proxy/05-curl-2/etc
cp tmp/client.cert demo/03-https-front-proxy/02-control-plane/etc
rm tmp/client.key tmp/client.cert

# Remove temporary directory
rmdir tmp

# Prepare echo-service
cd sources/echo-service
mvn package
cp target/echo-service-*.jar ../../demo/01-http-front-proxy/01-echo/
cp target/echo-service-*.jar ../../demo/02-incremental-deployment/01-echo-alpha/
cp target/echo-service-*.jar ../../demo/02-incremental-deployment/02-echo-beta/
cp target/echo-service-*.jar ../../demo/03-https-front-proxy/01-echo/
cd ../..

# Prepare control-plane-impl
cd sources/control-plane-impl
mvn package
cp target/control-plane-*.jar ../../demo/01-http-front-proxy/02-control-plane
cp target/control-plane-*.jar ../../demo/02-incremental-deployment/03-control-plane
cp target/control-plane-*.jar ../../demo/03-https-front-proxy/02-control-plane
cd ../..

# Prepare envoy binary
cp resources/envoy demo/01-http-front-proxy/03-envoy/bin
cp resources/envoy demo/02-incremental-deployment/04-envoy/bin
cp resources/envoy demo/03-https-front-proxy/03-envoy/bin
