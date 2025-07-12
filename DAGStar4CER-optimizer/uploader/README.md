# Description

This module contains methods that interact with Spar, Flink and Yarn REST API.

### Run an uploader class to submit a JAR to a Resource Manager(RM).

## Standard POST request

    curl -X POST -d http://master-host:6066/v1/submissions/create --header "Content-Type:application/json" --data '{
      "action": "CreateSubmissionRequest",
      "appResource": "file:///localhost:9000/user/spark-examples_2.11-2.0.0.jar",                   
      "clientSparkVersion": "2.0.0",
      "appArgs": [ "10" ],
      "environmentVariables" : {
        "SPARK_ENV_LOADED" : "1"
      },
      "mainClass": "org.apache.spark.examples.SparkPi",
      "sparkProperties": {
        "spark.jars": "hdfs://localhost:9000/user/spark-examples_2.11-2.0.0.jar",
        "spark.driver.supervise":"false",
        "spark.executor.memory": "512m",
        "spark.driver.memory": "512m",
        "spark.submit.deployMode":"cluster",
        "spark.app.name": "SparkPi",
        "spark.master": "spark://master-host:6066"
      }
    }

To kill a submitted app curl -X POST http://spark-cluster-ip:6066/v1/submissions/kill/driver-20170206232033-0003 to get status
curl http://spark-in-action:6066/v1/submissions/status/driver-20170206232033-0003

    Need to have "spark.jars": "hdfs://localhost:9000/user/spark-examples_2.11-2.0.0.jar", and
    "environmentVariables" : {
    "SPARK_ENV_LOADED" : "1"
    },