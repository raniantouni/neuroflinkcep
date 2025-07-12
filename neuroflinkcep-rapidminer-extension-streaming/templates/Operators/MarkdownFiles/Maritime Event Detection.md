
# Maritime Event Detection

Additional Tags: 

Tags to be removed:

Operator Key: streaming:maritime_event_detection

Group: infore_project/partner_services

## Description

This operator uses the Maritime Event Detection Service to detect events on the streamed maritime data in a streaming analytic workflow.

To utilize this operators functionality an Akka cluster with the Maritime Event Detection Algorithm has to be set up.
The Kafka cluster and the names of the input and output topics used by the Maritime Event Detection has to be provided to the operator.

The operator starts the Maritime Event Detection on the Akka cluster (connection information through Maritime Connection).
Then the input data events received at the *input stream* port are pushed to the data topic of the Kafka cluster.
All input data events need to have the mandatory fields (t, lon, lat) describing timestamp, longitude and latitude.
If the data events are coming from AIS they should contain a field named "ship", otherwise if they are coming from another source (after fusion has taken place) they should contain the fields "target" and "track".

The Maritime Event Detection will perform the event detection and pushes the detected events to the *output topic* of the Kafka cluster.

The operator reads from the output topic of the Kafka cluster and pushes the computed estimated events further downstream (to the *output stream* port).

This is a streaming operator and needs to be placed inside a Streaming Nest or a Streaming Optimization operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator is placed.

## Tutorial Process

#### Tutorial 1 (Simple Maritime Event Detection)

In this tutorial process the usage of the Maritime Event Detection operator is demonstrated.

```xml
<process version="9.8.000">
    <context>
        <input/>
        <output/>
        <macros/>
    </context>
    <operator activated="true" class="process" compatibility="9.10.000" expanded="true" name="Process" origin="GENERATED_TUTORIAL">
        <parameter key="logverbosity" value="init"/>
        <parameter key="random_seed" value="2001"/>
        <parameter key="send_mail" value="never"/>
        <parameter key="notification_email" value=""/>
        <parameter key="process_duration_for_mail" value="30"/>
        <parameter key="encoding" value="SYSTEM"/>
        <process expanded="true">
            <operator activated="true" class="retrieve" compatibility="9.10.000" expanded="true" height="68" name="Retrieve Flink Cluster" origin="GENERATED_TUTORIAL" width="90" x="179" y="34">
                <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster"/>
            </operator>
            <operator activated="true" class="streaming:streaming_nest" compatibility="0.6.000-SNAPSHOT" expanded="true" height="82" name="Streaming Nest" origin="GENERATED_TUTORIAL" width="90" x="380" y="34">
                <parameter key="job_name" value="test job"/>
                <process expanded="true">
                    <operator activated="true" class="retrieve" compatibility="9.10.000" expanded="true" height="68" name="Retrieve Kafka Cluster (2)" origin="GENERATED_TUTORIAL" width="90" x="45" y="34">
                        <parameter key="repository_entry" value="//Local Repository/Connections/Kafka Cluster"/>
                    </operator>
                    <operator activated="true" class="multiply" compatibility="9.10.000" expanded="true" height="103" name="Multiply" origin="GENERATED_TUTORIAL" width="90" x="179" y="34"/>
                    <operator activated="true" class="streaming:kafka_source" compatibility="0.6.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" origin="GENERATED_TUTORIAL" width="90" x="313" y="238">
                        <parameter key="topic" value="input"/>
                        <parameter key="start_from_earliest" value="false"/>
                        <description align="center" color="transparent" colored="false" width="126">Receive input events from the input kafka topic</description>
                    </operator>
                    <operator activated="true" class="retrieve" compatibility="9.10.000" expanded="true" height="68" name="Retrieve Kafka - Internal Communication" origin="GENERATED_TUTORIAL" width="90" x="313" y="136">
                        <parameter key="repository_entry" value="//Local Repository/Connections/Kafka - Internal Communication"/>
                    </operator>
                    <operator activated="true" class="retrieve" compatibility="9.10.000" expanded="true" height="68" name="Retrieve MarineTraffic" width="90" x="447" y="85">
                        <parameter key="repository_entry" value="//Local Repository/Connections/MarineTraffic"/>
                    </operator>
                    <operator activated="true" class="streaming:maritime_event_detection" compatibility="0.6.000-SNAPSHOT" expanded="true" height="103" name="Maritime Event Detection" origin="GENERATED_TUTORIAL" width="90" x="581" y="238">
                        <parameter key="input_topic" value="input"/>
                        <parameter key="output_topic" value="output"/>
                        <description align="center" color="transparent" colored="false" width="126">Perform the Maritime Event Detection on the input data</description>
                    </operator>
                    <operator activated="true" class="streaming:kafka_sink" compatibility="0.6.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" origin="GENERATED_TUTORIAL" width="90" x="849" y="34">
                        <parameter key="topic" value="output"/>
                        <description align="center" color="transparent" colored="false" width="126">Push output events to the output kafka topic</description>
                    </operator>
                    <connect from_op="Retrieve Kafka Cluster (2)" from_port="output" to_op="Multiply" to_port="input"/>
                    <connect from_op="Multiply" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
                    <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
                    <connect from_op="Kafka Source" from_port="output stream" to_op="Maritime Event Detection" to_port="input stream"/>
                    <connect from_op="Retrieve Kafka - Internal Communication" from_port="output" to_op="Maritime Event Detection" to_port="kafka-connection"/>
                    <connect from_op="Retrieve MarineTraffic" from_port="output" to_op="Maritime Event Detection" to_port="maritime-connection"/>
                    <connect from_op="Maritime Event Detection" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
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

#### input topic

Name of the Kafka topic used by the Maritime Event Detection algorithm to receive input data events

#### output topic

Name of the Kafka topic used by the Maritime Event Detection algorithm to push output data events to.

#### key field

Name of key field to use for Kafka records.


## Input

#### maritime-connection (Connection)

The connection to the Maritime Akka Cluster to start the Maritime Event Detection service.

#### kafka-connection (Connection)

The connection to the Kafka Cluster which is used by the Maritime Event Detection Service for communication.

#### input stream (Stream Data Container)

The input of this streaming operation.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.

## Output

#### output stream (Stream Data Container)

The output of this streaming operation.
Connect it to the next Streaming operator to define the flow of the data events in the designed streaming analytic workflow.