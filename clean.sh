#!/bin/bash

# Test for availability of Apache Maven
if ! [ -x "$(command -v mvn)" ]; then
	echo "Error: mvn is not installed or not available on PATH." >&2
	exit 1
fi

echo 'Cleaning sources and demo...'

# Remove keys, certificates and jars from demo
find demo -name *.key | xargs --no-run-if-empty rm
find demo -name *.cert | xargs --no-run-if-empty rm
find demo -name *.jar | xargs --no-run-if-empty rm

# Remove envoy binary from demo
rm -f demo/01-http-front-proxy/03-envoy/bin/envoy
rm -f demo/02-incremental-deployment/04-envoy/bin/envoy
rm -f demo/03-https-front-proxy/03-envoy/bin/envoy

# Remove logs from control-plane directories
rm -f demo/01-http-front-proxy/02-control-plane/logs/*.log
rm -f demo/01-http-front-proxy/02-control-plane/logs/archived/*.log
rm -f demo/02-incremental-deployment/03-control-plane/logs/*.log
rm -f demo/02-incremental-deployment/03-control-plane/logs/archived/*.log
rm -f demo/03-https-front-proxy/02-control-plane/logs/*.log
rm -f demo/03-https-front-proxy/02-control-plane/logs/archived/*.log

# Remove logs from envoy directories
rm -f demo/01-http-front-proxy/03-envoy/logs/*.log
rm -f demo/02-incremental-deployment/04-envoy/logs/*.log
rm -f demo/03-https-front-proxy/03-envoy/logs/*.log

# Clean echo-service
cd sources/echo-service
mvn clean
cd ../..

# Clean control-plane-impl
cd sources/control-plane-impl
mvn clean
cd ../..


