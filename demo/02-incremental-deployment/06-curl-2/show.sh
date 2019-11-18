#!/bin/bash

while sleep 0.1; do curl -s http://localhost:8080 | jq -C .id; done
