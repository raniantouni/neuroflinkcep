
# Union Stream

Additional Tags: 

Tags to be removed:

Operator Key: streaming:union

Group: flow_control

## Description

This operator takes the union of the input data streams in a streaming analytic workflow.

This is a streaming operator and needs to be placed inside a Streaming Nest or a Streaming Optimization operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator is placed.

## Tutorial Process

#### Tutorial 1 (Simple Union Stream)

In this tutorial process the usage of the Union Stream operator is demonstrated.

```xml
<process version="9.8.001">
  <context>
    <input/>
    <output/>
    <macros/>
  </context>
  <operator activated="true" class="process" compatibility="9.8.001" expanded="true" name="Process">
    <parameter key="logverbosity" value="init"/>
    <parameter key="random_seed" value="2001"/>
    <parameter key="send_mail" value="never"/>
    <parameter key="notification_email" value=""/>
    <parameter key="process_duration_for_mail" value="30"/>
    <parameter key="encoding" value="SYSTEM"/>
    <process expanded="true">
      <operator activated="true" class="retrieve" compatibility="9.8.001" expanded="true" height="68" name="Retrieve Spring Flink Cluster" width="90" x="112" y="85">
        <parameter key="repository_entry" value="/Connections/Spring Flink Cluster"/>
      </operator>
      <operator activated="true" class="streaming:streaming_nest" compatibility="0.3.000-SNAPSHOT" expanded="true" height="82" name="Streaming Nest" width="90" x="313" y="85">
        <parameter key="job_name" value="PoC Job 11"/>
        <process expanded="true">
          <operator activated="true" class="retrieve" compatibility="9.8.001" expanded="true" height="68" name="Retrieve Spring Kafka Cluster" width="90" x="45" y="34">
            <parameter key="repository_entry" value="/Connections/Spring Kafka Cluster"/>
          </operator>
          <operator activated="true" class="multiply" compatibility="9.8.001" expanded="true" height="145" name="Multiply" width="90" x="179" y="34"/>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.3.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source (3)" width="90" x="380" y="340">
            <parameter key="topic" value="union-in2"/>
            <parameter key="start_from_earliest" value="false"/>
          </operator>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.3.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source (2)" width="90" x="380" y="238">
            <parameter key="topic" value="union-in1"/>
            <parameter key="start_from_earliest" value="false"/>
          </operator>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.3.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" width="90" x="380" y="136">
            <parameter key="topic" value="2-inSpring"/>
            <parameter key="start_from_earliest" value="false"/>
          </operator>
          <operator activated="true" class="streaming:union" compatibility="0.3.000-SNAPSHOT" expanded="true" height="124" name="Union Stream" width="90" x="581" y="136"/>
          <operator activated="true" class="streaming:kafka_sink" compatibility="0.3.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" width="90" x="782" y="34">
            <parameter key="topic" value="union-out"/>
          </operator>
          <connect from_op="Retrieve Spring Kafka Cluster" from_port="output" to_op="Multiply" to_port="input"/>
          <connect from_op="Multiply" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 3" to_op="Kafka Source (2)" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 4" to_op="Kafka Source (3)" to_port="connection"/>
          <connect from_op="Kafka Source (3)" from_port="output stream" to_op="Union Stream" to_port="in stream 3"/>
          <connect from_op="Kafka Source (2)" from_port="output stream" to_op="Union Stream" to_port="in stream 2"/>
          <connect from_op="Kafka Source" from_port="output stream" to_op="Union Stream" to_port="in stream 1"/>
          <connect from_op="Union Stream" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
          <portSpacing port="source_in 1" spacing="0"/>
          <portSpacing port="sink_out 1" spacing="0"/>
        </process>
      </operator>
      <connect from_op="Retrieve Spring Flink Cluster" from_port="output" to_op="Streaming Nest" to_port="connection"/>
      <portSpacing port="source_input 1" spacing="0"/>
      <portSpacing port="sink_result 1" spacing="0"/>
    </process>
  </operator>
</process>
```

## Input

#### in stream (Stream Data Container)

The input stream(s) to be part of the union.

## Output

#### output stream (Stream Data Container)

The output (union) stream.
All events of the input streams will be passed through.
