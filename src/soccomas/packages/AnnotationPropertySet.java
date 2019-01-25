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
 * This Class combines all annotation properties of given RDF files in a directory and save them all together in a jena model.
 */
public class AnnotationPropertySet {

    private String pathToMainDirectory;

    public AnnotationPropertySet(String pathToMainDirectory) {
        this.pathToMainDirectory = pathToMainDirectory;
    }


    /**
     * This method combines all annotation properties of the rdf files in a directory in a new jena model
     * @return the combined annotation properties model
     */
    public Model createModel () {

        // todo update queries after change URIs in middleware and frontend

        JenaIOTDBFactory connectionToTDB = new JenaIOTDBFactory();


        String sparqlGetAnnotationProperty = "" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "\n" +
                "CONSTRUCT {?s ?p ?o} " +
                "\n" +
                "WHERE {\n" +
                "GRAPH ?g {?s ?p ?o}\n" +
                "" +
                "FILTER (?o IN (owl:AnnotationProperty))" +
                "" +
                "}";

        // return a model with all annotation properties
        return connectionToTDB.pullDataFromTDB(this.pathToMainDirectory, sparqlGetAnnotationProperty);

    }


    /**
     * This method combines all annotation properties of the rdf files of a workspace in an JSONObject
     * @return an JSONObject with all annotation properties as key
     */
    public JSONObject createJSONObject () {

        Model annotationPropertyModel = createModel();

        ResIterator annotationPropertiesResIter = annotationPropertyModel.listSubjects();

        JSONObject annotationPropertiesInJSON = new JSONObject();

        while (annotationPropertiesResIter.hasNext()) {

            annotationPropertiesInJSON.put(annotationPropertiesResIter.nextResource().toString(), true);

        }


        // return an JSONObject with all annotation properties as key
        return annotationPropertiesInJSON;

    }


    /**
     * The method checks if an annotation property exist in the combined JSONObject or not.
     * @param annotationPropertiesInJSON a JSONObject with all known annotation properties inside
     * @param calcProperty is the annotation property to check if it is in the model or not
     * @return a boolean value. true if the annotation property is part of the model and false if it is not in the model
     */
    public boolean annotationPropertyExist(JSONObject annotationPropertiesInJSON, String calcProperty) {

        return annotationPropertiesInJSON.has(calcProperty);

    }

    /**
     * The method checks if an annotation property exist in the combined jena annotation property model or not.
     * @param annotationPropertyModel is the model to be investigated
     * @param calcProperty is the annotation property to check if it is in the model or not
     * @return a boolean value. true if the annotation property is part of the model and false if it is not in the model
     */
    public boolean annotationPropertyExist(Model annotationPropertyModel, String calcProperty) {

        String sparqlGetPropertyString;

        // create a TDB-Interface
        QueryExecution qexec;

        // boolean result for the query
        boolean result_bool;

        UrlValidator annotationValidator = new UrlValidator();

        // get a MDB url Encoder to encode the uri with utf-8
        SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

        if (annotationValidator.isValid(soccomasURLEncoder.encodeUrl(calcProperty, "UTF-8"))) {

            sparqlGetPropertyString = "" +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                    "" +
                    "ASK {\n" +
                    "  <" + calcProperty + "> rdf:type owl:AnnotationProperty \n" +
                    "}";

            qexec = QueryExecutionFactory.create(sparqlGetPropertyString, annotationPropertyModel);

            result_bool = qexec.execAsk();

        } else {

            // TODO add the prefix to the sparql query

            sparqlGetPropertyString = "" +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                    "" +
                    "ASK {\n" +
                    "  <" + calcProperty + "> rdf:type owl:AnnotationProperty \n" +
                    "}";

            qexec = QueryExecutionFactory.create(sparqlGetPropertyString, annotationPropertyModel);

            // execute the query
            result_bool = qexec.execAsk();

        }


        return result_bool;

    }




}
