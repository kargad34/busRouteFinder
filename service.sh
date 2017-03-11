#!/bin/bash
CLASSPATH=/usr/lib/jvm/java-1.8.0-openjdk-amd64/lib/*:./target/dependency/*:./target/busRouteFinder-1.0.jar 
java -XX:-UseParallelOldGC -Xms512m -Xmx1g -cp $CLASSPATH org.gokhanka.busprovider.busRouteFinder.App $1 $2
