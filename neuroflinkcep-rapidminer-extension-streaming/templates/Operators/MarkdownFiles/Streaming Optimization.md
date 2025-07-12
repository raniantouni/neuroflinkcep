
# Streaming Optimization

Additional Tags: 

Tags to be removed:

Operator Key: streaming:streaming_optimizer_nest

Group: infore_project/partner_services

## Description

This Operator allows to design streaming analytic processes in it's subprocess and only provide a collection of connections to streaming platforms on which the
workflow can be deployed - the INFORE optimizer is then used to decide on an optimized placement of the operators in the workflow at the different platforms.

Upon execution of the Streaming Optimization operator the designed workflow (in the subprocess of the operator) is provided, along with the other required configurations, to the INFORE optimizer. The optimizer then performs an optimization of the workflow based on the provided information.

The received optimized workflow is then used to update the inner subprocess (create the
corresponding Streaming Nest operators, place the operators inside the Nests according to the optimization, restore splitted connections).
After  the inner subprocess is updated, it is executed, which causes the deployment of the streaming workflows on the different platforms.

The connection information to the Optimizer Service has to be provided. The operator also
provides the option to write the different configurations (workflow, network, dictionary, request) and the response of the optimizer
to disk. If this option is selected, the input for the update of the inner subprocess (which is
normally the response of the INFORE optimizer) is also read from disk and the updated workflow is written to disk as well. All file locations can be controlled by
corresponding parameters. This allows for easy manipulation of the interaction with the
optimizer, to test things.

The operator also provides the option to perform a dummy optimization without an actual execution of the optimized workflow.

## Tutorial Process

#### Tutorial 1 (Demonstration of the Streaming Optimization)

In this tutorial process the usage of the Streaming Optimization operator is demonstrated.

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
      <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Kafka Cluster" width="90" x="179" y="34">
        <parameter key="repository_entry" value="//Local Repository/Connections/Kafka Cluster"/>
      </operator>
      <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Flink Cluster" width="90" x="179" y="136">
        <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster"/>
      </operator>
      <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Spark Cluster" width="90" x="179" y="238">
        <parameter key="repository_entry" value="//Local Repository/Connections/Spark Cluster"/>
      </operator>
      <operator activated="true" class="collect" compatibility="9.8.000" expanded="true" height="103" name="Collect" width="90" x="380" y="85">
        <parameter key="unfold" value="false"/>
      </operator>
      <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Flink Cluster 2" width="90" x="179" y="340">
        <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster 2"/>
      </operator>
      <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Flink Cluster 3" width="90" x="179" y="442">
        <parameter key="repository_entry" value="//Local Repository/Connections/Flink Cluster 3"/>
      </operator>
      <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Retrieve Spark Cluster 2" width="90" x="179" y="544">
        <parameter key="repository_entry" value="//Local Repository/Connections/Spark Cluster 2"/>
      </operator>
      <operator activated="true" class="collect" compatibility="9.8.000" expanded="true" height="124" name="Collect (2)" width="90" x="380" y="238">
        <parameter key="unfold" value="false"/>
      </operator>
      <operator activated="true" class="streaming:streaming_optimizer_nest" compatibility="0.1.000-SNAPSHOT" expanded="true" height="124" name="Streaming Optimization" width="90" x="581" y="34">
        <parameter key="write_and_read_from_json" value="false"/>
        <parameter key="host" value="localhost"/>
        <parameter key="port" value="99999"/>
        <parameter key="user" value="testuser"/>
        <parameter key="password" value="testpass"/>
        <parameter key="optimizer_version" value="1.0"/>
        <parameter key="optimizer_algorithm" value="automatic"/>
        <parameter key="network_name" value="network1"/>
        <parameter key="dictionary_name" value="dictionary1"/>
        <parameter key="workflow_name" value="Streaming"/>
        <enumeration key="streaming_sites_names">
          <parameter key="site_name" value="Site 1"/>
          <parameter key="site_name" value="Site 2"/>
        </enumeration>
        <parameter key="dummy_optimization" value="false"/>
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
        </process>
      </operator>
      <connect from_op="Retrieve Kafka Cluster" from_port="output" to_op="Streaming Optimization" to_port="kafka connection"/>
      <connect from_op="Retrieve Flink Cluster" from_port="output" to_op="Collect" to_port="input 1"/>
      <connect from_op="Retrieve Spark Cluster" from_port="output" to_op="Collect" to_port="input 2"/>
      <connect from_op="Collect" from_port="collection" to_op="Streaming Optimization" to_port="streaming site 1"/>
      <connect from_op="Retrieve Flink Cluster 2" from_port="output" to_op="Collect (2)" to_port="input 1"/>
      <connect from_op="Retrieve Flink Cluster 3" from_port="output" to_op="Collect (2)" to_port="input 2"/>
      <connect from_op="Retrieve Spark Cluster 2" from_port="output" to_op="Collect (2)" to_port="input 3"/>
      <connect from_op="Collect (2)" from_port="collection" to_op="Streaming Optimization" to_port="streaming site 2"/>
      <portSpacing port="source_input 1" spacing="0"/>
      <portSpacing port="sink_result 1" spacing="0"/>
    </process>
  </operator>
</process>
```

## Parameters

#### restore original process

This button allows to restore the original logical process, after an optimization of the inner subprocess was already performed.

#### write and read from json

If selected the configuration json files send to the INFORE optimizer are written to disk.
Also the response of the optimizer which is used to update the inner subprocess is read from disk.
This allows for easy testing of the interaction with the INFORE optimizer

#### network (write)

Path which is used to write the Network JSON configuration file to disk, which is used by the INFORE optimizer.

Only enabled if *write and read from json* is selected.

#### dictionary (write)

Path which is used to write the Dictionary JSON configuration file to disk, which is used by the INFORE optimizer.

Only enabled if *write and read from json* is selected.

#### request (write)

Path which is used to write the Request JSON configuration file to disk, which is used by the INFORE optimizer.

Only enabled if *write and read from json* is selected.

#### workflow (write)

Path which is used to write the Workflow JSON configuration file to disk, which is used by the INFORE optimizer.

Only enabled if *write and read from json* is selected.

#### optimizer response (write)

Path which is used to write the response JSON string of the INFORE optimizer to disk.

Only enabled if *write and read from json* is selected.

#### optimizer response (read)

File containing the JSON response of the INFORE optimizer. 
This is used to update the inner subprocess instead of the actual response of the optimizer

Only enabled if *write and read from json* is selected.

#### optimized workflow (write)

Path which is used to write the JSON representation of the updated inner subprocess to disk.

Only enabled if *write and read from json* is selected.

#### host

The host of the optimizer.

#### port

The host of the optimizer.

#### user

The user name used to log into the optimizer.

#### password

The password used to log into the optimizer.

#### optimizer version

The version of the optimizer to be used.

- **1.0**: Version 1.0 of the INFORE optimizer.

#### optimizer algorithm

The algorithm of the optimizer to be used.

- **a\***: A\_Star algorithm, fast but with large memory footprint.

- **exhaustive**: The exhaustive algorithm, slow but optimal.

- **greedy**: The greedy algorithm, fast but approximate solutions.

- **automatic**: Automatic decision (recommended).

#### network name

The unique name of the dictionary used by the optimizer.

#### dictionary name

The unique name of the dictionary used by the optimizer.

#### workflow name

The unique name of the workflow used by the optimizer.

#### streaming sites names

Names of the available streaming sites.

#### dummy optimization

If selected, the operator only performs the optimization, without actually deploying the optimized workflow.


## Input

#### kafka connection (Connection)

The connection to the Kafka cluster which shall be used to connect streaming jobs deployed on different platforms.

#### streaming site (com.rapidminer.operator.IOObjectCollection)

This port is a port extender, which means if a port is connected a new *streaming site* port is created.

Each *streaming site* port expects a collection of connections to streaming platforms available at the corresponding computation site.
The name of this streaming site can be provided at the *streaming sites names* parameter.
The INFORE optimizer expects all streaming platforms of one streaming site to be physically near each other and take this into consideration when the inner subprocess is updated.

The optimized workflow will be deployed (depending on the result of the optimization) on a combination of all available streaming platforms across all provided streaming sites.

After the optimization is performed, for every streaming platform in all streaming sites, an inner port is created in the subprocess of the Streaming Optimization operator and is connected to the corresponding Streaming Nest operator.

