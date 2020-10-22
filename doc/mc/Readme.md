User's Guide to the Web Interface (Model Checking)
==================================================
With this user's guide we want to give on overview of some common workflows of the web interface corresponding to AdamMC.

General Items:
--------------
When entering the interface for the model checking approach you get the following picture:
![Initial Screen For Model Checking Approach](screenshots/mc_initial_4k.png)

At the top of the screen you have a menu bar where you have the items
- File
  * **New Petri net with transits** - to clear the old Petri net with transits and create a new fresh one.
  * **Load APT from file** - to load a file from your disk in the APT format (see **todo** format description section)
  * **Save APT to file** - saves the current Petri net with transits to your disk in the APT format (see **todo** format description section)
  * **Load example** - lets you choose some of the provided example Petri nets with transits of the server
- View
  * **Log Window** - shows a logging window for debugging information for the advanced user
  * **Job Queue** - shows a panel with the recent jobs and results of the user and the possibility to load the results back into the interface, or delete or cancel them. Note that only the text and not the colored layer is clickable. You can even exchange your unique identifier of the browser to show others your job list, results, and problems.
![Job Queue](screenshots/mc_job_queue_4k.png)
  * **Show right panel** - expands or collapses the right panel which is shown when having any results created. This can also be done with the slider, which can be used to customize the sizes of the panel
  * **Show physics controls** - this adds the following slider to the bottom of the side
![Physics Control](screenshots/mc_physics_control_4k.png)
Here the behavior of the physics control for the nodes of the visualized objects, i.e., the Petri net with transits or the constructed Petri net, can be customized. When the nodes are unfreezed (see **todo** section create Petri net with Transits) the nodes can freely move over the panel. To created clearer views and to minimize the overlapping the *Repulsion Strength*, the *Link strength*, and the *Gravity strength* can be modified.
  * **Show node labels instead of IDs** - Most of the nodes of the  constructed Petri net for the reduction methods for checking Petri nets with transits against Flow-LTL have a correspondence to the input Petri net with transits. With this button you can toggle between showing the names of corresponding nodes or the original ones.
- Check - this starts the model checking procedure and results in opening a tab to the right showing the answer whether the input Petri net with transit satisfies the Flow-LTL formula formula. If it is not satisfied a counter example is given.
- Reduction
  * **Petri net** - this creates the constructed *Petri net* for the reduction method from the given input Petri net with transits and Flow-LTL formula and shows it in the right panel.
  * **LTL formula** - this created the constructed *LTL formula* for the reduction method from the given input Petri net with transits and Flow-LTL formula and shows it in the right panel.

The items to the right gives you the following features
- **Help** - opens a help dialog with some short cuts and the syntax of the Flow-LTL formulas
- **About** - opens a dialog with some information about this approach
- **GitHub** - opens the source code for the web interface on GitHub
- **Home** - leads you back to the index page to choose between the model checking and the distributed synthesis approach

Create a New Petri Net with Transits
------------------------------------
![Create New Petri Net with Transits](screenshots/mc_expanded_toolbar_4k.png)
