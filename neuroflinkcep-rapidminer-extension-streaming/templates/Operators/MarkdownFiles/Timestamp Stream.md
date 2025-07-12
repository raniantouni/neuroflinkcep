
# Timestamp Stream

Additional Tags: 

Tags to be removed:

Operator Key: streaming:timestamp

Group: transformations

## Description

This operator puts a timestamp on the events of a stream (assigns it to the key provided as parameter).

This is a streaming operator and needs to be placed inside a Streaming Nest or a Streaming Optimization operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator gets placed.

## Tutorial Process

#### Tutorial 1 (Simple Timestamp Stream)

This tutorial process demonstrates the usage of the Timestamp Stream operator.

```xml
<process version="9.8.000">
    <context>
        <input/>
        <output/>
        <macros/>
    </context>
    <operator activated="true" class="process" compatibility="9.8.000" expanded="true" name="Process" origin="GENERATED_TUTORIAL">
        <parameter key="logverbosity" value="init"/>
        <parameter key="random_seed" value="2001"/>
        <parameter key="send_mail" value="never"/>
        <parameter key="notification_email" value=""/>
        <parameter key="process_duration_for_mail" value="30"/>
        <parameter key="encoding" value="SYSTEM"/>
        <process expanded="true">
            <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Flink Cluster" origin="GENERATED_TUTORIAL" width="90" x="45" y="34">
                <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster"/>
            </operator>
            <operator activated="true" class="streaming:streaming_nest" compatibility="0.5.000-SNAPSHOT" expanded="true" height="82" name="Streaming Nest" origin="GENERATED_TUTORIAL" width="90" x="179" y="34">
                <parameter key="job_name" value="test job"/>
                <process expanded="true">
                    <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Kafka Cluster" origin="GENERATED_TUTORIAL" width="90" x="45" y="34">
                        <parameter key="repository_entry" value="//Local Repository/Connections/Kafka Cluster"/>
                    </operator>
                    <operator activated="true" class="multiply" compatibility="9.8.000" expanded="true" height="103" name="Multiply" origin="GENERATED_TUTORIAL" width="90" x="179" y="34"/>
                    <operator activated="true" class="streaming:kafka_source" compatibility="0.5.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" origin="GENERATED_TUTORIAL" width="90" x="313" y="136">
                        <parameter key="topic" value="input"/>
                        <parameter key="start_from_earliest" value="false"/>
                        <description align="center" color="transparent" colored="false" width="126">Receive input events from the input kafka topic</description>
                    </operator>
                    <operator activated="true" class="streaming:timestamp" compatibility="0.5.000-SNAPSHOT" expanded="true" height="68" name="Timestamp Stream" width="90" x="447" y="136">
                        <parameter key="key" value="timestamp"/>
                        <parameter key="format_as_number" value="false"/>
                        <parameter key="date_format" value="yyyy-MM-dd'T'HH:mm:ss.SSSZ"/>
                    </operator>
                    <operator activated="true" class="streaming:kafka_sink" compatibility="0.5.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" origin="GENERATED_TUTORIAL" width="90" x="648" y="34">
                        <parameter key="topic" value="output"/>
                        <description align="center" color="transparent" colored="false" width="126">Push output events to the output kafka topic</description>
                    </operator>
                    <connect from_op="Retrieve Kafka Cluster" from_port="output" to_op="Multiply" to_port="input"/>
                    <connect from_op="Multiply" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
                    <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
                    <connect from_op="Kafka Source" from_port="output stream" to_op="Timestamp Stream" to_port="input stream"/>
                    <connect from_op="Timestamp Stream" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
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

## Parameters

#### key

Key, which the timestamp is to be associated with.

#### format_as_number

If true, the Unix time will be used as long value.

#### date_format

Format, which the timestamp should be serialized into.


## Input

#### input stream (Stream Data Container)

The input of this streaming operation.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.

## Output

#### output stream (Stream Data Container)

The output of this streaming operation.
Connect it to the next Streaming operator to define the flow of the data events in the designed streaming analytic workflow.