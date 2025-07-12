
# Spring Complex Event Forecasting Engine

Additional Tags: 

Tags to be removed:

Operator Key: streaming:spring_cef

Group: infore_project/partner_services

## Description

This operator deploys the Spring Complex Event Forecasting Engine (CEF) on the provided Flink cluster and uses it to perform complex event forecasting on financial, streamed data events in a streaming analytic workflow.

This operators first deploys the Spring CEF Engine to the Flink cluster provided at the *flink-connection* input port.
During start of the engine the required parameters are provided to the CEF by the operator.
In addition, the required topics (*input* and *output*) on the kafka cluster for communication with the engine are created as well.

The operator pushes the data events received at the *input stream* port to the *input* kafka topic.
The Spring CEF Engine performs the complex event forecasting

The operator reads from the *output* topic of the kafka cluster and pushes the detected complex events further downstream (to the *output stream* port).

This is a streaming operator and needs to be placed inside a Streaming Nest or a Streaming Optimization operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator is placed.

## Tutorial Process

#### Tutorial 1 (Simple Spring CEF)

In this tutorial process the usage of the Spring Complex Event Forecasting Engine operator is demonstrated.

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
      <operator activated="true" class="retrieve" compatibility="9.8.001" expanded="true" height="68" name="Retrieve Flink Cluster" width="90" x="179" y="34">
        <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster"/>
      </operator>
      <operator activated="true" class="streaming:streaming_nest" compatibility="0.3.000-SNAPSHOT" expanded="true" height="82" name="Streaming Nest" width="90" x="380" y="34">
        <parameter key="job_name" value="test job"/>
        <process expanded="true">
          <operator activated="true" class="retrieve" compatibility="9.8.001" expanded="true" height="68" name="Retrieve Kafka Cluster (2)" width="90" x="45" y="34">
            <parameter key="repository_entry" value="//Local Repository/Connections/Kafka Cluster"/>
          </operator>
          <operator activated="true" class="multiply" compatibility="9.8.001" expanded="true" height="124" name="Multiply" width="90" x="179" y="34"/>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.3.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" width="90" x="313" y="340">
            <parameter key="topic" value="input_labeled"/>
            <parameter key="start_from_earliest" value="false"/>
            <description align="center" color="transparent" colored="false" width="126">Receive the input (stock) data</description>
          </operator>
          <operator activated="true" class="retrieve" compatibility="9.8.001" expanded="true" height="68" name="Retrieve Kafka - Internal Communication" width="90" x="313" y="85">
            <parameter key="repository_entry" value="//Local Repository/Connections/Kafka - Internal Communication"/>
          </operator>
          <operator activated="true" class="retrieve" compatibility="9.8.001" expanded="true" height="68" name="Retrieve Flink Cluster 2" width="90" x="313" y="187">
            <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster 2"/>
            <description align="center" color="transparent" colored="false" width="126">This is the Flink cluster we want to deploy the CEF on.</description>
          </operator>
          <operator activated="true" class="streaming:spring_cef" compatibility="0.3.000-SNAPSHOT" expanded="true" height="103" name="Spring Complex Event Forecasting Engine" width="90" x="581" y="187">
            <description align="center" color="transparent" colored="false" width="126">Use the Spring Complex Event Forecasting engine to compute the forecasted values</description>
          </operator>
          <operator activated="true" class="streaming:kafka_sink" compatibility="0.3.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" width="90" x="849" y="34">
            <parameter key="topic" value="output"/>
            <description align="center" color="transparent" colored="false" width="126">Push output (forecasted) events to the output kafka topic</description>
          </operator>
          <connect from_op="Retrieve Kafka Cluster (2)" from_port="output" to_op="Multiply" to_port="input"/>
          <connect from_op="Multiply" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
          <connect from_op="Kafka Source" from_port="output stream" to_op="Spring Complex Event Forecasting Engine" to_port="input stream"/>
          <connect from_op="Retrieve Kafka - Internal Communication" from_port="output" to_op="Spring Complex Event Forecasting Engine" to_port="kafka-connection"/>
          <connect from_op="Retrieve Flink Cluster 2" from_port="output" to_op="Spring Complex Event Forecasting Engine" to_port="flink-connection"/>
          <connect from_op="Spring Complex Event Forecasting Engine" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
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

#### pattern_length

Length of the estimated pattern.

#### step_length

Step length that is used in historical data. Usually: 1

#### forecast_horizon

The length of the projection into the future.

#### precision

The minimum correlation value to detect patterns.

#### length

Training example size (i.e. first N stocks will be used for initialisation).

#### input topic

Name of the Kafka topic used by the Spring CEF Engine to receive input data events

#### output topic

Name of the Kafka topic used by the Spring CEF Engine to push output data events to.

#### job jar

Path to the .jar file of the Spring CEF Engine.

#### parallelism

Parallelism of the Spring CEF Flink job.


## Input

#### kafka-connection (Connection)

The connection to the Kafka Cluster which is used by the CEF Engine for communication.

#### flink-connection (Connection)

The connection to the Flink Cluster which on which the CEF Engine shall be deployed.

#### input stream (Stream Data Container)

The input of this streaming operation.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.

## Output

#### output stream (Stream Data Container)

The output of this streaming operation.
Connect it to the next streaming operator to define the flow of the data events in the designed streaming analytic workflow.