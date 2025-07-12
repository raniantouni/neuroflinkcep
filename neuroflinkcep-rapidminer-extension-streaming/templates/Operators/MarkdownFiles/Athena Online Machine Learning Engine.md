
# Athena Online Machine Learning Engine

Additional Tags: 

Tags to be removed:

Operator Key: streaming:athena_oml

Group: infore_project/partner_services

## Description

This operator deploys the Online Machine Learning Engine (OML) on the provided Flink cluster and uses it to train and test a machine learning (ML) model on streamed data events in a streaming analytic workflow - the operator is also able to use the deployed OML Service to score unlabeled streamed date events with the trained ML model

This operators first deploys the OML Engine (developed by the Athena Research Center, jar needs to be provided) to the Flink cluster provided at the *flink-connection* input port.
In addition the required topics (*training, forecast input, forecast output, request, response, parameter message*) on the kafka cluster for communication with the OML Engine are created as well.

The operator pushes the data events received at the *training input* port and the *input stream* port to the corresponding kafka topic.
Configuration and requests messages are send to the corresponding topics by the operator as well.
The OML Engine trains a ML model on the *training input* and scores the data events on the *forecast input* with the trained model.

The operator reads from the *forecast output* topic of the kafka cluster and pushes the computed scored/forecasted events further downstream (to the *output stream* port).

This is a streaming operator and needs to be placed inside a Streaming Nest or a Streaming Optimization operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator is placed.

## Tutorial Process

#### Tutorial 1 (Simple OML)

In this tutorial process the usage of the Athena Online Machine Learning Engine operator is demonstrated.

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
          <operator activated="true" class="streaming:kafka_source" compatibility="0.1.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source (2)" width="90" x="313" y="493">
            <parameter key="topic" value="input_unlabeled"/>
            <parameter key="start_from_earliest" value="false"/>
            <description align="center" color="transparent" colored="false" width="126">Receive unlabeled input events from the input kafka topic</description>
          </operator>
          <operator activated="true" class="streaming:kafka_source" compatibility="0.1.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" width="90" x="313" y="340">
            <parameter key="topic" value="input_labeled"/>
            <parameter key="start_from_earliest" value="false"/>
            <description align="center" color="transparent" colored="false" width="126">Receive labeled input events from the input kafka topic</description>
          </operator>
          <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Kafka - Internal Communication" width="90" x="313" y="85">
            <parameter key="repository_entry" value="//Local Repository/Connections/Kafka - Internal Communication"/>
          </operator>
          <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Flink Cluster 2" width="90" x="313" y="187">
            <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster 2"/>
            <description align="center" color="transparent" colored="false" width="126">This is the Flink cluster we want to deploy the CEF on.</description>
          </operator>
          <operator activated="true" class="streaming:athena_oml" compatibility="0.1.000-SNAPSHOT" expanded="true" height="124" name="Athena Online Machine Learning Engine" width="90" x="514" y="289">
            <parameter key="training_topic" value="training"/>
            <parameter key="forecast_input_topic" value="forecast_input"/>
            <parameter key="forecast_output_topic" value="forecast_output"/>
            <parameter key="request_topic" value="request"/>
            <parameter key="response_topic" value="response"/>
            <parameter key="parameter_message_topic" value="parameter message"/>
            <parameter key="learner" value="SVM"/>
            <list key="learner_hyper_parameters"/>
            <list key="learner_parameters"/>
            <list key="preprocessor_hyper_parameters"/>
            <list key="preprocessor_parameters"/>
            <list key="training_configuration"/>
            <parameter key="job_jar" value="oml.jar"/>
            <description align="center" color="transparent" colored="false" width="126">Use the OML Service to train a ML model on the labeled data.&lt;br/&gt;&lt;br/&gt;Use this model to score the unlabeled data</description>
          </operator>
          <operator activated="true" class="streaming:kafka_sink" compatibility="0.1.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" width="90" x="849" y="34">
            <parameter key="topic" value="output"/>
            <description align="center" color="transparent" colored="false" width="126">Push output (scored) events to the output kafka topic</description>
          </operator>
          <connect from_op="Retrieve Kafka Cluster (2)" from_port="output" to_op="Multiply" to_port="input"/>
          <connect from_op="Multiply" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
          <connect from_op="Multiply" from_port="output 3" to_op="Kafka Source (2)" to_port="connection"/>
          <connect from_op="Kafka Source (2)" from_port="output stream" to_op="Athena Online Machine Learning Engine" to_port="input stream"/>
          <connect from_op="Kafka Source" from_port="output stream" to_op="Athena Online Machine Learning Engine" to_port="training input"/>
          <connect from_op="Retrieve Kafka - Internal Communication" from_port="output" to_op="Athena Online Machine Learning Engine" to_port="kafka-connection"/>
          <connect from_op="Retrieve Flink Cluster 2" from_port="output" to_op="Athena Online Machine Learning Engine" to_port="flink-connection"/>
          <connect from_op="Athena Online Machine Learning Engine" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
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

#### training topic

Name of the Kafka topic used by the OML engine to receive training data events.

#### forecast input topic

Name of the Kafka topic used by the OML engine to receive input data events to be forecasted/scored.

#### forecast output topic

Name of the Kafka topic used by the SDE Service to push forecasted/scored output data events to.

#### request topic

Name of the Kafka topic used by the OML engine to receive request messages.

#### response topic

Name of the Kafka topic used by the SDE Service to push response messages to.

#### parameter message topic

Name of the Kafka topic used by the OML engine to receive parameter messages.

#### learner

Name of the ML Model learner used.

#### learner hyper parameters

Hyper parameters for the ML Model learner.

#### learner parameters

Parameters for the ML Model learner.

#### preprocessor hyper parameters

Hyper parameters for the Preprocesser.

#### preprocessor parameters

Parameters for the Preprocesser.

#### training configuration

Configuration of the training job.

#### job jar

Path to the .jar file of the OML Engine.


## Input

#### kafka-connection (Connection)

The connection to the Kafka Cluster which is used by the Online Machine Learning Engine for communication.

#### flink-connection (Connection)

The connection to the Flink Cluster which on which the Online Machine Learning Engine shall be deployed.

#### training input (Stream Data Container)

The input of the training data events.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.

#### input stream (Stream Data Container)

The input of unlabeled data events to be scored.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.

## Output

#### output stream (Stream Data Container)

The output of the scored/forecasted data events.
Connect it to the next Streaming operator to define the flow of the data events in the designed streaming analytic workflow.