# RapidMiner Streaming Extension

## Changelog

### Version 0.8.0

-

### Version 0.7.1

-

### Version 0.7.0

- Added new operator Maritime Track Fusion operator which integrates the Track Fusion functionality of the research
  partner Marine Traffic.
  - It allows to fusion different input data stream to a joint stream of ship track data to be further analysed.
- Added the integration of the continuous optimization of deployed streaming workflows with the INFORE Optimizer Service
  - The optimization request is added as a Job to the Streaming Management Dashboard. If a new physical (optimized)
    version of the to-be-optimized workflow is available, user can redeploy the new version with one click.

### Version 0.6.1

- Added checks for the topic name parameter of the Kafka Source and Sink operators 
  - The topic name is checked for invalid and/or colliding characters. Quickfixes are provided to replace these
    with underscores.
  - A user error is thrown in case a topic name contains invalid characters
- Added the "Early Time-Series Classification Engine" operator to integrate the partner service from the INFORE project
  to deploy and use the early time-series classification engine of the partner in streaming workflows.
- Bugfix for the integration of the Benchmarking component of the INFORE Optimizer service.
- Bugfixes for the Streaming Optimization Operator for the communications with the INFORE Optimizer service.

### Version 0.6.0

- Added a User Error, in case no stream processing operators are added in the Streaming Nest operator.
- Kafka Connection Objects moved to Kafka Connector extension
- Kafka topic detection added to Kafka Sink and Kafka Sink operators
- Connectivity checks for Flink and Spark Connections
 - Add a new Maritime Akka Cluster connection object class to store the ip address (and any other necessary information
 ) and be used by the Maritime Event Detection operator
- Plug-Ins for deploying RM models on Flink and Spark clusters are shipped with the extension
- Stopping process deployment now works
- Maritime Event Detection operator now differentiate between job types (events & fusion)

### Version 0.5.1

- Bugfix for the Streaming Optimization operator: The execution order of the optimized workflow was not updated and an
  incorrect order could be created.
- Fixed an error in case a Streaming Job was submitted from a non-stored process
  

### Version 0.5.0

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

### Version 0.4.1

-

### Version 0.4.0

- Added the parameters k and offset reset to the Complex Event Forecasting Engine Operator.
- RapidMiner model deployment
- Updated Streaming dashboard
- Update of the integration of the INFORE Optimizer --> see Streaming Optimization operator
- Fixed problem with extensive logging in the financial source operators
- RapidMiner version upgrade --> 9.9.0

### Version 0.3.0

- Added Spring Complex Event Forecasting engine operator, to deploy the forecasting functionality provided by the SPRING
  research partner, in a streaming analytical workflow
- Added Union Stream operator, to perform union operations on two or more input streams in a streaming analytical
  workflow
- Updated the Synopsis Data Engine Operator, to include the latest developments of the corresponding engine.

### Version 0.2.0

- Read Kafka Topic and Write Kafka Topic now uses connection objects, also bugfix for using remote clusters
- Added streaming operators to access the financial data server from SPRING in a streaming workflow ( ??? )
- Added new Streaming Management Dashboard as a Panel in RapidMiner Studio to monitor and manage deployed streaming
  worklfows
- Update Streaming Optimization Operator to use connection objects, to have more advanced parameter handling and
  splitted the subprocess into two subprocesses, one handling the logical workflow and one showing the optimized
  workflow

### Version 0.1.1

- Added copyright headers to all java source files
- Version not released to Marketplace!

### Version 0.1.0

- Initial (alpha) version of the extension
- Main functionality for the deployment of streaming analytic processes implemented
    - Added the Streaming Nest operator for designing and deploying streaming analytic processes
- Added streaming operators to pull and push from/to Kafka (Kafka Source, Kafka Sink)
- Added operators to pull from Kafka and convert to ExampleSet (Read Kafka Topic) and to convert ExampleSet to data
  events and push to Kafka (Write Kafka Topic)
- Added operators to access the financial data server from SPRING (Get Quote Symbols, Get Quotes , Get Depths)
- Added streaming operators to perform several basic streaming functionalities (Aggregate Stream , Duplicate Stream,
  Join Stream, Connect Stream, Filter Stream, Map Stream, Select Stream, Parse Field Stream, Stringify Stream)
- Added integration with the INFORE Optimizer (Streaming optimization operator)
- Added integration with different INFORE Components (Synopsis Data Engine, Athena Online Machine Learning Engine,
  Complex Event Forecasting Engine, Maritime Event Detection) 
