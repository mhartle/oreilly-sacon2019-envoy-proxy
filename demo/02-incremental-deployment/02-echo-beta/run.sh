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
	--server.id="beta" \
	--server.address=127.0.0.3 \
	--server.port=8082
