/*
 * Created by Roman Baum on 20.07.15.
 * Last modified by Roman Baum on 22.01.19.
 */
package soccomas.packages;

import soccomas.basic.SOCCOMASURLEncoder;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;

/**
 * This Class combines all classes of given RDF files in a directory and save them all together in a jena model.
 */
public class ClassSet {

    private String pathToMainDirectory;

    public ClassSet(String pathToMainDirectory) {
        this.pathToMainDirectory = pathToMainDirectory;
    }

    /**
     * This method combines all classes of the rdf files in a directory in a new jena model
     * @return the combined class model
     */
    public Model createModel () {

        // todo update queries after change URIs in middleware and frontend

        JenaIOTDBFactory connectionToTDB = new JenaIOTDBFactory();


        String sparqlGetClass = "" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "\n" +
                "CONSTRUCT {?s ?p ?o} " +
                "\n" +
                "WHERE {\n" +
                "GRAPH ?g {?s ?p ?o}\n" +
                "" +
                "FILTER (?o IN (owl:Class) && !isBlank(?s) )" +
                "" +
                "}";

        // return a model with all classes
        return connectionToTDB.pullDataFromTDB(this.pathToMainDirectory, sparqlGetClass);

    }

    /**
     * The method checks if a class exist in the combined jena class model or not.
     * @param classModel is the model to be investigated
     * @param calcResource is the class to check if it is in the model or not
     * @return a boolean value. true if the class is part of the model and false if it is not in the model
     */
    public boolean classExist (Model classModel, String calcResource) {

        String sparqlGetPropertyString;

        // create a TDB-Interface
        QueryExecution qexec;

        // boolean result for the query
        boolean result_bool = false;

        UrlValidator objectValidator = new UrlValidator();

        // get a MDB url Encoder to encode the uri with utf-8
        SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

        if (objectValidator.isValid(soccomasURLEncoder.encodeUrl(calcResource, "UTF-8"))) {

            sparqlGetPropertyString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "" +
                    "ASK {\n" +
                    " <" + calcResource + "> rdf:type owl:Class \n" +
                    "}";

            qexec = QueryExecutionFactory.create(sparqlGetPropertyString, classModel);

            result_bool = qexec.execAsk();

        } /*else {

            // TODO add the prefix to the sparql query

            sparqlGetPropertyString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "" +
                    "ASK {\n" +
                    "  <" + calcResource + "> rdf:type owl:Class\n" +
                    "}";


            qexec = QueryExecutionFactory.create(sparqlGetPropertyString, classModel);

            // execute the query
            result_bool = qexec.execAsk();

        }*/

        return result_bool;

    }




}
