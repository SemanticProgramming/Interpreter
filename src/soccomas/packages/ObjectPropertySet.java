/*
 * Created by Roman Baum on 12.04.16.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages;

import soccomas.basic.SOCCOMASURLEncoder;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.json.JSONObject;

/**
 * This Class combines all object properties of given RDF files in a directory and save them all together in a jena model.
 */
public class ObjectPropertySet {

    private String pathToMainDirectory;

    public ObjectPropertySet(String pathToMainDirectory) {
        this.pathToMainDirectory = pathToMainDirectory;
    }


    /**
     * This method combines all object properties of the rdf files in a directory in a new jena model
     * @return the combined object properties model
     */
    public Model createModel () {

        // todo update queries after change URIs in middleware and frontend

        JenaIOTDBFactory connectionToTDB = new JenaIOTDBFactory();


        String sparqlGetObjectProperty = "" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "\n" +
                "CONSTRUCT {?s ?p ?o} " +
                "\n" +
                "WHERE {\n" +
                "GRAPH ?g {?s ?p ?o}\n" +
                "" +
                "FILTER (?o IN (owl:ObjectProperty))" +
                "" +
                "}";

        // return a model with all object properties
        return connectionToTDB.pullDataFromTDB(this.pathToMainDirectory, sparqlGetObjectProperty);

    }

    /**
     * This method combines all object properties of the rdf files of a workspace in an JSONObject
     * @return an JSONObject with all object properties as key
     */
    public JSONObject createJSONObject () {

        Model objectPropertyModel = createModel();

        ResIterator objectPropertiesResIter = objectPropertyModel.listSubjects();

        JSONObject objectPropertiesInJSON = new JSONObject();

        while (objectPropertiesResIter.hasNext()) {

            objectPropertiesInJSON.put(objectPropertiesResIter.nextResource().toString(), true);

        }


        // return an JSONObject with all object properties as key
        return objectPropertiesInJSON;

    }


    /**
     * The method checks if an object property exist in the combined JSONObject or not.
     * @param objectPropertiesInJSON a JSONObject with all known object properties inside
     * @param calcProperty is the object property to check if it is in the model or not
     * @return a boolean value. true if the object property is part of the model and false if it is not in the model
     */
    public boolean objectPropertyExist(JSONObject objectPropertiesInJSON, String calcProperty) {

        return objectPropertiesInJSON.has(calcProperty);

    }

    /**
     * The method checks if an object property exist in the combined jena object property model or not.
     * @param objectPropertyModel is the model to be investigated
     * @param calcProperty is the object property to check if it is in the model or not
     * @return a boolean value. true if the object property is part of the model and false if it is not in the model
     */
    public boolean objectPropertyExist(Model objectPropertyModel, String calcProperty) {

        String sparqlGetPropertyString;

        // create a TDB-Interface
        QueryExecution qexec;

        // boolean result for the query
        boolean result_bool;

        UrlValidator objectValidator = new UrlValidator();

        // get a MDB url Encoder to encode the uri with utf-8
        SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

        if (objectValidator.isValid(soccomasURLEncoder.encodeUrl(calcProperty, "UTF-8"))) {

            sparqlGetPropertyString = "" +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                    "" +
                    "ASK {\n" +
                    "  <" + calcProperty + "> rdf:type owl:ObjectProperty \n" +
                    "}";

            qexec = QueryExecutionFactory.create(sparqlGetPropertyString, objectPropertyModel);

            result_bool = qexec.execAsk();

        } else {

            // TODO add the prefix to the sparql query

            sparqlGetPropertyString = "" +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                    "" +
                    "ASK {\n" +
                    "  <" + calcProperty + "> rdf:type owl:ObjectProperty \n" +
                    "}";

            qexec = QueryExecutionFactory.create(sparqlGetPropertyString, objectPropertyModel);

            // execute the query
            result_bool = qexec.execAsk();

        }


        return result_bool;

    }




}
