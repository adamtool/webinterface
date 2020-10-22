User's Guide to the Web Interface (Distributed Sythesis)
========================================================

With this user's guide we want to give on overview of some common workflows of the web interface corresponding to AdamSYNT.

- [General Items](#GeneralItems)
- [Create a new Petri game](#CreateAPetriGame)
- [Simulating a Petri game](#SimulatingAPetriGame)
- [Synthesis of Distributed Systems with Petri Games](#SynthesisOfDistributedSystemsWithPetriGames)
- [Reduction from Petri games to Two-Player Games over Finite Graphs](#reduction)
- [Text Editor](#textEditor)


General Items:
--------------
<a name="GeneralItems"></a>
When entering the interface for the synthesis of distributed systems approach you get the following picture:
![Initial Screen For Synthesis Approach](screenshots/synt_start.png)

At the top of the screen you have a menu bar where you find the items

- **File**
  * **New Petri game** - to clear the old Petri game and create a new fresh one.
  * **Load APT from file** - to load a file from your disk in the APT format (see [here](https://uol.de/f/2/dept/informatik/ag/csd/adam/Format.pdf) for a format description)
  * **Save APT to file** - saves the current Petri net with transits to your disk in the APT format (see [here](https://uol.de/f/2/dept/informatik/ag/csd/adam/Format.pdf) for a format description)
  * **Load example** - lets you choose some of the provided example Petri games of the server
- **View**
  * **Log Window** - shows a logging window for debugging information for the advanced user
  * **Job Queue** - shows a panel with the recent jobs and results of the user and the possibility to load the results back into the interface, or delete or cancel them. Note that only the text and not the colored layer is clickable. You can even exchange your unique identifier of the browser to show others your job list, results, and problems.
![Job Queue](screenshots/mc_job_queue.png)
  * **Show right panel** - expands or collapses the right panel which is shown when having any results created. This can also be done with the slider, which can be used to customize the sizes of the panel
  * **Show physics controls** - this adds the following slider to the bottom of the side
![Physics Control](../mc/screenshots/mc_physics_control_4k.png)
Here the behavior of the physics control for the nodes of the visualized objects, i.e., the input Petri game, the strategy, the two-player game, and the two-player strategy, can be customized. When the nodes are unfreezed (see [here](#unfreeze)) the nodes can freely move over the panel. To created clearer views and to minimize the overlapping the *Repulsion Strength*, the *Link strength*, and the *Gravity strength* can be modified.
  * **Show partitions** - To allow for faster algorithms the user has to annotate the places with partion ids (see [here](#partitions)). With this item the visualization of the partitions can be toggled on and off.
  * **Show node labels instead of IDs** - The nodes of the strategy for corresponds to the nodes of the Petri game. With this button you can toggle between showing the names of corresponding nodes or the original ones.
- **Solve** - this starts the distributed synthesis procedure and opens a tab to the right showing either the strategy or that no strategy exists.
- **Analyze**
  * **Exists winning strategy** - opens a new tap to the right with the answer whether a strategy for the input Petri game exists or not
  * **2-Player Strategy** - opens a new tap showing either the strategy of two-player game corresponding to the input Petri game or that no strategy exists (see the [reduction section](#reduction)).
  * **2-Player game (complete, BDD)** - creates the two-player game to the given Petri game (see the [reduction section](#reduction)). This method creates the complete graph and is very expensive due to the huge state-space of most examples.
  * **2-Player game (incremental, explicit)** - creates the two-player game to the given Petri game (see the [reduction section](#reduction)). This method creates the state space incrementally by interactively only calculating the next successor states. It only considers cases where in every infinite sequence of transitions there are infinitely many transitions involving the environment. This allows a faster calculation and clearer view, but only considers a subclass of Petri games.
  * **2-Player game (incremental, BDD)** - creates the two-player game to the given Petri game (see the [reduction section](#reduction)). This method creates the state space incrementally by interactively only calculating the next successor states. Due to the usage of BDDs the calculation of the first successor state may take a long time, since already a lot of the state-space has to be calculated.

The items to the right gives you the following features

- **Help** - opens a help dialog with some short cuts and the link to this user's guide
- **About** - opens a dialog with some information about this approach
- **GitHub** - opens the source code for the web interface on GitHub
- **Home** - leads you back to the index page to choose between the model checking and the synthesis approach

Create a New Petri Game:
------------------------
<a name="CreateAPetriGame"></a>
![Creating a New Petri Game](screenshots/synt_leftMenu.png)

Simulating a Petri game:
------------------------
<a name="SimulatingAPetriGame"></a>
![Simulating a Petri Game](screenshots/synt_simulation.png)

Synthesis of Distributed Systems with Petri Games:
--------------------------------------------------
<a name="SynthesisOfDistributedSystemsWithPetriGames"></a>

![Exists Winning Strategy](screenshots/synt_exstrat.png)
![Winning Strategy](screenshots/synt_strat.png)

Reduction from Petri games to Two-Player Games over Finite Graphs:
------------------------------------------------------------------
<a name="reduction"></a>
![Winning Strategy](screenshots/synt_twoplayer_strat.png)



Text Editor:
------------
<a name="textEditor"></a>
A text editor is provided to change and also edit the input Petri game:
![Text Editor](screenshots/synt_apt_editor.png)
For the format please see [here](https://uol.de/f/2/dept/informatik/ag/csd/adam/Format.pdf).
