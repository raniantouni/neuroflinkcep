# Benchmarking module

JVM container used to submit Spark/Flink jobs. Interfaces with other modules via a STOMP endpoints.


## Build

Installing the maven project also creates/updates the docker image of 'infore/benchmarking:latest'

    mvn install


## Deployment and Dependencies

Add ALL necessary dependencies in the pom.xml file.

Primary entrypoint at benchmarking.Main.java main method (don't rename the class).


## Building an up-to-date container image

Run the following command *BEFORE* the docker-compose up benchmarking


## Content mapping

Contents of these folders are visible to the python runtime.

    ./volume1 -> /usr/share/volume1  [BOUND VOLUME]
    ./logs -> /usr/share/logs  [BOUND VOLUME]
