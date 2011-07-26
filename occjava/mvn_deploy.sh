#!/bin/sh
mvn install:install-file -DgroupId=org.jcae -DartifactId=occjava -Dversion=0.17-SNAPSHOT -Dfile=lib/occjava.jar -Dpackaging=jar
