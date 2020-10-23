How to build and deploy server and client

Run the following commands:
cd server/
mvn clean package && java -jar target/web-frontend-1.0-SNAPSHOT-jar-with-dependencies.jar
    
Then open localhost:4567 in your browser.

For model checking to work, you have to have the tools abc, aiger and mchyper, as well as GNU 'time', installed on your system and update the file ADAM.properties to have the correct paths to each one.  The source code of each one is in a .tar.gz or a .zip file in this repository.  The README for mchyper explains how to compile them.

The temporary files created by abc, mchyper, etc. are stored by default in './tmp/' in the server's working directory.  This location can be overridden using the command line flag -DADAMWEB_TEMP_DIRECTORY=/path/to/store/temporary/files/
