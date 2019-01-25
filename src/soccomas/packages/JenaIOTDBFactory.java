/*
 * Created by Roman Baum on 21.01.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages;

import soccomas.vocabulary.OntologyPrefixList;
import org.apache.jena.query.*;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.resultset.RDFOutput;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *      Methods are provided for push or pull Models to or from a TDB.
 */

public class JenaIOTDBFactory {


    // default - Constructor
    public JenaIOTDBFactory() {

    }


    /**
     * This method formats a query result to a JSONArray with format [{"resource":"resource_data_0", "label":"label_data_0"}
     * , ... , {"resource":"resource_data_n", "label":"label_data_n"}].
     * @param resultSet contains an unformatted result from a jena tdb.
     * @return a structured JSONArray.
     */
    private JSONArray convertAutoCompleteResultToJSONArray(ResultSet resultSet) {

        JSONArray jsonArray = new JSONArray();

        OntologyPrefixList ontologiesVocabulary = new OntologyPrefixList();

        JSONObject ontologyPrefixJSON = ontologiesVocabulary.getOntologyPrefixList();

        while (resultSet.hasNext()) {

            JSONObject jsonObject = new JSONObject();
            String key = "";
            String value = "";

            QuerySolution currSolution = resultSet.nextSolution();

            Iterator<String> currSolutionPart = currSolution.varNames();

            while (currSolutionPart.hasNext()) {

                String currSolString = currSolutionPart.next();

                if (currSolution.contains(currSolString) &&
                        currSolution.get(currSolString).isLiteral()) {

                    key = "label";
                    value = currSolution.getLiteral(currSolString).toString();

                } else if (currSolution.contains(currSolString) &&
                        currSolution.get(currSolString).isResource()) {

                    key = "resource";
                    value = currSolution.getResource(currSolString).toString();

                }

                jsonObject.put(key, value);

            }

            // add ontology class prefix to label
            Iterator<String> ontologyPrefixesIter = ontologyPrefixJSON.keys();

            while (ontologyPrefixesIter.hasNext()) {

                String ontologyPrefixKey = ontologyPrefixesIter.next();

                if (jsonObject.get("resource").toString().contains(ontologyPrefixKey)) {

                    jsonObject.put("label", (jsonObject.get("label").toString() + " (" + ontologyPrefixJSON.get(ontologyPrefixKey).toString() + ")"));

                }

            }

            jsonArray.put(jsonObject);

        }

        return jsonArray;

    }


    /**
     * This method saves an named graph in a tdb
     * @param pathToTDB the path to the tdb directory
     * @param modelURI the uri of the named graph
     * @param addedModel the named graph which should save in the jena tdb
     * @return a message if the named graph was successfully saved or not
     */
    public String addModelDataInTDB(String pathToTDB, String modelURI, Model addedModel) {

        long dummy = System.currentTimeMillis();

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        System.out.println((System.currentTimeMillis() - dummy) + "\t before write");
        dataset.begin( ReadWrite.WRITE );
        try {
            // create the default model/graph

            System.out.println((System.currentTimeMillis() - dummy) + "\t before model");
            Model namedModel = dataset.getNamedModel(modelURI);
            System.out.println((System.currentTimeMillis() - dummy) + "\t after model");
            // add a new model to the specific named graph in the Jena TDB
            namedModel.add(addedModel);
            System.out.println((System.currentTimeMillis() - dummy) + "\t after add model");
            // finish the "WRITE" transaction
            dataset.commit();
            System.out.println((System.currentTimeMillis() - dummy) + "\t after commit");

            return "The triple was successfully saved in the named graph called: " + modelURI;
        }
        finally {

            // close the dataset
            dataset.end();

            // write a message if the import was successfully
            System.out.println((System.currentTimeMillis() - dummy) + "\t after write");

        }

    }

    /**
     * This method saves models in the jena tdb
     * @param pathToTDB the path to the tdb directory
     * @param modelNameArList a list with names for models
     * @param addedModelArList a list with models
     * @return return a success message
     */
    public String addModelsInTDB(String pathToTDB, ArrayList<String> modelNameArList, ArrayList<Model> addedModelArList) {

        //long dummy = System.currentTimeMillis();

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        int loopsize = addedModelArList.size();

        dataset.begin( ReadWrite.WRITE );
        try {

            // create the default model/graph
            for (int i = 0; i < loopsize; i++ ) {

                Model namedModel = dataset.getNamedModel(modelNameArList.get(i));

                // add a new model to the specific named graph in the Jena TDB
                namedModel.add(addedModelArList.get(i));


            }

            // finish the "WRITE" transaction
            dataset.commit();
            //System.out.println((System.currentTimeMillis() - dummy) + "\t after commit");

            return "The triples were successfully saved in the named graph called: " + modelNameArList;

        } finally {
            // close the dataset
            dataset.end();
            // write a message if the import was successfully

        }

    }


    /**
     * This method saves models in the jena tdb and in a corresponding lucene index
     * @param pathToTDB the path to the tdb directory
     * @param pathToLucene the path to the corresponding lucene directory
     * @param modelNameArList a list with names for models
     * @param addedModelArList a list with models
     * @return return a success message
     */
    public String addModelsInTDBLucene(String pathToTDB, String pathToLucene, ArrayList<String> modelNameArList, ArrayList<Model> addedModelArList) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        // Define the index mapping
        EntityDefinition entDef = new EntityDefinition("uri", "text", "graph");

        entDef.setPrimaryPredicate(RDFS.label.asNode());

        // Lucene, in memory.
        Directory dir = null;

        try {

            dir = new SimpleFSDirectory(new File(pathToLucene).toPath());

        } catch (IOException e) {

            e.printStackTrace();

        }

        // Join together into a dataset
        dataset = TextDatasetFactory.createLucene(dataset, dir, new TextIndexConfig(entDef));

        int loopSize = addedModelArList.size();

        dataset.begin(ReadWrite.WRITE);

        try {
            // create the default model/graph

            for (int i = 0; i < loopSize; i++ ) {

                Model namedModel = dataset.getNamedModel(modelNameArList.get(i));

                // add a new model to the specific named graph in the Jena TDB
                namedModel.add(addedModelArList.get(i));

            }

            // finish the "WRITE" transaction
            //System.out.println((System.currentTimeMillis() - dummy) + "\t after commit");

            dataset.commit();

            return "The triples were successfully saved in the named graph called: " + modelNameArList + " and the Lucene directory.";

        } finally {

            // close the dataset
            dataset.end();
            // close the dataset
            dataset.close();

        }



    }


    /**
     * This method check if a named model exist in the tdb or not
     * @param pathToTDB the path to the tdb directory
     * @param namedModelString contains the URI of a named model
     * @return "true" if the model exist otherwise "false"
     */
    public boolean modelExistInTDB(String pathToTDB, String namedModelString) {

        // create the TDB-Dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        // Start a Read transaction
        dataset.begin( ReadWrite.READ );

        try {

            return dataset.containsNamedModel(namedModelString);

        } finally {

            // close the dataset
            dataset.end();
            // close the dataset
            dataset.close();

        }

    }


    /**
     * This method check if a statement exist in the tdb or not
     * @param pathToTDB the path to the tdb directory
     * @param sparqlQueryString contains the SPARQL-Query to find the result value
     * @return "true" if the statement exist otherwise "false"
     */
    public boolean statementExistInTDB(String pathToTDB, String sparqlQueryString) {

        // create the TDB-Dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        // Start a Read transaction
        dataset.begin( ReadWrite.READ );

        try {

            // create a query execution
            QueryExecution qExec = QueryExecutionFactory.create(sparqlQueryString, dataset);

            return qExec.execAsk();

        } finally {

            // close the dataset
            dataset.end();

        }

    }


    /**
     * This method execute a SPARQL text query and provide a structured JSONArray for the query result.
     * @param pathToTDB the path to the tdb directory
     * @param pathToLucene the path to the corresponding lucene directory
     * @param sparqlQueryString contains the SPARQL-Query to find the result value
     * @return an JSONArray with the result data for an autocomplete field
     */
    public JSONArray pullAutoCompleteFromTDBLucene(String pathToTDB, String pathToLucene, String sparqlQueryString) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        // Define the index mapping
        EntityDefinition entDef = new EntityDefinition("uri", "text", "graph");

        entDef.setPrimaryPredicate(RDFS.label.asNode());

        // Lucene, in memory.
        Directory dir = null;

        try {

            dir = new SimpleFSDirectory(new File(pathToLucene).toPath());

        } catch (IOException e) {

            e.printStackTrace();

        }

        TextIndexConfig textIndexConfig = new TextIndexConfig(entDef);

        // create a TDB-dataset
        dataset = TextDatasetFactory.createLucene(dataset, dir, textIndexConfig);

        dataset.begin(ReadWrite.READ);

        try {

            Query query = QueryFactory.create(sparqlQueryString);

            QueryExecution qExec = QueryExecutionFactory.create(query, dataset);

            // Select a RDF-result set with data from the Jena-TDB
            ResultSet resultsSel = qExec.execSelect();

            return convertAutoCompleteResultToJSONArray(resultsSel);


        } finally {

            // close the dataset
            dataset.end();
            // close the dataset
            dataset.close();

        }

    }


    /**
     * This method execute a SPARQL text query and provide a result set for the query.
     * @param pathToTDB the path to the tdb directory
     * @param sparqlQueryString the path to the corresponding lucene directory
     * @return a ResultSet for a query
     */
    public ResultSet pullMultipleSelectDataFromTDB(String pathToTDB, String sparqlQueryString) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        dataset.begin(ReadWrite.READ);

        try {

            Query query = QueryFactory.create(sparqlQueryString);

            QueryExecution qExec = QueryExecutionFactory.create(query, dataset);

            // Select a RDF-result set with data from the Jena-TDB
            return qExec.execSelect();

        } finally {

            // close the dataset
            dataset.end();
            // close the dataset
            dataset.close();

        }

    }


    /**
     *      Return a Model from a Jena-TDB. To pull the model from the TDB use a SPARQL-Query.
     *
     *  <p>
     *      You can use a ASK, CONSTRUCT, DESCRIBE or SELECT SPARQL Query.
     */
    public Model pullDataFromTDB(String pathToTDB, String sparqlQueryString) {

        // create the TDB-Dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        //create a default model
        Model results = null;

        // Start a Read transaction
        dataset.begin( ReadWrite.READ );

        try {

            // create a Query
            Query query = QueryFactory.create(sparqlQueryString);

            // create a Jena-TDB-Interface
            QueryExecution qExec = QueryExecutionFactory.create(query, dataset);

            // check the query type
            int queryType = query.getQueryType();

            switch (queryType) {

                // ASK-SPARQL-Query
                case Query.QueryTypeAsk:

                    // boolean result from the Jena-TDB
                    boolean result_bool = qExec.execAsk();

                    // convert boolean result to model
                    results = RDFOutput.encodeAsModel(result_bool);

                    break;

                // CONSTRUCT-SPARQL-Query
                case Query.QueryTypeConstruct:

                    // construct a model with data from the Jena-TDB
                    results = qExec.execConstruct();

                    break;

                // DESCRIBE-SPARQL-Query

                case Query.QueryTypeDescribe:

                    // describe a model with data from the Jena-TDB
                    results = qExec.execDescribe();

                    break;

                case Query.QueryTypeSelect:

                    // select a result set with data from the Jena-TDB
                    ResultSet results_sel = qExec.execSelect();

                    // convert result set to model
                    results = RDFOutput.encodeAsModel(results_sel);

                    break;

            }

            // finish the "READ" transaction
            dataset.commit();
        } finally {

            // close the dataset
            dataset.end();

        }

        return results;
    }


    /**
     * This method returns a copy of a named model in the jena tdb.
     * @param pathToTDB the path to the tdb directory
     * @param modelURI the uri of a named model
     * @return a copy of a named model
     */
    public Model pullNamedModelFromTDB(String pathToTDB, String modelURI) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        try (RDFConnection conn = RDFConnectionFactory.connect(dataset)) {

            conn.begin(ReadWrite.WRITE);

            try {

                Model model = conn.fetch(modelURI);

                Model outputModel = ModelFactory.createDefaultModel().add(model);

                conn.commit();

                return outputModel;

            } finally {
                // close the dataset
                conn.end();
                // write a message if the import was successfully
                System.out.println("The model " + modelURI + " was successfully loaded from directory: " + pathToTDB);

            }

        }

    }


    /**
     *      Create an arbitrary RDF-output file from an arbitrary Jena-TDB. To pull the model from the TDB use
     *      a SPARQL-Query.
     *
     *  <p>
     *      You can use an ASK, CONSTRUCT, DESCRIBE or SELECT Query.
     */
    public void pullDataFromTDB(String pathToTDB, String outputFileName, String sparqlQueryString)
            throws IOException {

        // create the TDB-Dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        // Start a Read transaction
        dataset.begin( ReadWrite.READ );

        // create the outputStream
        FileWriter out = new FileWriter( outputFileName + ".owl" );

        // create the outputStream
        FileWriter out_rj = new FileWriter( outputFileName + ".jsonld" );

        // create the outputStream
        FileWriter out_nt = new FileWriter( outputFileName + ".nt" );

        // create the outputStream
        FileOutputStream sel_out = new FileOutputStream( outputFileName + ".owl");

        //create a default model
        Model results;

        //create a default base namespace
        String BaseNamespace;

        // create a default RDFWriter
        RDFWriter w;

        // create a Query
        Query query = QueryFactory.create(sparqlQueryString);

        // create a Jena-TDB-Interface
        QueryExecution qexec = QueryExecutionFactory.create(query, dataset);

        // check the query type
        int queryType = query.getQueryType();

        switch (queryType) {

            // ASK-SPARQL-Query
            case Query.QueryTypeAsk:

                boolean result_bool = qexec.execAsk();

                FileOutputStream fOS= new FileOutputStream(outputFileName + ".txt");

                DataOutputStream askOut = new DataOutputStream(fOS);

                askOut.writeBoolean(result_bool);

                System.out.println("Output file was generated. The file contains a boolean value.");

                break;

            // CONSTRUCT-SPARQL-Query
            case Query.QueryTypeConstruct:

                // construct a RDF-model with data from the Jena-TDB
                results = qexec.execConstruct();


                //results.write(System.out, "RDF/XML");

                // get the xml:base from the base namespace of the ontology
                BaseNamespace = results.getNsPrefixURI("");
                //results.setNsPrefix("", BaseNamespace); <--- still in progress
                //BaseNamespace = BaseNamespace.substring(0,BaseNamespace.length()-1);


                // fill RDFWriter with some property for the output-database
                w = results.getWriter("RDF/XML");
                w.setProperty("attributeQuoteChar","\"");
                w.setProperty("xmlbase",BaseNamespace);
                w.setProperty("showDoctypeDeclaration",true);
                w.setProperty("tab",8);

                try {
                    // write the ontology to the output-database
                    w.write(results, out, BaseNamespace);
                    results.write(out_rj,"JSON-LD");
                    results.write(out_nt,"N-TRIPLES");
                }
                finally {
                    try {
                        // close the outputStream
                        out.close();
                        // write a message if the export was successfully
                        System.out.println(outputFileName + " was generated.");
                        System.out.println("Data export successfully!");
                    }
                    catch (IOException closeException) {
                        // ignore
                    }
                }

                break;

            // DESCRIBE-SPARQL-Query
            case Query.QueryTypeDescribe:


                // describe a RDF-model with data from the Jena-TDB
                results = qexec.execDescribe();


                //results.write(System.out, "RDF/XML");

                // get the xml:base from the base namespace of the ontology
                BaseNamespace = results.getNsPrefixURI("");
                //results.setNsPrefix("", BaseNamespace); <--- still in progress
                //BaseNamespace = BaseNamespace.substring(0,BaseNamespace.length()-1);


                // fill RDFWriter with some property for the output-database
                w = results.getWriter("RDF/XML");
                w.setProperty("attributeQuoteChar","\"");
                w.setProperty("xmlbase",BaseNamespace);
                w.setProperty("showDoctypeDeclaration",true);
                w.setProperty("tab",8);

                try {
                    // write the ontology to the output-database
                    w.write(results,out,BaseNamespace);
                }
                finally {
                    try {
                        // close the outputStream
                        out.close();
                        // write a message if the export was successfully
                        System.out.println(outputFileName + " was generated.");
                        System.out.println("Data export successfully!");
                    }
                    catch (IOException closeException) {
                        // ignore
                    }
                }


                break;

            // SELECT-SPARQL-Query
            case Query.QueryTypeSelect:

                // Select a RDF-result set with data from the Jena-TDB
                ResultSet results_sel = qexec.execSelect();

                try {
                    // write the result set to the output-file
                    RDFOutput.outputAsRDF(sel_out, "RDF/XML", results_sel);
                }
                finally {
                    try {
                        // close the outputStream
                        out.close();
                        // write a message if the export was successfully
                        System.out.println(outputFileName + " was generated.");
                        System.out.println("Data export successfully!");
                    }
                    catch (IOException closeException) {
                        // ignore
                    }
                }


                break;
        }

        // close the Jena-TDB-Interface
        qexec.close();

        // finish the "READ" transaction
        dataset.commit();

        // close the Dataset
        dataset.close();

    }


    /**
     * This method provide the URI of an resource or a literal.
     * @param pathToTDB the path to the tdb directory
     * @param sparqlQueryString contains the SPARQL-Query to find the result value
     * @param resultVar is the name of the result variable
     * @return a resource or literal as a string
     */
    public JSONArray pullMultipleDataFromTDB(String pathToTDB, String sparqlQueryString, String resultVar) {

        // create the TDB-Dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        JSONArray valuesFromTDB = new JSONArray();

        // Start a Read transaction
        dataset.begin(ReadWrite.READ);

        try {

            // create a Query
            Query query = QueryFactory.create(sparqlQueryString);

            // create a Jena-TDB-Interface
            QueryExecution qexec = QueryExecutionFactory.create(query, dataset);

            // check the query type
            int queryType = query.getQueryType();

            switch (queryType) {

                case Query.QueryTypeSelect:

                    // select a result set with data from the Jena-TDB
                    ResultSet resultsSel = qexec.execSelect();

                    while (resultsSel.hasNext()) {

                        QuerySolution querySolution = resultsSel.next();

                        if (querySolution.get(resultVar).isResource()) {

                            valuesFromTDB.put(querySolution.getResource(resultVar).toString());

                        } else if (querySolution.get(resultVar).isLiteral()) {

                            valuesFromTDB.put(querySolution.getLiteral(resultVar).getLexicalForm());

                        }



                    }

                    break;

            }

            // finish the "READ" transaction
            dataset.commit();
        }
        finally {

            // close the dataset
            dataset.end();

        }

        return valuesFromTDB;

    }


    /**
     * This method provide the URI of an resource or a literal.
     * @param pathToTDB the path to the tdb directory
     * @param sparqlQueryString contains the SPARQL-Query to find the result value
     * @param resultVar is the name of the result variable
     * @return a resource or literal as a string
     */
    public String pullSingleDataFromTDB(String pathToTDB, String sparqlQueryString, String resultVar) {

        // create the TDB-Dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        //create a default model
        String valueFromTDB = "";

        // Start a Read transaction
        dataset.begin(ReadWrite.READ);

        try {

            // create a Query
            Query query = QueryFactory.create(sparqlQueryString);

            // create a Jena-TDB-Interface
            QueryExecution qexec = QueryExecutionFactory.create(query, dataset);

            // check the query type
            int queryType = query.getQueryType();

            switch (queryType) {

                case Query.QueryTypeSelect:

                    // select a result set with data from the Jena-TDB
                    ResultSet resultsSel = qexec.execSelect();

                    if (resultsSel.hasNext()) {

                        QuerySolution querySolution = resultsSel.next();

                        if (querySolution.get(resultVar).isResource()) {

                            valueFromTDB = querySolution.getResource(resultVar).toString();

                        } else if (querySolution.get(resultVar).isLiteral()) {

                            valueFromTDB = querySolution.getLiteral(resultVar).getLexicalForm();

                        }

                    }

                    break;

            }

            // finish the "READ" transaction
            dataset.commit();
        }
        finally {

            // close the dataset
            dataset.end();

        }

        return valueFromTDB;

    }

    /**
     * This method pull a literal with his datatype from the jena tdb
     * @param pathToTDB the path to the tdb directory
     * @param sparqlQueryString contains the SPARQL-Query to find the result value
     * @param resultVar is the name of the result variable
     * @return a literal with datatype as a string
     */
    public String pullSingleLiteralWithDatatypeFromTDB(String pathToTDB, String sparqlQueryString, String resultVar) {

        // create the TDB-Dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        //create a default model
        String valueFromTDB = "";

        // Start a Read transaction
        dataset.begin(ReadWrite.READ);

        try {

            // create a Query
            Query query = QueryFactory.create(sparqlQueryString);

            // create a Jena-TDB-Interface
            QueryExecution qexec = QueryExecutionFactory.create(query, dataset);

            // select a result set with data from the Jena-TDB
            ResultSet resultsSel = qexec.execSelect();

            if (resultsSel.hasNext()) {

                QuerySolution querySolution = resultsSel.next();

                valueFromTDB = querySolution.getLiteral(resultVar).toString();

            }

            // finish the "READ" transaction
            dataset.commit();
        }
        finally {

            // close the dataset
            dataset.end();

        }

        return valueFromTDB;

    }


    /**
     *      Return a String from a Jena-TDB. The string contains the result of a SPARQL-Query.
     *
     *  <p>
     *      You can use a ASK, CONSTRUCT, DESCRIBE or SELECT SPARQL Query.
     */
    public String pullStringDataFromTDB(String pathToTDB, Query sparqlQueryString, String outputFormat) {

        //long dummy = System.currentTimeMillis();

        //System.out.println((System.currentTimeMillis() - dummy) + "\t start");

        // create the TDB-Dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        //System.out.println((System.currentTimeMillis() - dummy) + "\t after dataset");

        //create a default model
        Model results;

        String outputString = "";

        // Start a Read transaction
        dataset.begin( ReadWrite.READ );

        try {

            // create a Jena-TDB-Interface
            QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, dataset);

            // check the query type
            int queryType = sparqlQueryString.getQueryType();

            switch (queryType) {

                // ASK-SPARQL-Query
                case Query.QueryTypeAsk:

                    // boolean result from the Jena-TDB
                    boolean result_bool = qexec.execAsk();

                    // convert boolean result to a string
                    outputString = Boolean.toString(result_bool);

                    break;

                // CONSTRUCT-SPARQL-Query
                case Query.QueryTypeConstruct:

                    // construct a model with data from the Jena-TDB
                    results = qexec.execConstruct();

                    // create a string writer to convert the model to a string
                    StringWriter outConstruct = new StringWriter();

                    // write model with outputFormat to string writer
                    results.write(outConstruct, outputFormat);

                    // convert string writer to string
                    outputString = outConstruct.toString();

                    break;

                // DESCRIBE-SPARQL-Query

                case Query.QueryTypeDescribe:

                    // describe a model with data from the Jena-TDB
                    results = qexec.execDescribe();

                    // create a string writer to convert the model to a string
                    StringWriter outDescribe = new StringWriter();

                    // write model with outputFormat to string writer
                    results.write(outDescribe, outputFormat);

                    // convert string writer to string
                    outputString = outDescribe.toString();

                    break;

                case Query.QueryTypeSelect:

                    // select a result set with data from the Jena-TDB
                    ResultSet results_sel = qexec.execSelect();

                    // convert result set to string
                    outputString = ResultSetFormatter.asText(results_sel);

                    break;

            }
            // finish the "READ" transaction
            dataset.commit();
        }
        finally {

            //System.out.println((System.currentTimeMillis() - dummy) + "\t after read transaction");

            // close the dataset
            dataset.end();

        }

        return outputString;
    }

    /**
     *      Push an arbitrary RDF-file in the destination default graph
     */

    public String pushDataInTDB(String pathToTDB, String inputFileName) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        dataset.begin( ReadWrite.WRITE );
        try {
            // create the default model/graph
            Model tdb = dataset.getDefaultModel();
            // load model to the Jena TDB
            TDBLoader.loadModel(tdb, inputFileName);
            // finish the "WRITE" transaction
            dataset.commit();

            return inputFileName + " was successfully stored in a default graph!";
        }
        finally {
            // close the dataset
            dataset.end();
            // write a message if the import was successfully
            System.out.println(inputFileName + " was successfully stored in a default graph!");
        }

    }

    /**
     *      Push an arbitrary RDF-file in the destination named graph
     */

    public String pushDataInTDB(String pathToTDB, String nameTDB, String inputFileName) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        dataset.begin( ReadWrite.WRITE );
        try {
            // create the default model/graph
            Model tdb = dataset.getNamedModel(nameTDB);
            // load model to the Jena TDB
            TDBLoader.loadModel(tdb, inputFileName);
            // finish the "WRITE" transaction
            dataset.commit();

            return inputFileName + " was successfully stored in the named graph: " + nameTDB;
        }
        finally {
            // close the dataset
            dataset.end();
            // write a message if the import was successfully
            System.out.println(inputFileName + " was successfully stored in the named graph: " + nameTDB);
        }
    }

    /**
     *      Remove arbitrary Statements from the destination Jena-TDB
     */

    public String removeModelDataInTDB(String pathToTDB, String nameTDB, Model modelWithStatements) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        // initialize a Statement array list
        ArrayList<Statement> statementsToRemove = new ArrayList<>();

        // create the statement iterator for the input model
        StmtIterator stmtIterator = modelWithStatements.listStatements();

        // iterate over all statements
        while (stmtIterator.hasNext()) {
            // add the current statement to the array list
            statementsToRemove.add(stmtIterator.next());
        }

        dataset.begin( ReadWrite.WRITE );
        try {
            // create the default model/graph
            Model tdb = dataset.getNamedModel(nameTDB);
            // remove the statements from the specific named graph in the Jena TDB
            tdb.remove(statementsToRemove);
            // finish the "WRITE" transaction
            dataset.commit();

            return "The triple was successfully removed from the named graph called: " + nameTDB;
        }
        finally {
            // close the dataset
            dataset.end();
            // write a message if the delete was successfully

        }

    }


    /**
     * This method removes triples from named graphs in the jena tdb
     * @param pathToTDB the path to the dataset
     * @param modelNameArList a list with the uri of the named graphs
     * @param modelsToRemove a list with triples, which should remove from the named graph
     * @return return a success message
     */
    public String removeModelsFromTDB(String pathToTDB, ArrayList<String> modelNameArList, ArrayList<Model> modelsToRemove) {

        //long dummy = System.currentTimeMillis();

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        int loopSize = modelsToRemove.size();

        dataset.begin( ReadWrite.WRITE );
        try {
            // create the default model/graph

            for (int i = 0; i < loopSize; i++ ) {

                // get the named model
                Model ngToReplace = dataset.getNamedModel(modelNameArList.get(i));

                ArrayList<Statement> axiomsToDelete = new ArrayList<>();
                ArrayList<Statement> axiomsToDeleteInModelsToRemove = new ArrayList<>();

                ResIterator modelsToRemoveResIter = modelsToRemove.get(i).listSubjects();

                while (modelsToRemoveResIter.hasNext()) {
                    // check if a bNode must deleted

                    Resource bNodeToDeleteInModelsToRemove = modelsToRemoveResIter.next();

                    if (bNodeToDeleteInModelsToRemove.isAnon()) {

                        StmtIterator modelsToRemoveStmtIter = bNodeToDeleteInModelsToRemove.listProperties();

                        while (modelsToRemoveStmtIter.hasNext()) {

                            Statement modelsToRemoveStmt = modelsToRemoveStmtIter.next();

                            if (modelsToRemoveStmt.getPredicate().equals(OWL2.annotatedSource)) {
                                // specify bNode axiom

                                Property annotatedSourceProperty = modelsToRemoveStmt.getPredicate();
                                RDFNode annotatedSourceObject = modelsToRemoveStmt.getObject();

                                ResIterator ngToReplaceResIter = ngToReplace.listSubjectsWithProperty(annotatedSourceProperty, annotatedSourceObject);

                                while (ngToReplaceResIter.hasNext()) {

                                    Resource bNodeToDelete = ngToReplaceResIter.next();

                                    StmtIterator ngToReplaceStmtIter = bNodeToDelete.listProperties();

                                    while (ngToReplaceStmtIter.hasNext()) {

                                        Statement ngToReplaceStmt = ngToReplaceStmtIter.next();

                                        // remove blank nodes before update
                                        axiomsToDelete.add(ngToReplaceStmt);
                                        axiomsToDeleteInModelsToRemove.add(modelsToRemoveStmt);

                                    }

                                }

                            }

                        }

                    }

                }

                for (int j = 0; j < axiomsToDelete.size(); j++) {

                    axiomsToDelete.get(j).remove();
                    axiomsToDeleteInModelsToRemove.get(j).remove();

                }

                // build a list with all statements to delete.
                List<Statement> stmtList = modelsToRemove.get(i).listStatements().toList();

                ngToReplace.remove(stmtList);

            }

            // finish the "WRITE" transaction
            dataset.commit();
            //System.out.println((System.currentTimeMillis() - dummy) + "\t after commit");

            return "The triples were successfully deleted from the named graph called: " + modelNameArList;
        }
        finally {
            // close the dataset
            dataset.end();
            // write a message if the import was successfully

        }



    }


    /**
     *      Remove an arbitrary named graph from the destination Jena-TDB
     */

    public void removeNamedModelFromTDB(String pathToTDB, String graphNameTDB) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        dataset.begin( ReadWrite.WRITE );
        try {
            // create the default model/graph
            dataset.removeNamedModel(graphNameTDB);
            // finish the "WRITE" transaction
            dataset.commit();
        }
        finally {
            // close the dataset
            dataset.end();
            // write a message if the import was successfully
            System.out.println("Data was successfully removed from the tuplestore!");
        }
    }


    /**
     * This method removes model(s) from the jena tdb
     * @param pathToTDB the path to the dataset
     * @param modelNameArList a list with the uri of the named graphs
     */
    public void removeNamedModelsFromTDB(String pathToTDB, ArrayList<String> modelNameArList) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        dataset.begin( ReadWrite.WRITE );
        try {

            for (String aModelNameArList : modelNameArList) {

                dataset.removeNamedModel(aModelNameArList);

            }


            // finish the "WRITE" transaction
            dataset.commit();
        }
        finally {
            // close the dataset
            dataset.end();
            // write a message if the import was successfully
            System.out.println("Data was successfully removed from the tuplestore!");
        }
    }


    /**
     * This method updates statement of a named graph in the jena tdb.
     * @param pathToTDB the path to the dataset
     * @param namedModel contains the URI of a named graph
     * @param deprecatedStmtsModel contains deprecated statements, which must remove from the named model
     * @param newStmtsModel contains new statements, which must add to the named model
     */
    public void updateModelDataInTDB(String pathToTDB, String namedModel, Model deprecatedStmtsModel, Model newStmtsModel) {

        // create a TDB-dataset
        Dataset dataset = TDBFactory.createDataset(pathToTDB);

        // initialize a Statement array list
        ArrayList<Statement> statementsToRemove = new ArrayList<>();

        // create the statement iterator for the input model
        StmtIterator deprecatedStmtIterator = deprecatedStmtsModel.listStatements();

        // iterate over all statements
        while (deprecatedStmtIterator.hasNext()) {
            // add the current statement to the array list
            statementsToRemove.add(deprecatedStmtIterator.next());
        }

        // initialize a Statement array list
        ArrayList<Statement> statementsToAdd = new ArrayList<>();

        // create the statement iterator for the input model
        StmtIterator addStmtIterator = newStmtsModel.listStatements();

        // iterate over all statements
        while (addStmtIterator.hasNext()) {
            // add the current statement to the array list
            statementsToAdd.add(addStmtIterator.next());
        }

        dataset.begin( ReadWrite.WRITE );
        try {
            // create the default model/graph
            Model tdb = dataset.getNamedModel(namedModel);
            // remove the statements from the specific named graph in the Jena TDB
            tdb.remove(statementsToRemove);
            // add the statements to a specific named graph in the Jena TDB
            tdb.add(statementsToAdd);
            // finish the "WRITE" transaction
            dataset.commit();
        } finally {
            // close the dataset
            dataset.end();
            // write a message if the delete was successfully
            System.out.println("The triple(s) was successfully updated in the named graph called: " + namedModel);

        }

    }
}
