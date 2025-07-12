#!/bin/bash

. "/docker_home/sbin/spark-config.sh"
. "/docker_home/bin/load-spark-env.sh"

mkdir -p $SPARK_WORKER_LOG
export SPARK_HOME=/spark
ln -sf /dev/stdout $SPARK_WORKER_LOG/spark-worker.out

./spark/bin/spark-class org.apache.spark.deploy.worker.Worker --properties-file /docker_home/conf/worker/spark-defaults.conf --webui-port $SPARK_WORKER_WEBUI_PORT $SPARK_MASTER >>$SPARK_WORKER_LOG/spark-worker.out
