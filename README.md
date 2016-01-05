# Provenance Map Orbiter

Provenance Map Orbiter enables its users to visualize and interactively explore large provenance graphs, which can be used for a variety of tasks from determining the inputs to a particular process to debugging entire workflow executions or tracking difficult-to-find dependencies.

## Features
- A fully visual way of viewing provenance graphs - no need to type queries by hand
- Ability to summarize (cluster) graphs and explore them interactively by zooming in/out of summary nodes:
  - Clustering by timestamps
 - Clustering by the control flow
- Filter and search the displayed nodes using their attributes and properties
- Perform simple lineage queries right by filtering or highlighting ancestors or descendants of a selected node
- An alternate time-line view displaying when and for how long each process ran, as well as showing which processes were started by which processes

## Supported Formats
- [Core Provenance Library (CPL)](https://github.com/pmacko86/core-provenance-library)
- [PASS Twig](http://www.eecs.harvard.edu/syrah/pass) (requires a Unix-based system and PASS Tools to be installed)
- [Open Provenance Model (OPM)](http://twiki.ipaw.info/bin/view/OPM)
- A custom format based on `.n3` that makes it easy for users to hand-write graphs

## Publications
- Peter Macko and Margo Seltzer. *Provenance Map Orbiter: Interactive Exploration of Large Provenance Graphs.* 3rd Workshop on the Theory and Practice of Provenance (TaPP '11), Heraklio, Crete, Greece, June 2011. ([pdf](http://www.eecs.harvard.edu/~pmacko/papers/orbiter-tapp11.pdf))
