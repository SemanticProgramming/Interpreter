package soccomas.packages;


import org.apache.jena.atlas.logging.LogCtl;

import java.io.IOException;

/**
 * Created by rbaum on 16.01.15.
 */
public class JenaTDBInput {



    public static void main(String[] args) throws IOException {
        // Hide the first 3 rows
        LogCtl.setCmdLogging();

        // name of the input-database or input-file
        String inputFileName = "output.owl";

        // name of the tdb-directory
        String pathToTDB  = "../../Dokumente/sample_tbd/";

        // create new object for data input
        JenaIOTDBFactory jenaIOTDBFactory = new JenaIOTDBFactory();

        // call method to push data in the jena tdb
        jenaIOTDBFactory.pushDataInTDB(pathToTDB, inputFileName);
    }
}
