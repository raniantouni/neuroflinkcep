# RapidMiner as Plugin

This module wraps RapidMiner Core and makes it available as a PF4J plugin.
The main "entry point" for the plugin is the one, marked in the special __extension.idx__ file in the _resources_ folder.

## Building plugin artifact

The plugin artifact is a ZIP file which contains all the libraries + a special __plugin.properties__:
```
plugin.class=com.rapidminer.extension.streaming.raap.wrapper.RapidMinerPlugin
plugin.id=rm-as-plugin
plugin.version=9.9.0
plugin.description=RapidMiner as Plugin
plugin.license=Apache License 2.0
```

## Dependencies

* __rapidminer-as-plugin-common__
  * declares the (plugin) interfaces, in a RapidMiner independent fashion, although it is of course only used for RapidMiner related task(s)
  * defined common utilities for handling RaaP (Rapidminer as a Plugin, this plugin)
  * root for the PF4J utilities that load this module as a plugin (separate classloading mechanism, etc.)
  * is decoupled from this module, so that it can be used anywhere in the "class hierarchy" (on clusters like Flink or Spark), without needing to worry about dependency-hell
    
* __rapidminer-as-plugin-bridge__
  * utilities for adapting data between RaaP and RapidMiner, since this will be used at multiple places:
    * Studio when setting up the RaaP mechanism based on RapidMiner models
    * cluster side when adapting data to types acceptable by RapidMiner models
    * cluster side when adapting data (RapidMiner model application results) to the types declared/defined in RaaP