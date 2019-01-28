
# Java Interpreter
We develop a Semantic Programming Ontology (SPrO; https://github.com/SemanticProgramming/SPrO) that is used for software programming. SPrO can be used like a programming language with which one can control a data-centric Semantic Web application by describing it within a corresponding source code ontology. With the terms from SPrO you can describe the graphical user interface (GUI), data representations, user interactions, and all workflow processes of a Semantic Web application. The Java Interpreter functions as an interpreter that dynamically interprets and executes the descriptions in a source code ontology by interpreting them as declarative specifications. 

Further information on the project is available at http://escience.biowikifarm.net - feel free to contact us at 
dev@morphdbase.de

This repository contains the Java-based Interpreter for Semantic Programming based on the Semantic Programming Ontology (SPrO). It interprets and executes specifications of Semantic Web applications contained in source code ontologies. For Semantic Programming you also need the Semantic Programming Ontology (SPrO; https://github.com/SemanticProgramming/SPrO) and the interface (https://github.com/SemanticProgramming/Interface). Basic functionalities are described in the SOCCOMAS source code ontology (SC-Basic; https://github.com/SemanticProgramming/SOCCOMAS) and can be re-used for your Semantic Web application.

**Please be aware that this project is ongoing research! This code is for demonstration purposes only. Do not use
 in production environment!**
 


## Installation

#### Get GIT repository:
Clone via bash...

    git clone https://github.com/SemanticProgramming/Interpreter.git

#### Setup a Web server:
Install a Web server e.g. Jetty:

    https://dzone.com/articles/installing-and-running-jetty

## Configure the Web application

#### Update path in middleware
Change configuration in the Java class 'ApplicationConfigurator':

    protected static final String domain = "http://www.your-domain.com";

    private static final String mainDirectory = "/path/to/tuple-store-root-directory/";

#### Create web archive file:
After that create the specific web archive:

    cd /to/your/folder/location
    jar -cvf web-app.war *


#### Fill Jena TDB
Load the application ontologies in the Jena TDB with the class **LoadFileInTDB**

    cd /to/your/folder/location
    javac LoadFileInTDB.java


## Start the Web application

copy or link the Web archive to the Web server directory

    (e.g. opt/jetty/webapps)

assign the root directories to jetty

    sudo chown -R jetty:jetty /path/to/tuple-store-root-directory/tdb/
    sudo chown -R jetty:jetty /path/to/tuple-store-root-directory/tdb-lucene/

start the web server

    (e.g. sudo service jetty start)

login to http://www.your-domain.com

go to admin site (click "**Go to admin page**" under "**My MDB**")

