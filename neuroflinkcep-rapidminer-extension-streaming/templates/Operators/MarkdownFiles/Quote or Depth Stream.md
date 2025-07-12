
# Quote or Depth Stream

Additional Tags: 

Tags to be removed:

Operator Key: streaming:stream_quotes_depths

Group: infore_project/financial_server

## Description

This streaming operator retrieves financial data streams for quotes and depths from the Spring Financial server in real-time.

This operator fetches real-time quotes and depths streams for a given stock symbol from the Spring Financial Server. 
This operator is a streaming operator which means that it can execute as part of the stream graph created using the RapidMiner Streaming extension. 
The operator currently supports Flink as its streaming execution platform.

## Tutorial Process

#### Tutorial 1 (Get financial streams for the quote or depth of a stock in real-time)

This tutorial demonstrates how real-time quotes or depths for a symbol can be retrieved from the Spring Financial data server.
As first step, please create a connection object to Spring Financial Server using your credentials. Contact Spring Technologies to receive credentials.
You also need to create a connection object for your Flink and Kafka cluster. 
The latter is not required but is used here as a target storage to demonstrate how you can use this operator in a streaming chain of operators that retrieve real-time data from a streaming source and stores it to another streaming sink.

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
          <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Flink Connection" origin="GENERATED_TUTORIAL" width="90" x="112" y="85">
            <parameter key="repository_entry" value="/Connections/FlinkClusterConnection"/>
          </operator>
          <operator activated="true" class="streaming:streaming_nest" compatibility="0.2.000-SNAPSHOT" expanded="true" height="82" name="Streaming Nest" origin="GENERATED_TUTORIAL" width="90" x="313" y="85">
            <parameter key="job_name" value="Financial-UseCase-Job"/>
            <process expanded="true">
              <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Spring Connection" origin="GENERATED_TUTORIAL" width="90" x="112" y="136">
                <parameter key="repository_entry" value="/Connections/SpringFinancialServerConnection"/>
              </operator>
              <operator activated="true" class="streaming:stream_quotes_depths" compatibility="0.2.000-SNAPSHOT" expanded="true" height="68" name="Quote or Depth Stream" origin="GENERATED_TUTORIAL" width="90" x="246" y="136">
                <parameter key="stream_time_(ms)" value="5000"/>
                <parameter key="select_operation" value="quotes"/>
                <parameter key="disconnect_time_(ms)" value="1000"/>
              </operator>
              <operator activated="true" class="retrieve" compatibility="9.8.000" expanded="true" height="68" name="Kafka Connection" origin="GENERATED_TUTORIAL" width="90" x="514" y="85">
                <parameter key="repository_entry" value="/Connections/KafkaClusterConnection"/>
              </operator>
              <operator activated="true" class="streaming:kafka_sink" compatibility="0.2.000-SNAPSHOT" expanded="true" height="82" name="Kafka Sink" origin="GENERATED_TUTORIAL" width="90" x="715" y="187">
                <parameter key="topic" value="topic2-zk"/>
              </operator>
              <connect from_op="Spring Connection" from_port="output" to_op="Quote or Depth Stream" to_port="con"/>
              <connect from_op="Quote or Depth Stream" from_port="output stream" to_op="Kafka Sink" to_port="input stream"/>
              <connect from_op="Kafka Connection" from_port="output" to_op="Kafka Sink" to_port="connection"/>
              <portSpacing port="source_in 1" spacing="0"/>
              <portSpacing port="sink_out 1" spacing="0"/>
              <portSpacing port="sink_out 2" spacing="0"/>
              <description align="left" color="green" colored="true" height="363" resized="true" width="395" x="27" y="27">STEP 1: Retrieve L1 (Quotes) or L2 (Depths) real-time streams&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;Symbol for Quote and Depth = Eurex|846959|NoExpiry&lt;br&gt;</description>
              <description align="left" color="orange" colored="true" height="363" resized="true" width="423" x="428" y="27">STEP 2: Store L1 or L2 stream in a Kafka topic&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;Create Kafka topics for storing L1 and L2 data, e.g. as following:&lt;br&gt;Topic for Depth = topic1-zk&lt;br&gt;Topic for Quote = topic2-zk&lt;br&gt;</description>
            </process>
          </operator>
          <connect from_op="Flink Connection" from_port="output" to_op="Streaming Nest" to_port="connection"/>
          <connect from_op="Streaming Nest" from_port="out 1" to_port="result 1"/>
          <portSpacing port="source_input 1" spacing="0"/>
          <portSpacing port="sink_result 1" spacing="0"/>
          <portSpacing port="sink_result 2" spacing="0"/>
        </process>
      </operator>
    </process>
```

## Parameters

#### stock symbol

A symbol for a specific stock.

#### stream time (ms)

Time (in milliseconds) to receive streaming data. This serves as a batch of streaming records received and passed on in given time.

#### select operation

Select either quotes (Level 1 or L1 data) or depths (Level 2 or L2 data) to retrieve.

#### disconnect time (ms)

Time (in milliseconds) to gracefully disconnect from the server.

## Input

#### con (Connection)

Connection to the Spring Financial data server.

## Output

#### output stream (com.rapidminer.extension.streaming.ioobject.StreamDataContainer)

Financial stream retrieved from the Spring Financial Server.