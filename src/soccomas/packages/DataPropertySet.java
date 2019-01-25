package soccomas.packages;
/*
 * Created by Roman Baum on 20.07.15.
 * Last modified by Roman Baum on 22.01.19.
 */
import soccomas.basic.SOCCOMASURLEncoder;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.json.JSONObject;

/**
 * This Class combines all data properties of given RDF files in a directory and save them all together in a jena model.
 */
public class DataPropertySet {

    private String pathToMainDirectory;

    public DataPropertySet(String pathToMainDirectory) {
        this.pathToMainDirectory = pathToMainDirectory;
    }


    /**
     * This method combines all data properties of the rdf files in a directory in a new jena model
     * @return the combined data properties model
     */
    public Model createModel () {

        // todo update queries after change URIs in middleware and frontend

        JenaIOTDBFactory connectionToTDB = new JenaIOTDBFactory();


        String sparqlGetDataProperty = "" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "\n" +
                "CONSTRUCT {?s ?p ?o} " +
                "\n" +
                "WHERE {\n" +
                "GRAPH ?g {?s ?p ?o}\n" +
                "" +
                "FILTER (?o IN (owl:DatatypeProperty))" +
                "" +
                "}";

        // return a model with all data properties
        return connectionToTDB.pullDataFromTDB(this.pathToMainDirectory, sparqlGetDataProperty);

    }


    /**
     * This method combines all data properties of the rdf files of a workspace in an JSONObject
     * @return an JSONObject with all data properties as key
     */
    public JSONObject createJSONObject () {

        Model dataPropertyModel = createModel();

        ResIterator dataPropertiesResIter = dataPropertyModel.listSubjects();

        JSONObject dataPropertiesInJSON = new JSONObject();

        while (dataPropertiesResIter.hasNext()) {

            dataPropertiesInJSON.put(dataPropertiesResIter.nextResource().toString(), true);

        }

        // return an JSONObject with all data properties as key
        return dataPropertiesInJSON;

    }


    /**
     * The method checks if an object property exist in the combined JSONObject or not.
     * @param dataPropertiesInJSON a JSONObject with all known data properties inside
     * @param calcProperty is the object property to check if it is in the model or not
     * @return a boolean value. true if the object property is part of the model and false if it is not in the model
     */
    public boolean dataPropertyExist(JSONObject dataPropertiesInJSON, String calcProperty) {

        return dataPropertiesInJSON.has(calcProperty);

    }

    /**
     * The method checks if a data property exist in the combined jena data property model or not.
     * @param dataPropertyModel is the model to be investigated
     * @param calcProperty is the data property to check if it is in the model or not
     * @return a boolean value. true if the data property is part of the model and false if it is not in the model
     */
    public boolean dataPropertyExist (Model dataPropertyModel, String calcProperty) {

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
                    "  <" + calcProperty + "> rdf:type owl:DatatypeProperty \n" +
                    "}";

            qexec = QueryExecutionFactory.create(sparqlGetPropertyString, dataPropertyModel);

            result_bool = qexec.execAsk();

        } else {

            // TODO add the prefix to the sparql query

            sparqlGetPropertyString = "" +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                    "" +
                    "ASK {\n" +
                    "  <" + calcProperty + "> rdf:type owl:DatatypeProperty \n" +
                    "}";

            qexec = QueryExecutionFactory.create(sparqlGetPropertyString, dataPropertyModel);

            // execute the query
            result_bool = qexec.execAsk();

        }


        return result_bool;

    }




}
