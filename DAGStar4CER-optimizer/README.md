# ATHENA Component

Service and resources for workflow optimization, file storage, content visualization and statistics collection for the INFORE project.

Online [README and resources](https://drive.google.com/drive/folders/1KfGr7kNI82Ws7cEKHVqloHjY1kOjiD2Y).

Offline [Resources](input)

# ATHENA Services

## [Core](core)

Module for project wide dependencies.

## [File Server](file_server)

File storage with REST API.

## [Benchmarking](benchmarking)

Workflow submission to BigData platforms.

## [Bayesian Optimization](bayesian_optimization)

Alternative operator cost estimation.

## [Optimizer](optimizer)

Statistics collection, parsing utilities and optimization rules of the project. Contains methods for parsing input resource files regarding workflows,
sites, dictionaryOperators and more.

## [Uploader](uploader)

Module used for uploading and running workflows to different BigData platforms.

## [Web Interface](web)

Web service responsible for submitting workflows and transmitting back optimized workflows.

# Persistent storage and content visualization

The ELK stack (Elasticsearch Logstash Kibana) is used for storing user files, aggregating platforms statistics and visualizing results. A dockerized
version of ELK stack is included in the docker-compose along with the optimizer service. To view submitted files visit the following line (replace
localhost with the deployed IP).

# Resource parsing and schema

The following classes are used to parse optimizer resources.

[Benchmarking](core/src/main/java/core/parser/benchmarking/BenchmarkingRequest.java)

[Dictionary](core/src/main/java/core/parser/dictionary/OldINFOREDictionary.java)

[Network](core/src/main/java/core/parser/network/INFORENetwork.java)

[Opt. Request](core/src/main/java/core/parser/workflow/OptimizationRequest.java)

# Configure / Build / Deploy

Edit the .env file if you would like to change any of the networking or security options.

    mvn install
    docker-compose up

Note: If the docker service doesn't recognise one of the docker-compose script services, simply navigate to their directory and run the following
command before attempting to docker-compose up again.

    mvn install

# Contact

George Stamatakis

email: giorgoshstam@gmail.com

# License

The project's license can be found [locally](LICENSE.md) and [online](https://www.gnu.org/licenses/agpl-3.0.md).
