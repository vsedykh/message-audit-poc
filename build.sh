#!/bin/bash
set -e
mvn clean install -DskipTests
docker build -t sourceadapter1:latest sourceadapter1/
docker build -t sinkadapter1:latest sinkadapter1/
docker build -t sinkadapter2:latest sinkadapter2/
docker build -t message-audit-ingester:latest message-audit-ingester/