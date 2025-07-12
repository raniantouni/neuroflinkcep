
# Get Quote Symbols

Additional Tags: 

Tags to be removed:

Operator Key: streaming:get_quote_symbols

Group: infore_project/financial_server

## Description

This operator fetches a list of supported quote symbols from the Spring Financial server.

This operator will connect and fetch the list of quote symbols from the Spring Financial Server. 
Symbols from all stock exchanges supported by the server are received.
This operator is intended for examining and collecting data in RapidMiner. 
From the output of this operator, a particular symbol can be used in a streaming process to continuously process real-time quotes or depths by using the Quote or Depth Stream operator.

## Tutorial Process

#### Tutorial 1 (Getting quote symbols from supported stock exchanges)

This tutorial demonstrates fetching quote symbols from the Spring Financial data server.
As first step, please use your credentials to create a connection and point retrieve operator to your connection.
Contact Spring Technologies to receive credentials.

```xml
<process version="9.6.000">
                    <context>
                        <input/>
                        <output/>
                        <macros/>
                    </context>
                    <operator activated="true" class="process" compatibility="9.6.000" expanded="true" name="Process"
                              origin="GENERATED_TUTORIAL">
                        <parameter key="logverbosity" value="init"/>
                        <parameter key="random_seed" value="2001"/>
                        <parameter key="send_mail" value="never"/>
                        <parameter key="notification_email" value=""/>
                        <parameter key="process_duration_for_mail" value="30"/>
                        <parameter key="encoding" value="SYSTEM"/>
                        <process expanded="true">
                            <operator activated="true" class="retrieve" compatibility="9.6.000" expanded="true"
                                      height="68" name="Retrieve MySpringConnection" origin="GENERATED_TUTORIAL"
                                      width="90" x="112" y="85">
                                <parameter key="repository_entry" value="/Connections/MySpringConnection"/>
                                <description align="center" color="transparent" colored="false" width="126">Create your
                                    own connection object
                                </description>
                            </operator>
                            <operator activated="true" class="streaming:get_quote_symbols" compatibility="0.2.000"
                                      expanded="true" height="68" name="Get Quote Symbols" origin="GENERATED_TUTORIAL"
                                      width="90" x="246" y="85">
                                <parameter key="stream time (ms)" value="5000"/>
                                <description align="center" color="transparent" colored="false" width="126">fetch quote
                                    symbols
                                </description>
                            </operator>
                            <connect from_op="Retrieve MySpringConnection" from_port="output" to_op="Get Quote Symbols"
                                     to_port="con"/>
                            <connect from_op="Get Quote Symbols" from_port="example set symbols" to_port="result 1"/>
                            <portSpacing port="source_input 1" spacing="0"/>
                            <portSpacing port="sink_result 1" spacing="0"/>
                            <portSpacing port="sink_result 2" spacing="0"/>
                        </process>
                    </operator>
                </process>
```

## Parameters

#### stream time (ms)

Time (in milliseconds) to receive streaming data, which is a list of quote symbols (for all supported stock exchanges).


## Input

#### con (Connection)

Connection to the Spring Financial data server.

## Output

#### collection of real time depths (com.rapidminer.operator.IOObjectCollection)

ExampleSet representing a list of quote symbols.