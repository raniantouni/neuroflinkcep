
# Duplicate Stream

Additional Tags: 

Tags to be removed:

Operator Key: streaming:duplicate

Group: flow_control

## Description

This operator duplicates an input data stream in a streaming analytic workflow.

This is a streaming operator and needs to be placed inside a Streaming Nest or a Streaming Optimization operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator is placed.

## Tutorial Process

#### Tutorial 1 (Simple Duplicate Stream)

In this tutorial process the usage of the Duplicate Stream operator is demonstrated.

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
          <operator activated="true" class="streaming:kafka_source" compatibility="0.1.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" width="90" x="313" y="136">
            <parameter key="topic" value="input"/>
            <parameter key="start_from_earliest" value="false"/>
            <description align="center" color="transparent" colored="false" width="126">Receive input events from the input kafka topic</description>
          </operator>
          <operator activated="true" class="streaming:duplicate" compatibility="0.1.000-SNAPSHOT" expanded="true" height="82" name="Duplicate Stream" width="90" x="648" y="85">
            <description align="center" color="transparent" colored="false" width="126">Duplicate the input stream</description>
          </operator>
          <operator activated="true" class="streaming:kafka_sink" compatibility="0.1.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink (2)" width="90" x="849" y="238">
            <parameter key="topic" value="output"/>
            <description align="center" color="transparent" colored="false" width="126">Push output events to the second output kafka topic</description>
          </operator>
          <operator activated="true" class="streaming:kafka_sink" compatibility="0.1.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" width="90" x="849" y="34">
            <parameter key="topic" value="output"/>
            <description align="center" color="transparent" colored="false" width="126">Push output events to the first output kafka topic</description>
          </operator>
          <connect from_op="Retrieve Kafka Cluster (2)" from_port="output" to_op="Multiply" to_port="input"/>
          <connect from_op="Multiply" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 3" to_op="Kafka Sink (2)" to_port="connection"/>
          <connect from_op="Kafka Source" from_port="output stream" to_op="Duplicate Stream" to_port="input stream"/>
          <connect from_op="Duplicate Stream" from_port="output stream 1" to_op="Kafka Sink" to_port="input stream"/>
          <connect from_op="Duplicate Stream" from_port="output stream 2" to_op="Kafka Sink (2)" to_port="input stream"/>
          <portSpacing port="source_in 1" spacing="0"/>
          <portSpacing port="sink_out 1" spacing="0"/>
        </process>
        <description align="center" color="transparent" colored="false" width="126">Deploy the designed Streaming Analytic process on the provided Flink Cluster.&lt;br&gt;</description>
      </operator>
      <connect from_op="Retrieve Flink Cluster" from_port="output" to_op="Streaming Nest" to_port="connection"/>
      <portSpacing port="source_input 1" spacing="0"/>
      <portSpacing port="sink_result 1" spacing="0"/>
    </process>
  </operator>
</process>
```

## Input

#### input stream (Stream Data Container)

The input stream to be duplicated.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.

## Output

#### output stream 1 (Stream Data Container)

The first output stream.
All events of the input stream will be passed through.
Connect it to the next Streaming operator to define the flow of the data events in the designed streaming analytic workflow.

#### output stream 2 (Stream Data Container)

The second output stream.
All events of the input stream will be passed through.
Connect it to the next Streaming operator to define the flow of the data events in the designed streaming analytic workflow.