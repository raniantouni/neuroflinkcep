# Instructions 
In order to launch the requested container(s) simply cd into the correct directory and run
the following instructions to spin up/down the necessary containers.

    docker-compose up
    
    docker-compose down
    
## Cheatsheet

Find docker container IP from host:
    
    docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <container_id>

Problems with docker internal IP:

    docker run -it --add-host=host.docker.internal:$(ip route | grep docker0 | awk '{print $9}') debian bash

## Containers
Most docker-compose files contain multiple services.
    
## Instructions
Deployment commands and container configurations for each container.

## Flink
These are the default ports used by the Flink image:

The Web Client is on port 8081

JobManager RPC port 6123

TaskManagers RPC port 6122

TaskManagers Data port 6121

   
    docker-compose up
    docker-compose scale taskmanager=<N>
    
## Postgres
See the logs:

    docker logs -f my_postgres
Try running psql:

    docker exec -it my_postgres sudo -u postgres psql
hit CTRL+D to exit

Connect to 'mydb'
    
    docker exec -it my_postgres psql -U postgres -c "\c mydb"
    
## Spark 

    docker-compose up
    
## RMI extra settings

-Dcom.sun.management.jmxremote.rmi.port=9090
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.port=9090
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.local.only=false
-Djava.rmi.server.hostname=localhost    