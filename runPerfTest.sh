#!/usr/bin/env bash
mvn package
java -server -XX:+UseG1GC -XX:MaxGCPauseMillis=2 -jar target/packet-router-1.0-SNAPSHOT-jar-with-dependencies.jar
