#!/bin/bash

export SPARK_MASTER_HOST=`hostname`

. "/docker_home/sbin/spark-config.sh"
. "/docker_home/bin/load-spark-env.sh"

mkdir -p $SPARK_MASTER_LOG
export SPARK_HOME=/spark
ln -sf /dev/stdout $SPARK_MASTER_LOG/spark-master.out

./spark/bin/spark-class org.apache.spark.deploy.master.Master -i $SPARK_MASTER_HOST -p $SPARK_MASTER_PORT --webui-port $SPARK_MASTER_UI_PORT --properties-file /docker_home/conf/master/spark-defaults.conf >>$SPARK_MASTER_LOG/spark-master.out
