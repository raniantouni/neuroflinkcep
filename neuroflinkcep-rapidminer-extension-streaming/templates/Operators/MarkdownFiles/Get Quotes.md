
# Get Quotes

Additional Tags: 

Tags to be removed:

Operator Key: streaming:get_quotes

Group: infore_project/financial_server

## Description

This operator communicates with Spring Financial server and gets a stream of real-time quotes of each input symbol.

This operator will connect and fetch real-time quotes from the Spring Financial Server for a list of input symbols.
Symbols can be received from the Get Quote Symbols operator. 
The Get Quotes operator may need some time (at least a few seconds) as the streamed data comes from different sources.
This operator outputs a collection of ExampleSets, each representing a list of real-time quotes for an input symbol(s).
The order information for each quote is provided in the annotation. This provides the reference to the stock exchange and symbol, for which the quotes are retrieved.
This operator is intended for examining and collecting data in RapidMiner. To use it as part of a streaming process, use Quote or Depth Stream operator.

## Tutorial Process

#### Tutorial 1 (Getting Stock Exchange Quotes for Symbols)

This tutorial fetches real-time quotes for all input symbols from the Spring Financial data server.
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
                                      height="68" name="Retrieve Spring Connection" origin="GENERATED_TUTORIAL"
                                      width="90" x="45" y="85">
                                <parameter key="repository_entry" value="/Connections/MySpringConnection"/>
                                <description align="center" color="transparent" colored="false" width="126">Point to
                                    your connection
                                </description>
                            </operator>
                            <operator activated="true" class="multiply" compatibility="9.6.000" expanded="true"
                                      height="103" name="Multiply" origin="GENERATED_TUTORIAL" width="90" x="179"
                                      y="85"/>
                            <operator activated="true" class="streaming:get_quote_symbols" compatibility="0.2.000"
                                      expanded="true" height="68" name="Get Quote Symbols" origin="GENERATED_TUTORIAL"
                                      width="90" x="313" y="187">
                                <parameter key="stream time (ms)" value="5000"/>
                                <description align="center" color="transparent" colored="false" width="126">Fetch Quote
                                    Symbols
                                </description>
                            </operator>
                            <operator activated="true" class="filter_example_range" compatibility="9.6.000"
                                      expanded="true" height="82" name="Filter Example Range"
                                      origin="GENERATED_TUTORIAL" width="90" x="447" y="187">
                                <parameter key="first_example" value="1"/>
                                <parameter key="last_example" value="1"/>
                                <parameter key="invert_filter" value="false"/>
                                <description align="center" color="transparent" colored="false" width="126">Select 2
                                    Symbols to make a test
                                </description>
                            </operator>
                            <operator activated="true" class="streaming:get_quotes" compatibility="0.2.000"
                                      expanded="true" height="82" name="Get Quotes" width="90" x="648" y="85">
                                <parameter key="stream time (ms)" value="30000"/>
                                <parameter key="disconnect time (ms)" value="1000"/>
                                <description align="center" color="transparent" colored="false" width="126">Fetch
                                    real-time Quotes for the input Symbols
                                </description>
                            </operator>
                            <connect from_op="Retrieve Spring Connection" from_port="output" to_op="Multiply"
                                     to_port="input"/>
                            <connect from_op="Multiply" from_port="output 1" to_op="Get Quotes" to_port="con"/>
                            <connect from_op="Multiply" from_port="output 2" to_op="Get Quote Symbols" to_port="con"/>
                            <connect from_op="Get Quote Symbols" from_port="example set symbols"
                                     to_op="Filter Example Range" to_port="example set input"/>
                            <connect from_op="Filter Example Range" from_port="example set output" to_op="Get Quotes"
                                     to_port="example set of symbols"/>
                            <connect from_op="Get Quotes" from_port="collection of real time quotes"
                                     to_port="result 1"/>
                            <portSpacing port="source_input 1" spacing="0"/>
                            <portSpacing port="sink_result 1" spacing="0"/>
                            <portSpacing port="sink_result 2" spacing="0"/>
                        </process>
                    </operator>
                </process>
```

## Parameters

#### stream time (ms)

Time (in milliseconds) spent on receiving streaming data per input symbol. 
Received data represents real-time quotes. 
This may take a few seconds. 
If no results are received for shorter times, try with increased value.

#### disconnect time (ms)

Time (in milliseconds) to gracefully disconnect from the server.


## Input

#### con (Connection)

Connection to the Spring Financial data server.

#### example set of symbols (Data Table)

ExampleSet representing a list of quote symbols.

## Output

#### collection of real time quotes (com.rapidminer.operator.IOObjectCollection)

Collection of ExampleSets, each representing a list of real-time quotes for an input symbol.