# RapidMiner Streaming Extension
This extension provides operators that allow RapidMiner users to design and deploy arbitrary streaming jobs.

## Version
Dependency versions throughout the project:

* **Flink**: 1.9.2 with Scala 2.11
* **Spark**: 2.4.5 with Scala 2.11
* **RapidMiner**: 9.9.0

## Architecture

From a high level, this is how it looks like:

![High level overview](./doc/high_level_arch.svg?raw=true "High level architecture")

1. User defines data stream processing workflow in RapidMiner Studio (GUI)
2. User starts process execution as usual (big, blue play button)
3. RapidMiner collects workflow nodes, their configurations and builds a graph from them (platform independent)
4. RapidMiner serializes this workflow graph
5. RapidMiner submits platform-specific job (containing the workflow graph processor) + serialized graph
6. Cluster (e.g.: Flink, Spark) receives job (+ artifacts) and starts its execution
7. Platform-specific processor (main entry point for the job) deserializes, translates (into a platform specific representation) and executes workflow

## Job submission

How do we submit jobs ?

### Spark

The so-called _fat-JAR_ is already part of the extension package (see the Spark module section in this document).

1. User configures Spark connection, attaches it to the workflow and runs the process
2. Graph/workflow gets serialized into a temporary file
3. Connection configuration gets merged with the default configuration (```resources/rm-spark-defaults.conf```) and then serialized into a temporary file
4. Module _fat-JAR_ gets a temporary copy into which both temporary files get embedded (==> deployable)
5. The _fat-JAR_ will be copied over to the appropriate HDFS cluster (based on connection configuration)
6. Apache-Livy REST API call to start job

### Flink

The so-called _fat-JAR_ is already part of the extension package (see the Flink module section in this document).

1. User configures Flink connection, attaches it to the workflow and runs the process
2. Graph/workflow gets serialized into a temporary file
3. Module _fat-JAR_ gets a temporary copy into which graph file get embedded (==> deployable)
4. _fat-JAR_ is then uploaded via Flink REST API
5. Job gets started via Flink REST API

## Project structure

The project comprises the following modules:

```
rapidminer-extension-streaming (main project src)
flink (module)
spark (module)
utility (module)
rapidminer-as-plugin (module)
rapidminer-as-plugin-bridge (module)
rapidminer-as-plugin-common (module)
```

### Code dependencies

Below graph shows compile time dependencies between sources/modules:

```
rapidminer-extension-streaming (main project src) -----------------------------> rapidminer-as-plugin-bridge (module)
                                                     \
flink (module) ------------------------------------------> utility (module) ---> rapidminer-as-plugin-common (module)
                                                     /
spark (module) -------------------------------------
```

### Runtime

At runtime, when we actually launch streaming jobs, the following "dependency" graph exists:

```
                                                       ---> flink (module) ---
                                                     /                        \
rapidminer-extension-streaming (main project src) ---                          ---> rapidminer-as-plugin (module)
                                                     \                        /
                                                      ---> spark (module) ---
```

## Modules

### Utility

The *utility* module contains common functionality needed by other modules (e.g.: HDFS, Kryo).
It is also the place where the framework independent stream graph lives.
RapidMiner users interact with the GUI to build processing workflows.
Under the hood, GUI elements, upon process execution, build such a framework independent stream graph for deployment.

### Flink

The *flink* module contains Apache Flink specific code.
It is basically a deployable unit which could almost be forwarded to a Flink cluster as it is.
However, it first needs to be equipped with a framework independent stream graph, which happens at runtime (will be embedded in the JAR copy).

### Spark

The *spark* module contains Apache Spark Streaming (structured) specific code.
It is also almost deployable as it is, i.e. could be forwarded to a Spark cluster for execution.
However, the same way as *flink*, it also needs to be equipped with a framework independent stream graph, which happens at runtime here as well.
In this case, since we are already making use of a 3rd party store (e.g.: HDFS cluster) for JAR distribution, we simply move the serialized graph there too.

## RapidMiner model deployment

### Modules

* [rapidminer-as-plugin](rapidminer-as-plugin)
* [rapidminer-as-plugin-bridge](rapidminer-as-plugin-bridge)
* [rapidminer-as-plugin-common](rapidminer-as-plugin-common)

### Initial assumptions

* This endeavour should be considered as a best-effort feature/service (i.e. probably not everything will work as smoothly as on your computer, where Studio is in full control of the process)
* The platforms will need to be equipped with the necessary tools/libraries/artifacts to be able to support RapidMiner model deployment (i.e. custom docker images will be derived)
* RapidMiner should not be naively (class-)loaded by the underlying platform, since there could be differences in library usage to the underlying platform's (e.g.: apache-commons, etc.)

### Approach

Here we will detail certain design decisions we made to address the above assumptions.

* In case of Spark, this was clearly und undoubtedly an issue (dependency hell), so it needed to be addressed: the concept of RapidMiner-as-a-Plugin (RaaP) was introduced
* RaaP, using PF4J (https://pf4j.org/), is being literally brought in as a plugin, which itself is capable of loading plugins on its own (RapidMiner extensions)
* PF4J provides a simple, reversed plugin loading mechanism, which in this case is exactly what we need:
  1. load everything that is related to RapidMiner or its dependencies from its own artifacts (that's why we need to set up new images, basically need to embed a complete RapidMiner installation)
  2. load everything else, mostly JDK/core Java utilities, using the host's loader (platform classloader that starts the stream processing job)
  3. we, of course, realize this requires the collaboration of the plugin (RaaP) and its plugins themselves (accessing the system classloader for whatever reason could be tricky)
  4. there are probably, numerous other reasons why this won't work as smoothly as in Studio, but that's why we set out to make it a best-effort service in the first place, without putting too much effort in it
* The new module ```rapidminer-as-plugin-common```:
    1. will already be placed on the cluster, so that stream processing jobs as well as RaaP can use it
    2. houses PF4J (common-)utilities for loading RaaP + custom data containers to communicate data toward/from it in a RapidMiner independent way
    3. custom data containers seem to be necessary since we cannot allow any dependency on RapidMiner code from a stream processing job (due to potential dependency hell, see above)
    4. however, we can allow both, stream processing code (e.g.: Flink job) and RaaP to use the same containers for communicating data in both directions (since these classes would be loaded by a common parent classloader)
* The new module ```rapidminer-as-plugin-bridge```:
    1. bridge between non-RaaP code (e.g.: Flink job wanting to use a RapidMiner model for scoring) and RaaP code, for instance for the purpose of conversion between the 2 realms
    2. this mediation is also need in Studio when setting up the environment
    3. depends on: ```rapidminer-as-plugin-common``` and RapidMiner-core
* The new module ```rapidminer-as-plugin```:
  1. will be the wrapper around RapidMiner (~RaaP itself) and be responsible for initializing the RapidMiner environment, thus enabling model scoring
  2. will already be placed on the cluster, where PF4J, upon request, will be able to load it as its plugin (packaged/shipped as a fat-Zip)
  3. depends on: ```rapidminer-as-plugin-common``` and ```rapidminer-as-plugin-bridge```

### Model deployment architecture

In below figures we will concentrate on connections/dependencies that are important from a RapidMiner model deployment perspective.

*  configure model deployment node in Studio:

```
rapidminer-extension-streaming (main project src)
   \
    \ ---> rapidminer-as-plugin-common (module)
```

* job starting on cluster (Flink or Spark), ```===>``` means a runtime only dependency (~plugin loaded dynamically)

```
flink (module)
   \
    \
     | ---> rapidminer-as-plugin-common (module) ===> rapidminer-as-plugin (module)
    /
   /
spark (module)
```

### Notes

1. ND4J extension (native loading) seems to have a problem with the Spark docker images (from Big-Data-Europe) we are currently using
2. For setting up the clusters (Flink, Spark) one needs the RaaP artifacts which can be generated by the following _Gradle_ tasks: ```rapidminer-as-plugin:shadowJar```, ```rapidminer-as-plugin-common:shadowJar```