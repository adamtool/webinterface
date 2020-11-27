How to develop
--------------
You can build the frontend itself and have your changes immediately compiled and reloaded in the browser if you go into the "client" directory and run "npm run dev".  (See client/README.md)  The development server will make itself available at localhost:8080.

You should run the Java server at the same time.  You will probably want to install abc, mchyper, etc. following the instructions in README.
- TODO #296 how to start the server
I use intellij and webstorm together at the same time

- In webstorm, "open" the directory 'webinterface/client'.
- In IntelliJ, "file>new>project from existing sources" select 

Misc. notes
-----------
In server/pom.xml, we pull adam_core.jar as a dependency into our Maven project via a "file repository" that was created using the following Maven command:
```mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=adam_core.jar -DgroupId=uniolunisaar.adam -DartifactId=adam-core -Dversion=<Version> -Dpackaging=jar -DlocalRepositoryPath=server/adam-core```
This is the best practice, according to Pascal Thivent in his answer on StackOverflow: https://stackoverflow.com/questions/2229757/maven-add-a-dependency-to-a-jar-by-relative-path


We also import javabdd the same way
e.g.
```mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=server/src/main/resources/javabdd-1.0b2.jar -DgroupId=net.sf.javabdd -DartifactId=javabdd -Dversion=1.0b2 -Dpackaging=jar -DlocalRepositoryPath=server/javabdd```

Build configuration; relative vs. absolute URLs
For HTTP requests to our server, we use relative URLs in production
and hard-coded URLs in development mode ('npm run dev').

How?
----
There are two entry points to our app.
One is in src/main.js, and the other is src/main-dev.js.
In main.js, we pass an empty base URL into the App component,
so that relative URLs will be used.
In main-dev.js, we pass the base URL "http://localhost:4567" into the
App component, so that hard-coded URLs will be used.

The entry point is specified in client/build/webpack.dev.conf.js
and in client/build/webpack.base.conf.js.  webpack.dev.conf.js is
referred to by 'npm run dev'.

Why?
----
'npm run dev' serves the client at localhost:8080, while a developer
is likely to have our java web server running at localhost:4567.
In this case, a relative URL would be wrong; the HTTP requests would go
to e.g. localhost:8080/calculateExistsWinningStrategy and return a 404
instead of localhost:4567/calculateExistsWinningStrategy.

Generating license information for the frontend
Install the program 'npm-license-crawler' in your global scope:

```npm i npm-license-crawler -g```

Then run it to generate a json file that holds information about all 
the external dependencies we have:

```npm-license-crawler --onlyDirectDependencies --json client/src/assets/licenses-json.json```

This file gets parsed and used in our "About" screen to show the license info
and attribution stuff that we have to include for all of our 3rd party libraries.

This process is arguably a little bit unwieldy, since you have to run it every
time you add or remove a dependency. 
TODO #303: Install npm-license-crawler as a devDependency and run it automatically 
during the build process
