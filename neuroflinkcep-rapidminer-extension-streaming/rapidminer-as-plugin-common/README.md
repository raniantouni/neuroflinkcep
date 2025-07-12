# RapidMiner as Plugin Common

This module provides common ground for RaaP related structures, utilities, logic.
These are, strictly speaking, RapidMiner independent, although it can of course be immediately seen that the whole point is to bring RapidMiner model application to streaming.
Nevertheless, it is important to see this distinction, since at runtime it becomes more relevant what's on the classpath and what isn't.

## Dependencies

* __pf4j__
  * plugin library making RaaP possible (provides inverted, child-first classloading)