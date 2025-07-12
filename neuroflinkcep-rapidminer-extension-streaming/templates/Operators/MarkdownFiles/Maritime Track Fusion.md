
# Maritime Track Fusion

Additional Tags: 

Tags to be removed:

Operator Key: streaming:maritime_fusion

Group: infore_project/partner_services

## Description

This operator uses the Maritime Track Fusion Service to perform a fusion of different streamed maritime input data streams in a streaming analytic workflow.

To utilize this operators functionality an Akka cluster with the Track Fusion Algorithm has to be set up.
The Kafka cluster and the names of the input and output topics used by the Track Fusion has to be provided to the operator.

The operator starts the Track Fusion on the Akka cluster (connection information through Maritime Connection).
Then the input data events received at the different *input stream* ports are pushed to the data topics of the Kafka cluster.
Any number of different input data streams can be connected to the input ports of the operator. 
These different input streams can be for example the AIS data stream of ship positions, sensor data or similar.
All input data events needs to have the mandatory fields (t, lon, lat) describing timestamp, longitude and latitude. 
If the data events contain the field "ship" it is assumed that the data stream is coming from AIS, otherwise from another source/sensor.

The Maritime Event Detection will perform the fusion which combines the data from the different input streams and creates one output stream of fused events.
These fused events are pushed to the *output topic* of the Kafka cluster.
The operator reads from the output topic of the Kafka cluster and pushes the fused events further downstream (to the *output stream* port).

This is a streaming operator and needs to be placed inside a Streaming Nest or a Streaming Optimization operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator is placed.

## Tutorial Process

#### Tutorial 1 (Simple Maritime Track Fusion)

In this tutorial process the usage of the Maritime Track Fusion operator is demonstrated.

```xml
<?xml version="1.0" encoding="UTF-8"?><process version="9.10.001">
  <context>
    <input/>
    <output/>
    <macros/>
  </context>
  <operator activated="true" class="process" compatibility="9.10.001" expanded="true" name="Process">
    <parameter key="logverbosity" value="init"/>
    <parameter key="random_seed" value="2001"/>
    <parameter key="send_mail" value="never"/>
    <parameter key="notification_email" value=""/>
    <parameter key="process_duration_for_mail" value="30"/>
    <parameter key="encoding" value="SYSTEM"/>
    <process expanded="true">
      <operator activated="true" class="retrieve" compatibility="9.10.001" expanded="true" height="68" name="Retrieve Freya Flink" width="90" x="112" y="34">
        <parameter key="repository_entry" value="/Connections/Freya Flink"/>
      </operator>
      <operator activated="true" class="streaming:streaming_nest" compatibility="0.6.002-SNAPSHOT" expanded="true" height="82" name="Streaming Nest" width="90" x="313" y="34">
        <parameter key="job_name" value="Track Fusion Test"/>
        <process expanded="true">
          <operator activated="true" class="retrieve" compatibility="9.10.001" expanded="true" height="68" name="Retrieve Freya Kafka" width="90" x="112" y="136">
            <parameter key="repository_entry" value="/Connections/Freya Kafka"/>
          </operator>
          <operator activated="true" class="retrieve" compatibility="9.10.001" expanded="true" height="68" name="Retrieve Maritime Cluster" width="90" x="112" y="34">
            <parameter key="repository_entry" value="/Connections/Maritime Cluster"/>
          </operator>
          <operator activated="true" class="multiply" compatibility="9.10.001" expanded="true" height="166" name="Multiply" width="90" x="246" y="85"/>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.6.002-SNAPSHOT" expanded="true" height="82" name="Kafka Source (3)" width="90" x="380" y="340">
            <parameter key="kafka_topic" value="uav"/>
            <parameter key="start_from_earliest" value="true"/>
          </operator>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.6.002-SNAPSHOT" expanded="true" height="82" name="Kafka Source (2)" width="90" x="380" y="238">
            <parameter key="kafka_topic" value="satellite"/>
            <parameter key="start_from_earliest" value="true"/>
          </operator>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.6.002-SNAPSHOT" expanded="true" height="82" name="Kafka Source" width="90" x="380" y="136">
            <parameter key="kafka_topic" value="ais"/>
            <parameter key="start_from_earliest" value="true"/>
          </operator>
          <operator activated="true" class="streaming:maritime_fusion" compatibility="0.6.002-SNAPSHOT" expanded="true" height="166" name="Maritime Track Fusion" width="90" x="514" y="34">
            <enumeration key="input_topics">
              <parameter key="topic_name" value="ais"/>
              <parameter key="topic_name" value="satellite"/>
            </enumeration>
            <parameter key="output_topic" value="output"/>
          </operator>
          <operator activated="true" class="streaming:kafka_sink" compatibility="0.6.002-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" width="90" x="715" y="85">
            <parameter key="kafka_topic" value="output"/>
          </operator>
          <connect from_op="Retrieve Freya Kafka" from_port="output" to_op="Multiply" to_port="input"/>
          <connect from_op="Retrieve Maritime Cluster" from_port="output" to_op="Maritime Track Fusion" to_port="maritime-connection"/>
          <connect from_op="Multiply" from_port="output 1" to_op="Maritime Track Fusion" to_port="kafka-connection"/>
          <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 3" to_op="Kafka Source (2)" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 4" to_op="Kafka Source (3)" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 5" to_op="Kafka Sink" to_port="connection"/>
          <connect from_op="Kafka Source (3)" from_port="output stream" to_op="Maritime Track Fusion" to_port="input stream 3"/>
          <connect from_op="Kafka Source (2)" from_port="output stream" to_op="Maritime Track Fusion" to_port="input stream 2"/>
          <connect from_op="Kafka Source" from_port="output stream" to_op="Maritime Track Fusion" to_port="input stream 1"/>
          <connect from_op="Maritime Track Fusion" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
          <portSpacing port="source_in 1" spacing="0"/>
          <portSpacing port="sink_out 1" spacing="0"/>
        </process>
      </operator>
      <connect from_op="Retrieve Freya Flink" from_port="output" to_op="Streaming Nest" to_port="connection"/>
      <portSpacing port="source_input 1" spacing="0"/>
      <portSpacing port="sink_result 1" spacing="0"/>
    </process>
  </operator>
</process>
```

## Parameters

#### input topics

Names of the Kafka topics used by the Track Fusion algorithm to receive input data events.
If there are more input data streams connected, than topic names provided, the operator automatically creates generic topic names for the additional input data streams.

#### output topic

Name of the Kafka topic used by the Track Fusion algorithm to push fused output data events to.

#### key field

Name of key field to use for Kafka records.


## Input

#### maritime-connection (Connection)

The connection to the Maritime Akka Cluster to start the Maritime Track Fusion algorithm.

#### kafka-connection (Connection)

The connection to the Kafka Cluster which is used by the Maritime Track Fusion Service for communication.

#### input streams (Stream Data Container)

The input of this streaming operation.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.
Any number of input data streams can be connected and will be fused together.

## Output

#### output stream (Stream Data Container)

The output of this streaming operation.
Connect it to the next Streaming operator to define the flow of the data events in the designed streaming analytic workflow.