
# Apply Model on Stream

Additional Tags: 

Tags to be removed:

Operator Key: streaming:rm_model_applier

Group: 

## Description

This operator makes the application of RapidMiner models on supported data streaming platforms available.
The model will be applied on every single record arriving from the input stream.

This is a streaming operator and needs to be placed inside a Streaming Nest operator.
The operator defines the logical functionality and can be used in all streaming analytic workflow for any supported streaming platform (currently Flink and Spark).
The actual implementation used depends on the type of connection connected to the Streaming Nest operator in which this operator is placed.

Keep in mind, that the cluster needs to be properly set up in advance (e.g.: RapidMiner on Flink), and you need to take the implications of running RapidMiner models, potentially in parallel, into account.
This operation is on a best effort basis!

## Tutorial Process

#### Tutorial 1 (Apply Model on Stream example)

In this tutorial process the usage of the Apply Model on Stream operator is demonstrated.

```xml
<process version="9.9.000">
    <context>
        <input/>
        <output/>
        <macros/>
    </context>
    <operator activated="true" class="process" compatibility="9.4.000" expanded="true" name="Process" origin="GENERATED_TUTORIAL">
        <parameter key="logverbosity" value="init"/>
        <parameter key="random_seed" value="2001"/>
        <parameter key="send_mail" value="never"/>
        <parameter key="notification_email" value=""/>
        <parameter key="process_duration_for_mail" value="30"/>
        <parameter key="encoding" value="SYSTEM"/>
        <process expanded="true">
            <operator activated="true" class="retrieve" compatibility="9.9.000" expanded="true" height="68" name="Retrieve Freya Flink Cluster" width="90" x="45" y="34">
                <parameter key="repository_entry" value="/Connections/Freya Flink Cluster"/>
            </operator>
            <operator activated="true" class="streaming:streaming_nest" compatibility="0.4.000-SNAPSHOT" expanded="true" height="82" name="Streaming Nest Flink Spring" width="90" x="179" y="34">
                <parameter key="job_name" value="PoC Job Model Deployment"/>
                <process expanded="true">
                    <operator activated="true" class="retrieve" compatibility="9.9.000" expanded="true" height="68" name="Retrieve Polynomial" width="90" x="45" y="238">
                        <parameter key="repository_entry" value="//Samples/data/Polynomial"/>
                    </operator>
                    <operator activated="true" class="concurrency:parallel_random_forest" compatibility="9.9.000" expanded="true" height="103" name="Random Forest" width="90" x="380" y="238">
                        <parameter key="number_of_trees" value="100"/>
                        <parameter key="criterion" value="least_square"/>
                        <parameter key="maximal_depth" value="10"/>
                        <parameter key="apply_pruning" value="false"/>
                        <parameter key="confidence" value="0.1"/>
                        <parameter key="apply_prepruning" value="false"/>
                        <parameter key="minimal_gain" value="0.01"/>
                        <parameter key="minimal_leaf_size" value="2"/>
                        <parameter key="minimal_size_for_split" value="4"/>
                        <parameter key="number_of_prepruning_alternatives" value="3"/>
                        <parameter key="random_splits" value="false"/>
                        <parameter key="guess_subset_ratio" value="true"/>
                        <parameter key="subset_ratio" value="0.2"/>
                        <parameter key="voting_strategy" value="confidence vote"/>
                        <parameter key="use_local_random_seed" value="false"/>
                        <parameter key="local_random_seed" value="1992"/>
                        <parameter key="enable_parallel_execution" value="true"/>
                    </operator>
                    <operator activated="true" class="retrieve" compatibility="9.9.000" expanded="true" height="68" name="Retrieve Freya Kafka Cluster Docker" width="90" x="45" y="34">
                        <parameter key="repository_entry" value="/Connections/Freya Kafka Cluster Docker"/>
                    </operator>
                    <operator activated="true" class="multiply" compatibility="9.9.000" expanded="true" height="103" name="Multiply (2)" width="90" x="179" y="34"/>
                    <operator activated="true" class="streaming:kafka_source" compatibility="0.4.000-SNAPSHOT" expanded="true" height="68" name="Kafka Source" width="90" x="380" y="136">
                        <parameter key="topic" value="rm-test-mt-in"/>
                        <parameter key="start_from_earliest" value="false"/>
                    </operator>
                    <operator activated="true" class="streaming:rm_model_applier" compatibility="0.4.000-SNAPSHOT" expanded="true" height="82" name="Apply Model on Stream" width="90" x="581" y="136"/>
                    <operator activated="true" class="streaming:kafka_sink" compatibility="0.4.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" width="90" x="782" y="34">
                        <parameter key="topic" value="rm-test-mt-out"/>
                    </operator>
                    <connect from_op="Retrieve Polynomial" from_port="output" to_op="Random Forest" to_port="training set"/>
                    <connect from_op="Random Forest" from_port="model" to_op="Apply Model on Stream" to_port="model"/>
                    <connect from_op="Retrieve Freya Kafka Cluster Docker" from_port="output" to_op="Multiply (2)" to_port="input"/>
                    <connect from_op="Multiply (2)" from_port="output 1" to_op="Kafka Sink" to_port="connection"/>
                    <connect from_op="Multiply (2)" from_port="output 2" to_op="Kafka Source" to_port="connection"/>
                    <connect from_op="Kafka Source" from_port="output stream" to_op="Apply Model on Stream" to_port="input stream"/>
                    <connect from_op="Apply Model on Stream" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
                    <portSpacing port="source_in 1" spacing="0"/>
                    <portSpacing port="sink_out 1" spacing="0"/>
                </process>
            </operator>
            <connect from_op="Retrieve Freya Flink Cluster" from_port="output" to_op="Streaming Nest Flink Spring" to_port="connection"/>
            <portSpacing port="source_input 1" spacing="0"/>
            <portSpacing port="sink_result 1" spacing="0"/>
        </process>
    </operator>
</process>
```

## Parameters

#### rapidminer_home

This parameter allows you to configure the location of the RapidMiner installation for RaaP
(RapidMiner-as-a-Plugin) on the used cluster.
Notice this is cluster dependant and might change with different connections used.

#### rapidminer_user_home

This parameter allows you to set the user-home (.RapidMiner) for RaaP (RapidMiner-as-a-Plugin), for
example for additional extensions to be used.
Notice this is cluster dependant and might change with different connections used.

## Input

#### input stream (Stream Data Container)

The input of this streaming operation.
It needs to receive the output of a preceding streaming operator, to define the flow of data events in the streaming analytic workflow.

#### model (Model)

The model to apply on the stream records.

## Output

#### output stream (Stream Data Container)

The output of this streaming operation.
Connect it to the next streaming operator to define the flow of the data events in the designed streaming analytic workflow.