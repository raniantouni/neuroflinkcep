# RapidMiner as Plugin Bridge

This module mediates between 2 realms per-se: RaaP (RapidMiner as a Plugin) and RapidMiner.

## Dependencies

* __rapidminer-as-plugin-common__
  * declares the (plugin) interfaces, in a RapidMiner independent fashion, although it is of course only used for RapidMiner related task(s)
    
* __rapidminer-core__ (not one of our modules)
  * holds all the types we need to make RapidMiner model application work (~target)