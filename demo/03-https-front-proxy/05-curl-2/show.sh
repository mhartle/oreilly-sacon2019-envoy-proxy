#!/bin/bash

curl -s --key etc/client.key --cert etc/client.cert --cacert etc/client.cert -k https://localhost:8443 | jq -C .
