
# Synopsis Data Engine

Additional Tags: 

Tags to be removed:

Operator Key: streaming:sde

Group: infore_project/partner_services

## Description

This operator uses the provided Synopsis Data Engine Service to compute synopsis of the streamed data events in a streaming analytic workflow.

To utilize this operators functionality a Synopsis Data Engine Service (SDE Service) has to be set up.
The Kafka cluster and the names of the request, data and output topic used by the SDE Service has to be provided to the operator.

The operator sends configuration messages to the request topic of the kafka cluster to configure the required synopsis computation.
Then the input data events received at the *input stream* port are pushed to the data topic of the Kafka cluster.
The SDE Service will compute the synopsis.
The operator will send estimate requests through the request topic to the SDE Service (frequency: *estimate frequency*) to request the resulting estimates from the SDE Service (they will be pushed to the output topic of the Kafka cluster).
If *estimate continuous* is selected, the synopsis is configured to produce continuous estimates, hence the estimate requests are not send in this case.

The operator reads from the output topic of the kafka cluster and pushes the computed estimated events further downstream (to the *output stream* port).

This is a streaming operator and needs to be placed inside a Streaming Nest or a Streaming Optimization operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator is placed.

## Tutorial Process

#### Tutorial 1 (Simple SDE)

In this tutorial process the usage of the Synopsis Data Engine operator is demonstrated.

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
            <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Flink Cluster" origin="GENERATED_TUTORIAL" width="90" x="179" y="34">
                <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster"/>
            </operator>
            <operator activated="true" class="streaming:streaming_nest" compatibility="0.3.000-SNAPSHOT" expanded="true" height="82" name="Streaming Nest" origin="GENERATED_TUTORIAL" width="90" x="380" y="34">
                <parameter key="job_name" value="test job"/>
                <process expanded="true">
                    <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Kafka Cluster (2)" origin="GENERATED_TUTORIAL" width="90" x="45" y="34">
                        <parameter key="repository_entry" value="//Local Repository/Connections/Kafka Cluster"/>
                    </operator>
                    <operator activated="true" class="multiply" compatibility="9.8.000" expanded="true" height="103" name="Multiply" origin="GENERATED_TUTORIAL" width="90" x="179" y="34"/>
                    <operator activated="true" class="streaming:kafka_source" compatibility="0.3.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" origin="GENERATED_TUTORIAL" width="90" x="313" y="238">
                        <parameter key="topic" value="input"/>
                        <parameter key="start_from_earliest" value="false"/>
                        <description align="center" color="transparent" colored="false" width="126">Receive input events from the input kafka topic</description>
                    </operator>
                    <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Kafka - Internal Communication" origin="GENERATED_TUTORIAL" width="90" x="313" y="85">
                        <parameter key="repository_entry" value="//Local Repository/Connections/Kafka - Internal Communication"/>
                    </operator>
                    <operator activated="true" class="streaming:sde" compatibility="0.3.000-SNAPSHOT" expanded="true" height="82" name="Synopsis Data Engine" origin="GENERATED_TUTORIAL" width="90" x="514" y="238">
                        <parameter key="synopsis_type" value="count min"/>
                        <parameter key="data_set_key" value="data set"/>
                        <parameter key="synopsis_params" value="2"/>
                        <parameter key="synopsis_parallelism" value="1"/>
                        <parameter key="u_id" value="1"/>
                        <parameter key="stream_id_key" value="stream_id"/>
                        <parameter key="estimate_type" value="Normal"/>
                        <parameter key="estimate_frequency" value="5"/>
                        <parameter key="estimate_params" value="2"/>
                        <parameter key="request_topic" value="request"/>
                        <parameter key="data_topic" value="data"/>
                        <parameter key="output_topic" value="output"/>
                        <description align="center" color="transparent" colored="false" width="126">Use the Synopsis Data Engine Service to compute the count min synopsis</description>
                    </operator>
                    <operator activated="true" class="streaming:kafka_sink" compatibility="0.3.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" origin="GENERATED_TUTORIAL" width="90" x="849" y="34">
                        <parameter key="topic" value="output"/>
                        <description align="center" color="transparent" colored="false" width="126">Push output events to the output kafka topic</description>
                    </operator>
                    <connect from_op="Retrieve Kafka Cluster (2)" from_port="output" to_op="Multiply" to_port="input"/>
                    <connect from_op="Multiply" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
                    <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
                    <connect from_op="Kafka Source" from_port="output stream" to_op="Synopsis Data Engine" to_port="input stream"/>
                    <connect from_op="Retrieve Kafka - Internal Communication" from_port="output" to_op="Synopsis Data Engine" to_port="connection"/>
                    <connect from_op="Synopsis Data Engine" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
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

#### synopsis type

The synopsis applied on the stream.

- **count min**: count min synopsis.

- **bloom filter**: bloom filter synopsis.

- **ams**: ams synopsis.

- **dft**: dft correlations synopsis.

- **lsh**: lsh synopsis.

- **core sets**: core sets synopsis.

- **hyper log log**: hyper log log synopsis.

- **sticky sampling**: sticky sampling synopsis.

- **lossy counting**: lossy counting synopsis.

- **chain sampler**: chain sampler synopsis.

- **gk quantiles**: gk quantiles synopsis.

- **maritime**: maritime synopsis (used for Use Case 3 of the INFORE project).

- **top k**: top k synopsis.

- **optimal distributed window sampling**: optimal distributed window sampling synopsis.

- **optimal distributed sampling**: optimal distributed sampling synopsis.

- **window quantiles**: window quantiles synopsis.

#### data set key

Data set key.

#### synopsis params

The parameters of the selected *synopsis type* (comma separated).

#### synopsis parallelism

The parallelism level of the computation of the synopsis in the SDE Service

#### u id

The unique ID used by the synopsis computation.

#### stream id key

Key in the data that should be used as StreamId.

#### estimate type

Configure the estimation type for the selected *synopsis type*.

#### estimate frequency

Frequency in which estimate requests are send to the SDE Service

#### estimate params

Parameters used in the estimate requests (comma separated).

#### request topic

Name of the Kafka topic used by the SDE Service to receive request/configuration messages.

#### data topic

Name of the Kafka topic used by the SDE Service to receive input data events

#### output topic

Name of the Kafka topic used by the SDE Service to push output data events to.


## Input

#### connection (Connection)

The connection to the Kafka Cluster which is used by the Synopsis Data Engine Service for communication.

#### input stream (Stream Data Container)

The input of this streaming operation.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.

## Output

#### output stream (Stream Data Container)

The output of this streaming operation.
Connect it to the next Streaming operator to define the flow of the data events in the designed streaming analytic workflow.