#!/bin/bash

# Find JAR files in directory
shopt -s nullglob
JARS=(*.jar)

# Abort launch if there is no JAR file
if [ -z "${JARS[@]}" ]
then
        echo "Failed to find JAR file in directory to launch."
	exit 1
fi

# Launch first JAR file
echo "Launching ${JARS[0]}..."
java -jar "${JARS[0]}" \
	--server.address=127.0.0.1 \
	--server.port.web=8000 \
	--server.port.xds=6565 \
	--server.tls.certChain=etc/control-plane.cert \
	--server.tls.privateKey=etc/control-plane.key
