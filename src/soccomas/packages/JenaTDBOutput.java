/*
 * Created by Roman Baum on 16.01.15.
 * Last modified by Roman Baum on 04.09.15.
 */
package soccomas.packages;


import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.atlas.logging.LogCtl;

import java.io.IOException;


public class JenaTDBOutput {



    public static void main(String[] args) throws IOException {

        // hide the first 3 rows
        LogCtl.setCmdLogging();

        // name of the input-database or output-file
        String outputFileName = "tdb-output.owl";

        // name of the tdb-directory
        String triplestore_directory  = "../../Dokumente/sample_tbd/";

        // write SPARQL-Query
        ConstructBuilder constructBuilder = new ConstructBuilder();

        constructBuilder.addConstruct("?s", "?p", "?o");

        constructBuilder.addWhere("?s", "?p", "?o");

        String sparqlQueryString = constructBuilder.buildString();

        // create new object for data output
        JenaIOTDBFactory testquery = new JenaIOTDBFactory();

        // call method to push data in the jena tdb
        testquery.pullDataFromTDB(triplestore_directory, outputFileName, sparqlQueryString);
    }
}
