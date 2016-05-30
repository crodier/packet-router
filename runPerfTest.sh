#!/usr/bin/env bash
mvn package

echo without G1GC
java -jar target/packet-router-1.0-SNAPSHOT-jar-with-dependencies.jar

#echo with G1GC
#java -server -XX:+UseG1GC -XX:MaxGCPauseMillis=2 -jar target/packet-router-1.0-SNAPSHOT-jar-with-dependencies.jar
