
# Java Interpreter


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

