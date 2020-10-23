How to create the backend for the web interface
===============================================
This ReadMe describes how the backend of the web interface can be build and integrated into the webserver. 
Furthermore, we describe how a new version of the backend can be published.

Initially (only once after a fresh clone):
------------------------------------------
When you have cloned the repository _webinterface_ for the first time and 
you have not used the _--recursive_ parameter, you have to first execute
```
git submodule update --init
```
to get all the code of the backend.

Second (or first), you have to set all freshly checked repositories to the master since the submodules get checkout 
with a detached head:
```
git submodule foreach --recursive git checkout master
```

Build and integrate the backend:
--------------------------------
Go into the backend folder
```
cd backend
```
and execute
```
make integrate_backend
```
The current version of the backend is integrated and the server is set to use the created adam_core.jar.

Publish a new version of the backend:
-------------------------------------
To publish a new version of the backend first go into the backend folder
```
cd backend
```
then execute
```
make publish_backend ver=<version>
```
with your desired version number _ver_ (the past version numbers can be found in _./server/pom.xml_ and can be printed with ```make print_current_backend_version ```).
This pulls all submodules to get the latest version of all submodules building the backend.
Then, it builds the jar for the backend, integrates it and links this version in the pom.xml of the server. 
Finally, it adds, commits, and pushes these changes to the repository _webinterface_.

Update the examples
-------------------
To update the examples under _File->Load example_ to the current version of the examples with coordinates in the repository [examples](https://github.com/adamtool/examples), you can use
```
make update_examples
```
If you want to have clean example folders which only contain the examples from the repo, you can use
```
make update_examples clean=true
```
