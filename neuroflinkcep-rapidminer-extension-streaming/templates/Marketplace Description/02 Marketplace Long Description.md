# Streaming

The extension adds Operators to design and deploy streaming analytic process on Flink or Spark.

This is a beta version. Please use it carefully, as features are still under development.

The streaming analytic process is designed inside the Streaming Nest operator. Processes are
 deployed by creating and providing a connection object to the corresponding cluster.

Processes designed are platform independent. They can be deployed on other platforms (changing
 also from Flink to Spark and vise-versa) by just changing the provided connection object.

Deployed streaming jobs can be monitored and managed by using the Streaming Dashboard, which can be enabled over the View -> Show Panel menu.

The extension provides the following Operators:

- Streaming
  - Input Output
    - Kafka Sink
    - Kafka Source
  - Flow Control
    - Duplicate Stream
    - Join Streams
    - Connect Streams
    - Union Stream
  - Transformations
    - Aggregate Stream
    - Filter Stream
    - Map Stream
    - Select Stream
    - Parse Field Stream
    - Stringify Field Stream
    - Timestamp Stream
  - Infore Project
    - Financial Server
      - Get Quote Symbols
      - Get Quotes
      - Get Depths
      - Quote or Depth Stream
    - Partner Services
      - Streaming Optimization (updated in 0.7.0)
      - Synopsis Data Engine
      - Athena Online Machine Learning Engine
      - Complex Event Forecasting Engine
      - Spring Complex Event Forecasting Engine
      - Maritime Event Detection
      - Maritime Track Fusion (new in 0.7.0)
    - Streaming Nest
    - Apply Model on Stream

**Version 0.7.0 (2022-05-06)**

- Added new operator Maritime Track Fusion operator which integrates the Track Fusion functionality of the research
  partner Marine Traffic.
  - It allows to fusion different input data stream to a joint stream of ship track data to be further analysed.
- Added the integration of the continuous optimization of deployed streaming workflows with the INFORE Optimizer Service
  - The optimization request is added as a Job to the Streaming Management Dashboard. If a new physical (optimized)
    version of the to-be-optimized workflow is available, user can redeploy the new version with one click.

**Version 0.6.1 (2022-02-07)**

- Added checks for the topic name parameter of the Kafka Source and Sink operators
  - The topic name is checked for invalid and/or colliding characters. Quickfixes are provided to replace these
    with underscores.
  - A user error is thrown in case a topic name contains invalid characters
- Added the "Early Time-Series Classification Engine" operator to integrate the partner service from the INFORE project
  to deploy and use the early time-series classification engine of the partner in streaming workflows.
- Bugfix for the integration of the Benchmarking component of the INFORE Optimizer service.
- Bugfixes for the Streaming Optimization Operator for the communications with the INFORE Optimizer service.

**Version 0.6.0 (2021-12-15)**

- Beta Release of the extension
  - The extension is now based on the Kafka Connector extension 0.3.0
    - Kafka topic detection added to Kafka Sink and Kafka Sink operators
    - Kafka connection objects are created by the Kafka Connector extension
      - Added options for secure connections to Kafka clusters (PLAIN, SASL, SSL) 
  - Plug-Ins for deploying RM models on Flink and Spark clusters are shipped with the extension
  - Connectivity checks for Flink and Spark Connections are now possible
  - Stopping process deployment now works
  - Added a User Error, in case no stream processing operators are added in the Streaming Nest operator.
  - Restructured operator groups
  - Add a new Maritime Akka Cluster connection object class to store the ip address (and any other necessary information
    ) and be used by the Maritime Event Detection operator
  - Maritime Event Detection operator now differentiate between job types (events & fusion)
  
**Version 0.5.0 (2021-10-21)**

- Added the possibility to provide an INFORE Optimizer connection object to the connection input port of the Streaming
  Nest operator.
  - In this case, the StreamGraph defining the streaming workflow is serialized and uploaded to the FileServer of the
    INFORE Optimizer Service.
  - Added the host and port parameters for the file server of the INFORE Optimizer Service to the corresponding
    connection class.
- Added new operator Timestamp Stream
  - This operator puts a timestamp on the events of a stream (assigns it to the key provided as parameter).
- Updated the Complex Event Forecasting Engine operator with three new parameters ("domain stream", "timestamp key", "config topic").
- Updated Streaming Optimization Operator to comply with the latest developments of the INFORE Optimizer Service

**Version 0.4.0 (2021-09-14)**

- Added new operator Apply model on Stream, which enables to deploy a RapidMiner model trained offline in a streaming process
  - The streaming cluster used for this, needs to have the RapidMiner Core available
- Updated Complex Event Forecasting operator
  - Added the parameters k and offset reset
- Updated Streaming dashboard
  - Improved visuals and UX
  - Added the process location of the RM process which was used to deploy the streaming job.
- Update Streaming Optimization operator
  - New connection object to connect to the INFORE Optimizer Service
  - Available computing sites can be more easily defined by an updated port structure
  - Logical and optimized workflow are now separated in different subprocess
  - Adapted configuration parameters and the internal working to changed interface of the INFORE Optimizer Service
- Fixed problem with extensive logging in the financial source operators

**Version 0.3.0 (2021-03-08)**

- Added Spring Complex Event Forecasting engine operator, to deploy the forecasting functionality provided by the SPRING research partner, in a streaming analytical workflow
- Added Union Stream operator, to perform union operations on two or more input streams in a streaming analytical workflow
- Updated the Synopsis Data Engine Operator, to include the latest developments of the corresponding engine.

**Version 0.2.0 (2020-12-09)**

- Read Kafka Topic and Write Kafka Topic now uses connection objects, also bugfix for using remote clusters
- Added streaming operator (only flink) to access the financial data server from SPRING in a streaming workflow ( Quote or Depth Stream )
- Added new Streaming Dashboard as a Panel in RapidMiner Studio to monitor and manage deployed streaming workflows
- Update Streaming Optimization Operator to use connection objects, to have more advanced parameter handling and splitted the subprocess into two subprocesses, one handling the logical workflow and one showing the optimized workflow

**Version 0.1.0 (2020-10-26)**

- Initial alpha version with the main functionality implemented.
- Main functionality for the deployment of streaming analytic processes implemented
    - Added the Streaming Nest operator for designing and deploying streaming analytic processes
- Added streaming operators to pull and push from/to Kafka (Kafka Source, Kafka Sink)
- Added operators to pull from Kafka and convert to ExampleSet (Read Kafka Topic) and to convert
 ExampleSet to data events and push to Kafka (Write Kafka Topic)
- Added operators to access the financial data server from SPRING (Get Quote Symbols, Get Quotes
, Get Depths)
- Added streaming operators to perform several basic streaming functionalities (Aggregate Stream
, Duplicate Stream, Join Stream, Connect Stream, Filter Stream, Map Stream, Select Stream, Parse
 Field Stream, Stringify Stream)
- Added integration with the INFORE Optimizer (Streaming optimization operator)
- Added integration with different INFORE Components (Synopsis Data Engine, Athena Online Machine
 Learning Engine, Complex Event Forecasting Engine, Maritime Event Detection) 
