
# Streaming Nest

Additional Tags: 

Tags to be removed:

Operator Key: streaming:streaming_nest

Group: 

## Description

This operator allows to design and deploy streaming analytic processes.

The streaming analytic process is designed in the subprocess of the Streaming Nest operator.
A streaming connection (Flink Connection or Spark Connection) has to be provided to the *connection* input port.
It defines on which cluster and technology the streaming analytic process is deployed, when the process is executed.

If an INFORE optimizer connection object is provided to the *connection* input port, the streaming workflow is not uploaded to and deployed on a streaming cluster, but the StreamGraph of the workflow is serialized and uploaded to the file server of the INFORE Optimizer Service.
This graph file can then be used to perform the benchmarking in the INFORE Optimizer.

## Tutorial Process

#### Tutorial 1 (Simple Streaming Nest)

In this tutorial process the usage of the Streaming Nest operator is demonstrated.

```xml
<process version="9.8.000">
  <context>
    <input/>
    <output/>
    <macros/>
  </context>
  <operator activated="true" class="process" compatibility="9.8.000" expanded="true" name="Process">
    <parameter key="logverbosity" value="init"/>
    <parameter key="random_seed" value="2001"/>
    <parameter key="send_mail" value="never"/>
    <parameter key="notification_email" value=""/>
    <parameter key="process_duration_for_mail" value="30"/>
    <parameter key="encoding" value="SYSTEM"/>
    <process expanded="true">
      <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Flink Cluster" width="90" x="179" y="34">
        <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster"/>
      </operator>
      <operator activated="true" class="streaming:streaming_nest" compatibility="0.1.000-SNAPSHOT" expanded="true" height="82" name="Streaming Nest" width="90" x="380" y="34">
        <parameter key="job_name" value="test job"/>
        <process expanded="true">
          <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Kafka Cluster (2)" width="90" x="45" y="34">
            <parameter key="repository_entry" value="//Local Repository/Connections/Kafka Cluster"/>
          </operator>
          <operator activated="true" class="multiply" compatibility="9.8.000" expanded="true" height="124" name="Multiply" width="90" x="179" y="34"/>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.1.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source (2)" width="90" x="313" y="289">
            <parameter key="topic" value="input2"/>
            <parameter key="start_from_earliest" value="false"/>
          </operator>
          <operator activated="true" class="streaming:aggregate" compatibility="0.1.000-SNAPSHOT" expanded="true" height="68" name="Aggregate Stream" width="90" x="447" y="289">
            <parameter key="key" value="partitionKey"/>
            <parameter key="value_key" value="test"/>
            <parameter key="window_length" value="5"/>
            <parameter key="function" value="Average"/>
          </operator>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.1.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" width="90" x="313" y="136">
            <parameter key="topic" value="input1"/>
            <parameter key="start_from_earliest" value="false"/>
          </operator>
          <operator activated="true" class="streaming:join" compatibility="0.1.000-SNAPSHOT" expanded="true" height="82" name="Join Streams" width="90" x="581" y="136">
            <parameter key="left_key" value="id"/>
            <parameter key="right_key" value="id"/>
            <parameter key="window_length" value="60"/>
          </operator>
          <operator activated="true" class="streaming:filter" compatibility="0.1.000-SNAPSHOT" expanded="true" height="68" name="Filter Stream" width="90" x="715" y="136">
            <parameter key="key" value="test"/>
            <parameter key="value" value="12"/>
            <parameter key="operator" value="Less than"/>
          </operator>
          <operator activated="true" class="streaming:kafka_sink" compatibility="0.1.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" width="90" x="849" y="34">
            <parameter key="topic" value="output"/>
          </operator>
          <connect from_op="Retrieve Kafka Cluster (2)" from_port="output" to_op="Multiply" to_port="input"/>
          <connect from_op="Multiply" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 3" to_op="Kafka Source (2)" to_port="connection"/>
          <connect from_op="Kafka Source (2)" from_port="output stream" to_op="Aggregate Stream" to_port="input stream"/>
          <connect from_op="Aggregate Stream" from_port="output stream" to_op="Join Streams" to_port="input stream 2"/>
          <connect from_op="Kafka Source" from_port="output stream" to_op="Join Streams" to_port="input stream 1"/>
          <connect from_op="Join Streams" from_port="output stream" to_op="Filter Stream" to_port="input stream"/>
          <connect from_op="Filter Stream" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
          <portSpacing port="source_in 1" spacing="0"/>
          <portSpacing port="sink_out 1" spacing="0"/>
        </process>
        <description align="center" color="transparent" colored="false" width="126">Deploy the designed Streaming Analytic process on the provided Flink Cluster.&lt;br/&gt;&lt;br/&gt;The same process can be deployed on the Spark cluster by just changing the provided connection.</description>
      </operator>
      <operator activated="false" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Spark Cluster" width="90" x="179" y="136">
        <parameter key="repository_entry" value="//Local Repository/Connections/Spark Cluster"/>
      </operator>
      <connect from_op="Retrieve Flink Cluster" from_port="output" to_op="Streaming Nest" to_port="connection"/>
      <portSpacing port="source_input 1" spacing="0"/>
      <portSpacing port="sink_result 1" spacing="0"/>
    </process>
  </operator>
</process>
```

## Parameters

#### job name

Name of the stream job.


## Input

#### connection (Connection)

The connection to the streaming cluster (flink or spark), the job shall be deployed on.

If an INFORE Optimizer connection object is provided, the StreamGraph defining the streaming workflow is serialized and uploaded to the file server of the INFORE Optimizer Service instead.

#### input (IOObject)

This port is a port extender, which means if a port is connected a new *input* port is created.
Any IOObject can be connected to the port and is passed to the corresponding inner  port.

## Output

#### output (IOObject)

This is port is a port extender, which means if a port is connected a new *output* port is created.
Any IOObject can be connected to the inner port and is passed to the corresponding outer port.