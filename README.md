# SOCCOMAS: A Web Content Management System based on Semantic Programming using the Semantic Programming Ontology
(version 0.2 beta)

SOCCOMAS (**S**emantic **O**ntology-**C**ontrolled application for web **CO**ntent **MA**ngement **S**ystems) is a semantic web content management system (S-WCMS) that utilizes the Semantic Programming Ontology (SPrO) and its associated Java-based middleware. SOCCOMAS is controlled by a source code ontology (SC-Basic), which contains descriptions of features and workflows typically required by a S-WCMS, such as user administration with login and signup forms, user registration and login process, session management and user profiles, but also publication life-cycle processes for data entries (i.e. collections of assertional statements referring to a particular entity of a specific kind, like for instance a specimen) and automatic procedures for tracking user contributions, provenance and logging change-history for each editing step of any given version of a data entry. All data and metadata are recorded in RDF following established (meta)data standards using terms and their corresponding URIs from existing ontologies. The middleware functions as a compiler that dynamically executes the descriptions in SC-Basic by interpreting them as declarative specifications. Each S-WCMS based on SOCCOMAS stores data as a knowledge base in a Jena tuplestore. 


Further information on the project is available at http://escience.biowikifarm.net - feel free to contact us at 
dev@morphdbase.de

This repository contains the SOCCOMAS source code ontology (SC-Basic)

**Please be aware that this project is ongoing research! This code is for demonstration purposes only. Do not use
 in production environment!**
  
# MDB Java Middleware


## Installation

#### Get GIT repository:
Clone via bash...

    git clone ssh://git@git.morphdbase.de:6020/roman/java-code.git

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

