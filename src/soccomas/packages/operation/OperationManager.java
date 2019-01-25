/*
 * Created by Roman Baum on 15.02.16.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages.operation;


import soccomas.basic.ApplicationConfigurator;
import soccomas.basic.SOCCOMASIDChecker;
import soccomas.basic.ShowEntryButton;
import soccomas.mongodb.MongoDBConnection;
import soccomas.packages.JenaIOTDBFactory;
import soccomas.packages.querybuilder.FilterBuilder;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.packages.querybuilder.SPARQLFilter;
import soccomas.vocabulary.SCBasic;
import soccomas.vocabulary.SCMDBMD;
import soccomas.vocabulary.SprO;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class OperationManager {

    private String pathToOntologies = ApplicationConfigurator.getPathToApplicationOntologyStore();

    private String mdbCoreID = "", mdbEntryID = "", mdbUEID = "";

    private Model overlayModel = ModelFactory.createDefaultModel();

    private MongoDBConnection mongoDBConnection;

    private JSONObject keywordsFromInputForSubsequentlyWA = new JSONObject();

    /**
     * Default constructor
     */
    public OperationManager(MongoDBConnection mongoDBConnection) {

        this.mongoDBConnection = mongoDBConnection;

    }


    /**
     * A constructor which provide a specific MDBUserEntryID for further calculations
     * @param mdbUEID contains the uri of the MDBUserEntryID
     */
    public OperationManager(String mdbUEID, MongoDBConnection mongoDBConnection) {

        this.mdbUEID = mdbUEID;
        this.mongoDBConnection = mongoDBConnection;

    }


    /**
     * A constructor which provide a specific MDBCoreID, MDBEntryID and MDBUserEntryID for further calculations
     * @param mdbCoreID contains the uri of the MDBCoreID
     * @param mdbEntryID contains the uri of the MDBEntryID
     * @param mdbUEID contains the uri of the MDBUserEntryID
     */
    public OperationManager(String mdbCoreID, String mdbEntryID, String mdbUEID, MongoDBConnection mongoDBConnection) {

        this.mdbCoreID = mdbCoreID;

        this.mdbEntryID = mdbEntryID;

        this.mdbUEID = mdbUEID;

        this.mongoDBConnection = mongoDBConnection;

    }

    public JSONObject checkAutocomplete(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        String individualURI = getIndividualURIForLocalIDFromMongoDB(jsonInputObject);

        InputInterpreter inputinterpreter = new InputInterpreter(individualURI, jsonInputObject, this.mongoDBConnection);

        return inputinterpreter.checkAutocomplete(connectionToTDB);

    }

    /**
     * This method reads and coordinates the input data for a panel
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject checkInput(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        String classURI, individualURI;

        if (jsonInputObject.get("value").toString().equals("show_localID")) {
            // case: change selected part in partonomy

            classURI = SCMDBMD.changePartButtonItem.toString();

            individualURI = SCMDBMD.basicMDBCOMPONENTChangePartButton.toString();

        } else if (jsonInputObject.get("value").toString().equals("show_tab_localID")) {
            // case: change tab of selected part in partonomy

            classURI = SCMDBMD.changePartTabButtonItem.toString();

            individualURI = SCMDBMD.basicMDBCOMPONENTChangePartTabButton.toString();

            jsonInputObject.put("value", "show_localID");

        } else {

            classURI = getClassURIForLocalIDFromMongoDB(jsonInputObject);

            individualURI = getIndividualURIForLocalIDFromMongoDB(jsonInputObject);

            jsonInputObject = getKeywordsForLocalIDFromMongoDB(jsonInputObject);

        }

        InputInterpreter inputinterpreter = new InputInterpreter(individualURI, jsonInputObject, this.overlayModel, this.mongoDBConnection);

        JSONObject outputJSON = inputinterpreter.checkInput(classURI, connectionToTDB);

        this.overlayModel = inputinterpreter.getOverlayModel();

        return outputJSON;

    }

    /**
     * This method reads and coordinates the input data for a panel
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject checkURI(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        SOCCOMASIDChecker mdbIDChecker = new SOCCOMASIDChecker();

        if (mdbIDChecker.isMDBID(jsonInputObject.get("value").toString(), connectionToTDB)) {

            String mdbResourcePart = jsonInputObject.get("value").toString();

            mdbResourcePart = mdbResourcePart.substring(mdbResourcePart.lastIndexOf("/") + 1);

            if (mdbIDChecker.isMDBEntryID()) {

                mdbResourcePart = mdbResourcePart + "#" + SCBasic.entryCompositionNamedGraph.getLocalName() + "_1";

                jsonInputObject.put("html_form", mdbResourcePart);

                jsonInputObject.put("localID", ShowEntryButton.localIndividualID);

                String individualURI = ShowEntryButton.individualID;

                String classURI = ShowEntryButton.classID;

                InputInterpreter inputInterpreter = new InputInterpreter(individualURI, jsonInputObject, this.overlayModel, this.mongoDBConnection);

                JSONObject outputJSON = inputInterpreter.checkInput(classURI, connectionToTDB);

                this.overlayModel = inputInterpreter.getOverlayModel();

                return outputJSON;

            }

        }

        return null;
    }

    /**
     * This method reads and coordinates the input data for a panel
     * @param jsonInputObject contains the information for the calculation
     * @param jsonOutputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject addVersionType(JSONObject jsonInputObject, JSONObject jsonOutputObject, JenaIOTDBFactory connectionToTDB) {

        InputInterpreter inputInterpreter = new InputInterpreter(jsonInputObject, this.mongoDBConnection);

        String mdbID;

        if (jsonOutputObject.has("html_form")) {

            if (jsonOutputObject.get("html_form").toString().contains("resource/")) {

                mdbID = ApplicationConfigurator.getDomain() + "/" + jsonOutputObject.get("html_form").toString();

            } else {

                mdbID = ApplicationConfigurator.getDomain() + "/resource/" + jsonOutputObject.get("html_form").toString();

            }

            if (mdbID.contains("#")) {

                mdbID = mdbID.substring(0, mdbID.lastIndexOf("#"));

                System.out.println("mdbID = " + mdbID);

                jsonOutputObject = inputInterpreter.addVersionType(jsonOutputObject, mdbID, connectionToTDB);

            } else {

                System.out.println("INFO: Output is no mdb entry status!");

                jsonOutputObject.put("inputIsActive" , "true");

            }

        } else {

            System.out.println("INFO: Output is no mdb entry status!");

            jsonOutputObject.put("inputIsActive" , "true");

        }

        return jsonOutputObject;

    }


    /**
     * This method reads and coordinates the input data for a panel
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject checkInputForListEntry(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        InputInterpreter inputInterpreter = new InputInterpreter(jsonInputObject, this.mongoDBConnection);

        return inputInterpreter.checkInputForListEntry(connectionToTDB);

    }

    /**
     * This method reads and coordinates the input data for an overlay queue
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return 'true' if all steps are proceed, else 'false'
     */
    public boolean checkOverlayQueueInput(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        String newNS = jsonInputObject.get("mdbentryid").toString();

        String mongoDBKey = jsonInputObject.get("html_form").toString();

        String session = jsonInputObject.get("connectSID").toString();

        System.out.println("jsonFromMongoDBkeyjsonnewNS = " + newNS);

        System.out.println("jsonFromMongoDBkeyjsonmongoDBkey = " + mongoDBKey);

        if (this.mongoDBConnection.documentExist("mdb-prototyp", session, mongoDBKey)) {

            JSONArray overlayJSON = this.mongoDBConnection.getJSONArrayForKey("mdb-prototyp", session, mongoDBKey);

            for (int i = 0; i < overlayJSON.length(); i++) {

                String oldIndividualID = overlayJSON.getJSONObject(i).get("individualID").toString();

                if (oldIndividualID.contains("dummy-overlay")) {

                    String newIndividualID = newNS + "#" + ResourceFactory.createResource(oldIndividualID).getLocalName();

                    overlayJSON.getJSONObject(i).put("individualID", newIndividualID);

                }

            }

            this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", session, mongoDBKey, overlayJSON);

            JSONObject jsonFromMongoDB = this.mongoDBConnection.pullJSONObjectFromMongoDB(jsonInputObject);

            Iterator<String> keys = jsonFromMongoDB.keys();

            JSONArray jsonInputQueue = new JSONArray();

            while (keys.hasNext()) {

                String key = keys.next();

                if (StringUtils.isNumeric(key)) {

                    if (jsonInputObject.has("subsequently_workflow_action")) {

                        if (jsonInputObject.get("subsequently_workflow_action").toString().equals("true")) {

                            JSONObject currInputJSON = jsonFromMongoDB.getJSONArray(key).getJSONObject(0);

                            if (jsonInputObject.has("subsequently_root")) {

                                currInputJSON.put("subsequently_root", jsonInputObject.get("subsequently_root").toString());

                            }

                            if (jsonInputObject.has("keywords_to_transfer")) {

                                currInputJSON.put("keywords_to_transfer", jsonInputObject.getJSONObject("keywords_to_transfer"));

                            }

                            jsonInputQueue.put(Integer.parseInt(key), currInputJSON);

                        }

                    } else {

                        jsonInputQueue.put(Integer.parseInt(key), jsonFromMongoDB.getJSONArray(key).getJSONObject(0));

                    }

                    System.out.println("jsonFromMongoDBkey = " + key);

                }

            }

            for (int i = 0; i < jsonInputQueue.length(); i++) {

                OperationManager queueOperationManager = new OperationManager(this.mongoDBConnection);

                // calculate the input from the mongoDB
                JSONObject outputJSON = queueOperationManager.checkInput(jsonInputQueue.getJSONObject(i), connectionToTDB);

                if (outputJSON.has("use_in_known_subsequent_WA")) {

                    JSONObject currKeywordFromInputForSubsequentlyWA = outputJSON.getJSONObject("use_in_known_subsequent_WA");

                    Iterator<String> currKeywordFromInputForSubsequentlyWAIter = currKeywordFromInputForSubsequentlyWA.keys();

                    while (currKeywordFromInputForSubsequentlyWAIter.hasNext()) {

                        String currKey = currKeywordFromInputForSubsequentlyWAIter.next();

                        this.keywordsFromInputForSubsequentlyWA.put(currKey, currKeywordFromInputForSubsequentlyWA.get(currKey).toString());

                    }

                }

            }

            return true;

        }

        return false;

    }


    /**
     * This method coordinates the input data for a subsequently redirection.
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject subsequentlyRedirected(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        JSONObject checkURIJSONInputObject = new JSONObject();

        checkURIJSONInputObject.put("type", "check_uri");
        checkURIJSONInputObject.put("value", jsonInputObject.get("redirect_to_hyperlink").toString());
        checkURIJSONInputObject.put("mdbueid", jsonInputObject.get("mdbueid").toString());
        checkURIJSONInputObject.put("mdbueid_uri", jsonInputObject.get("mdbueid_uri").toString());
        checkURIJSONInputObject.put("connectSID", jsonInputObject.get("connectSID").toString());
        checkURIJSONInputObject.put(SprO.sproVARIABLEKnownResourceA.toString(), jsonInputObject.get(SprO.sproVARIABLEKnownResourceA.toString()).toString());
        checkURIJSONInputObject.put(SprO.sproVARIABLEKnownResourceB.toString(), jsonInputObject.get(SprO.sproVARIABLEKnownResourceB.toString()).toString());

        JSONObject outputJSON = checkURI(checkURIJSONInputObject, connectionToTDB);

        return outputJSON;

    }

    /**
     * This method coordinates the input data for a subsequently workflow
     * @param jsonInputObject contains the information for the calculation
     * @param classURI contains the URI of an ontology class
     * @param keywordsToTransferJSON contains keywords from a preceding transition
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void checkSubsequentlyWorkflow(JSONObject jsonInputObject, String classURI,
                                          JSONObject keywordsToTransferJSON, JenaIOTDBFactory connectionToTDB) {

        String individualURI = getIndividualURIForClassURIFromJena(classURI, connectionToTDB);

        jsonInputObject = updateJSONInputObject(individualURI, keywordsToTransferJSON, jsonInputObject, connectionToTDB);

        InputInterpreter inputinterpreter = new InputInterpreter(individualURI, jsonInputObject, this.overlayModel, this.mongoDBConnection);

        inputinterpreter.checkInput(classURI, connectionToTDB);

        this.overlayModel = inputinterpreter.getOverlayModel();

    }

    /**
     * This method modifies the JSON input object.
     * @param individualURI contains the URI of an ontology individual
     * @param keywordsToTransferJSON contains keywords from a preceding transition
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    private JSONObject updateJSONInputObject(String individualURI, JSONObject keywordsToTransferJSON,
                                             JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        jsonInputObject.put("localID", ResourceFactory.createResource(individualURI).getLocalName());

        if (this.keywordsFromInputForSubsequentlyWA.keys().hasNext()) {

            Iterator<String> keywordsFromInputForSubsequentlyWAIter = this.keywordsFromInputForSubsequentlyWA.keys();

            while (keywordsFromInputForSubsequentlyWAIter.hasNext()) {

                String currKey = keywordsFromInputForSubsequentlyWAIter.next();

                if (keywordsToTransferJSON.has(currKey)) {

                    keywordsToTransferJSON.put(currKey, this.keywordsFromInputForSubsequentlyWA.get(currKey).toString());

                }

                this.keywordsFromInputForSubsequentlyWA.remove(currKey);

            }

        }

        jsonInputObject.put("precedingKeywords", keywordsToTransferJSON);

        Iterator<String> keywordsFromInputForSubsequentlyWAIter = keywordsToTransferJSON.keys();

        while (keywordsFromInputForSubsequentlyWAIter.hasNext()) {

            String currKey = keywordsFromInputForSubsequentlyWAIter.next();

            SOCCOMASIDChecker soccomasIDChecker = new SOCCOMASIDChecker();

            if (soccomasIDChecker.isMDBID(keywordsToTransferJSON.get(currKey).toString(), connectionToTDB)) {

                if (soccomasIDChecker.isMDBEntryID()) {

                    String htmlForm = jsonInputObject.get("html_form").toString();

                    String dummyPart = htmlForm.substring(htmlForm.lastIndexOf("/") + 1, htmlForm.indexOf("#"));

                    String entryIDPart = keywordsToTransferJSON.get(currKey).toString();

                    htmlForm = htmlForm.replaceAll(dummyPart, entryIDPart.substring(entryIDPart.lastIndexOf("/") + 1, entryIDPart.length()));

                    jsonInputObject.put("html_form", htmlForm);

                }

            }

        }

        jsonInputObject.put("localIDs", new JSONArray());

        return jsonInputObject;

    }


    /**
     * This method find the corresponding individual URI for a class URI.
     * @param classURI contains the URI of an ontology class
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an URI of an individual
     */
    private String getIndividualURIForClassURIFromJena(String classURI, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        SelectBuilder tripleSPO = new SelectBuilder();

        tripleSPO.addWhere("?s", RDF.type, "<" + classURI + ">");

        selectBuilder.addVar(selectBuilder.makeVar("?s"));

        selectBuilder.addGraph("?g", tripleSPO);

        String sparqlQueryString = selectBuilder.buildString();

        return connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?s");


    }


    /**
     * This method find the corresponding local identifier for an individual
     * @param jsonInputObject contains the information for the calculation
     * @return a modified JSONObject for further calculation
     */
    public JSONObject getKeywordsForLocalIDFromMongoDB (JSONObject jsonInputObject) {

        JSONArray jsonFromMongoDB = this.mongoDBConnection.pullListFromMongoDB(jsonInputObject);

        if (jsonInputObject.has("localIDs")) {

            JSONArray localIDs = jsonInputObject.getJSONArray("localIDs");

            if (localIDs != null) {

                for (int i = 0; i < localIDs.length(); i++) {

                    for (int j = 0; j < jsonFromMongoDB.length(); j++) {

                        if ((localIDs.getJSONObject(i).get("localID").toString()).equals(jsonFromMongoDB.getJSONObject(j).get("localID").toString()) &&
                                (jsonFromMongoDB.getJSONObject(j).has("keyword"))) {

                            // add the corresponding keyword information to the input
                            jsonInputObject.getJSONArray("localIDs").getJSONObject(i).put("keyword", jsonFromMongoDB.getJSONObject(j).get("keyword").toString());

                        }

                        if ((localIDs.getJSONObject(i).get("localID").toString()).equals(jsonFromMongoDB.getJSONObject(j).get("localID").toString()) &&
                                (jsonFromMongoDB.getJSONObject(j).has("keywordLabel"))) {

                            // add the corresponding keyword label information to the input
                            jsonInputObject.getJSONArray("localIDs").getJSONObject(i).put("keywordLabel", jsonFromMongoDB.getJSONObject(j).get("keywordLabel").toString());

                            if (jsonInputObject.getJSONArray("localIDs").getJSONObject(i).get("value") instanceof JSONObject) {

                                JSONObject valueObject = jsonInputObject.getJSONArray("localIDs").getJSONObject(i).getJSONObject("value");

                                jsonInputObject.getJSONArray("localIDs").getJSONObject(i).put("valueLabel", valueObject.get("label").toString());

                                jsonInputObject.getJSONArray("localIDs").getJSONObject(i).put("value", valueObject.get("resource").toString());

                            }

                        }

                        if ((localIDs.getJSONObject(i).get("localID").toString()).equals(jsonFromMongoDB.getJSONObject(j).get("localID").toString()) &&
                                (jsonFromMongoDB.getJSONObject(j).has("keywordDefinition"))) {

                            // add the corresponding keyword label information to the input
                            jsonInputObject.getJSONArray("localIDs").getJSONObject(i).put("keywordDefinition", jsonFromMongoDB.getJSONObject(j).get("keywordDefinition").toString());

                            if (jsonInputObject.getJSONArray("localIDs").getJSONObject(i).get("value") instanceof JSONObject) {

                                JSONObject valueObject = jsonInputObject.getJSONArray("localIDs").getJSONObject(i).getJSONObject("value");

                                jsonInputObject.getJSONArray("localIDs").getJSONObject(i).put("valueDefinition", valueObject.get("definition").toString());

                                jsonInputObject.getJSONArray("localIDs").getJSONObject(i).put("value", valueObject.get("resource").toString());

                            }

                        }

                    }

                }

            }

        }

        return jsonInputObject;

    }


    /**
     * This method gets the path of current the work directory
     * @return the path to the current ontology workspace
     */
    public String getPathToOntologies() {
        return this.pathToOntologies;
    }



    /**
     * This method is a getter for the overlay named graph.
     * @return a jena model for a MDB overlay
     */
    public Model getOverlayModel() {

        return this.overlayModel;

    }

    /**
     * This method reads and coordinates the output data for a panel
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject getOutput(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        String resourceURI = getURIForLocalID(jsonInputObject.get("localID").toString(), connectionToTDB);


        OutputGenerator outputGenerator;

        if (!this.mdbCoreID.isEmpty() && !this.mdbEntryID.isEmpty() && !this.mdbUEID.isEmpty()) {

            outputGenerator = new OutputGenerator(this.mdbCoreID, this.mdbEntryID, this.mdbUEID, this.mongoDBConnection);

        } else if(!this.mdbCoreID.isEmpty()) {

            outputGenerator = new OutputGenerator(this.mdbUEID, this.mongoDBConnection);

        } else {

            outputGenerator = new OutputGenerator(this.mongoDBConnection);

        }

        JSONObject jsonOutputObject = outputGenerator.getOutputJSONObjectOld(jsonInputObject, new JSONObject(), resourceURI, connectionToTDB);

        jsonOutputObject.put("load_page", resourceURI);

        try {

            URL url = new URL(resourceURI);

            String loadPageLocalID = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

            jsonOutputObject.put("load_page_localID", loadPageLocalID);

        } catch (MalformedURLException e) {

            e.printStackTrace();

        }

        return jsonOutputObject;

    }

    /**
     * This method reads and coordinates the output data for a panel
     * @param jsonInputObject contains the information for the calculation
     * @param outputObject an already existing output object
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject getOutput(JSONObject jsonInputObject, JSONObject outputObject, JenaIOTDBFactory connectionToTDB) {

        String resourceURI = getURIForLocalID(jsonInputObject.get("localID").toString(), connectionToTDB);

        OutputGenerator outputGenerator;

        if (!this.mdbCoreID.isEmpty() && !this.mdbEntryID.isEmpty() && !this.mdbUEID.isEmpty()) {

            outputGenerator = new OutputGenerator(this.mdbCoreID, this.mdbEntryID, this.mdbUEID, this.mongoDBConnection);

        } else if(!this.mdbCoreID.isEmpty()) {

            outputGenerator = new OutputGenerator(this.mdbUEID, this.mongoDBConnection);

        } else {

            outputGenerator = new OutputGenerator(this.mongoDBConnection);

        }

        return outputGenerator.getOutputJSONObjectOld(jsonInputObject, outputObject, resourceURI, connectionToTDB);

    }


    /**
     * This method reads and coordinates the output data for a panel
     * @param jsonInputObject contains the information for the calculation
     * @param outputObject an already existing output object
     * @param resourceURI contains the root resource for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject getOutput(JSONObject jsonInputObject, JSONObject outputObject, String resourceURI, JenaIOTDBFactory connectionToTDB) {

        OutputGenerator outputGenerator;

        if (!this.mdbCoreID.isEmpty() && !this.mdbEntryID.isEmpty() && !this.mdbUEID.isEmpty()) {

            outputGenerator = new OutputGenerator(this.mdbCoreID, this.mdbEntryID, this.mdbUEID, this.mongoDBConnection);

        } else if(!this.mdbUEID.isEmpty()) {

            outputGenerator = new OutputGenerator(this.mdbUEID, this.mongoDBConnection);

        } else {

            outputGenerator = new OutputGenerator(this.mongoDBConnection);

        }

        outputGenerator.setPathToOntologies(this.pathToOntologies);

        return outputGenerator.getOutputJSONObjectOld(jsonInputObject, outputObject, resourceURI, connectionToTDB);

    }


    /**
     * This method find an URI of a resource with his local identifier
     * @param localID the local identifier of the URI
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an uri of a resource
     */
    public String getURIForLocalID (String localID, JenaIOTDBFactory connectionToTDB) {

        FilterBuilder filterBuilder = new FilterBuilder();

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        SelectBuilder tripleSPO = new SelectBuilder();

        tripleSPO.addWhere("?s", "?p", "?o");

        selectBuilder.addVar(selectBuilder.makeVar("?s"));

        selectBuilder.addGraph("?g", tripleSPO);

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

        filterItems = filterBuilder.addItems(filterItems, "?p", "<http://www.geneontology.org/formats/oboInOwl#id>");

        ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

        selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

        filterItems.clear();

        ArrayList<String> oneDimensionalFilterItems = new ArrayList<>();

        oneDimensionalFilterItems.add(localID);

        filter = sparqlFilter.getRegexSTRFilter("?o", oneDimensionalFilterItems);

        selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

        String sparqlQueryString = selectBuilder.buildString();

        return connectionToTDB.pullSingleDataFromTDB(pathToOntologies, sparqlQueryString, "?s");


    }


    /**
     * This method find the class URI of a resource with the local identifier of an input field
     * @param jsonInputObject contains the information for the calculation
     * @return a class URI of a resource
     */
    public String getClassURIForLocalIDFromMongoDB(JSONObject jsonInputObject) {

        JSONObject jsonFromMongoDB = this.mongoDBConnection.pullDataFromMongoDBWithLocalID(jsonInputObject);

        return jsonFromMongoDB.get("classID").toString();

    }


    /**
     * This method find the individual URI of a resource with the local identifier of an input field
     * @param jsonInputObject contains the information for the calculation
     * @return a individual URI of a resource
     */
    public String getIndividualURIForLocalIDFromMongoDB(JSONObject jsonInputObject) {

        JSONObject jsonFromMongoDB = this.mongoDBConnection.pullDataFromMongoDBWithLocalID(jsonInputObject);

        return jsonFromMongoDB.get("individualID").toString();

    }


    /**
     * This method sets the path of the current work directory
     * @param pathToOntologies contains the path to the ontology workspace
     */
    public void setPathToOntologies(String pathToOntologies) {
        this.pathToOntologies = pathToOntologies;
    }



}
