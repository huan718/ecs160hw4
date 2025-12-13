#!/bin/bash

cd persistence-framework
mvn clean install
cd ..

cd microservices
mvn clean install
mvn spring-boot:run &
cd ..

sleep 20 # Wait for microservices to start

cd main-app
mvn clean install
mvn exec:java
cd ..