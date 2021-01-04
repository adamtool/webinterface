A Web Interface for the Model Checking and Synthesis of Distributed Systems with Data Flows
===========================================================================================
This is the web interface for the command-line tools [AdamMC](https://github.com/adamtool/adammc) and [AdamSYNT](https://github.com/adamtool/adamsynt).

- AdamMC is a model checker for asynchronous distributed systems modeled with Petri nets with transits and specifications given in Flow-LTL.
- AdamSYNT is a synthesizer for asynchronous distributed systems modeled with Petri games.

Features:
---------
- AdamMC:
	- Modeling, visualization, and simulation of Petri nets with transits
	- Model checking of Petri nets with transits against Flow-LTL
	- Model checking of 1-bounded Petri nets against LTL with places and transitions as atomic propositions
	- Visualization and simulation of counter examples
	- **Documentation**: https://github.com/adamtool/webinterface/tree/master/doc/mc
	- ![Reduction Petri net](screenshots/mc_reduction_net.png)
- AdamSYNT:
	- Modeling, visualization, and simulation of Petri games
	- Synthesis of Petri games with one environment player and a bounded number of system players with a local safety objective
	- Interactive visualization of the corresponding two-player game to aid in finding modeling bugs
	- Visualization and simulation of the strategies
	- **Documentation**: https://github.com/adamtool/webinterface/tree/master/doc/synt    
	- ![2-Player Incremental General Approach](screenshots/synt_twoplayer_general.png)

Dependencies:
-------------
This module depends on the 
- the repository as submodules: [libs](https://github.com/adamtool/libs), [examples](https://github.com/adamtool/examples), [framework](https://github.com/adamtool/framework), [logics](https://github.com/adamtool/logics), [modelchecker](https://github.com/adamtool/modelchecker), [synthesizer](https://github.com/adamtool/synthesizer), [high-level](https://github.com/adamtool/high-level), [webinterface-backend](https://github.com/adamtool/webinterface-backend).
- the external tools: [McHyper](https://github.com/reactive-systems/MCHyper), [AigerTools](http://fmv.jku.at/aiger/), [ABC](https://people.eecs.berkeley.edu/~alanmi/abc/).

How To Build:
-------------
I you have not cloned the repository with the ```--recursive``` flag, please first use
```
git submodule update --init
```
to get all code of the backend.

To compile the web interface please use the script

```
./buildWithBackend.sh
```
This also generates the corresponding backend jar file of Adam and integrates it. All other dependencies are downloaded
by maven. This could take a while.

Then open 
```localhost:4567```
in your browser and have fun with the web interface.

For model checking to work, you have to have the tools abc, aiger and mchyper, as well as GNU 'time', installed on your system and update the file ADAM.properties to have the correct paths to each one. The source code of each one is in a .tar.gz or a .zip file in this repository.  The README for mchyper explains how to compile them.

The temporary files created by abc, mchyper, etc. are stored by default in './tmp/' in the server's working directory.  This location can be overridden using the command line flag ```-DADAMWEB_TEMP_DIRECTORY=/path/to/store/temporary/files/```
