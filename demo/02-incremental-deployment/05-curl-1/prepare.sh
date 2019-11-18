#!/bin/bash

curl --verbose -X POST http://localhost:8000/incremental-deployment?completion=$1
