/*
 * Created by Roman Baum on 24.03.15.
 * Last modified by Roman Baum on 22.01.19.
 */

import soccomas.basic.ApplicationConfigurator;
import soccomas.mongodb.MongoDBConnection;
import soccomas.packages.JSONInputInterpreter;
import soccomas.packages.JenaIOTDBFactory;
import soccomas.packages.SOCCOMASOverlayHandler;
import soccomas.packages.operation.OperationManager;
import soccomas.vocabulary.SCBasic;
import soccomas.vocabulary.SCMDBBasic;
import soccomas.vocabulary.SprO;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.base.block.FileMode;
import org.apache.jena.tdb.sys.SystemTDB;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The class "SOCCOMASWebSocket" provides one default constructor and three methods. The method "connect" create a new session
 * and is the connection between Java and Javascript. The method "close" close the connection between Java and
 * Javascript. The method "onMessage" processed the javascript input and create a new Jena model. This model will be
 * converted to a string and finally sent back to Javascript.
 */


@ServerEndpoint("/soccomas-web-socket")
public class SOCCOMASWebSocket {

    // default - Constructor
    public SOCCOMASWebSocket() {

    }

    private static Set<Object> clients = Collections.synchronizedSet(new HashSet<>());

    @OnOpen
    public void connect(Session session) {
        /**
         *      create a new session and is the connection between Java and Javascript.
         */

        // set timeout to 180000ms
        session.getContainer().setAsyncSendTimeout(180000);

        // add this session to the connected sessions set
        clients.add(session);

        System.out.println("session = " + session);

    }

    @OnClose
    public void close(Session session) {

        /**
         *      close the connection between Java and Javascript.
         */

        // remove this session from the connected sessions set
        clients.remove(session);
    }

    @OnMessage
    public void onMessage(String JSONQuery, Session session) {

        /**
         *      process the javascript input and sent the output string back to Javascript.
         */

        // reduce the size of the TDB
        TDB.getContext().set(SystemTDB.symFileMode, FileMode.direct);

        // create a new JSON object with the input
        JSONObject jsonInputObject = new JSONObject(JSONQuery);

        System.out.println();
        System.out.println("jsonInputObject = " + jsonInputObject);
        System.out.println();

        MongoDBConnection mongoDBConnection = new MongoDBConnection("localhost", 27017);

        // get the SPARQL-Query from the JSON object
        String inputTypeString = jsonInputObject.get("type").toString();

        OperationManager operationManager = new OperationManager(mongoDBConnection);

        // create new connectionToTDB
        JenaIOTDBFactory connectionToTDB = new JenaIOTDBFactory();

        SOCCOMASOverlayHandler soccomasOverlayHandler = new SOCCOMASOverlayHandler(mongoDBConnection);

        switch (inputTypeString) {

            case "query":

                // get the SPARQL-Query from the JSON object
                String sparqlQueryString = jsonInputObject.get("query").toString();

                // get the outputFormat from the JSON object
                String outputFormat = jsonInputObject.get("format").toString();

                // position of the TDB
                String tripleStoreDirectory  = jsonInputObject.get("dataset").toString();

                // calculate the start date
                long executionStart = System.currentTimeMillis();

                // create a Query
                Query sparqlQuery = QueryFactory.create(sparqlQueryString);

                // get result string from the data set
                String resultString = connectionToTDB.pullStringDataFromTDB(tripleStoreDirectory, sparqlQuery,
                                                                    outputFormat);

                // calculate the query time
                long queryTime = System.currentTimeMillis() - executionStart;

                // create a new JSON object
                JSONObject jsonOutputObject = new JSONObject();

                // add a pair (key, value) to the JSON object
                jsonOutputObject.put("output_message", resultString);

                // add a pair (key, value) to the JSON object
                jsonOutputObject.put("query_time", queryTime);

                // convert the JSON object to a string
                String jsonOutputString = jsonOutputObject.toString();

                // send output to javascript
                session.getAsyncRemote().sendText(jsonOutputString);

                break;

            case "push_triples":

                String mdbTestStatusTransitionString = jsonInputObject.get("mdb_status_transition").toString();

                switch (mdbTestStatusTransitionString) {

                    case "test_status_transition":

                        JSONObject inputDataObject = jsonInputObject.getJSONObject("input_data");

                        JSONInputInterpreter jsonInputInterpreter = new JSONInputInterpreter();

                        // calculate the start date
                        executionStart = System.currentTimeMillis();

                        // get an array list with a core id and an output message
                        ArrayList<String> outputArrayList = jsonInputInterpreter.interpretObject(inputDataObject, connectionToTDB);

                        // calculate the query time
                        queryTime = System.currentTimeMillis() - executionStart;

                        System.out.println("query time= " + queryTime);

                        // create a new JSON object
                        jsonOutputObject = new JSONObject();

                        // add a pair (key, value) to the JSON object
                        jsonOutputObject.put("output_message", outputArrayList.get(0));

                        // add a pair (key, value) to the JSON object
                        jsonOutputObject.put("query_time", queryTime);

                        // convert the JSON object to a string
                        jsonOutputString = jsonOutputObject.toString();

                        // send output to javascript
                        session.getAsyncRemote().sendText(jsonOutputString);

                        break;

                }

                break;

            case "check_autocomplete" :

                // calculate the start date
                executionStart = System.currentTimeMillis();

                jsonOutputObject = operationManager.checkAutocomplete(jsonInputObject, connectionToTDB);

                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time= " + queryTime);

                // fill output data
                transferInputKeysToOutput(jsonInputObject,jsonOutputObject);

                // todo remove this if cookies works correct
                initializeMongoDB(jsonOutputObject, mongoDBConnection);

                System.out.println("jsonOutputObject: " + jsonOutputObject);

                // convert the JSON object to a string
                jsonOutputString = jsonOutputObject.toString();

                // send output to javascript
                session.getAsyncRemote().sendText(jsonOutputString);

                break;

            case "check_input" :

                // calculate the start date
                executionStart = System.currentTimeMillis();

                JSONObject originalJSONInputObject = new JSONObject(jsonInputObject.toString());

                jsonOutputObject = operationManager.checkInput(jsonInputObject, connectionToTDB);

                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time= " + queryTime);

                // fill output data
                transferInputKeysToOutput(jsonInputObject, jsonOutputObject);

                // todo remove this if cookies works correct and uncomment the code below instead
                initializeMongoDB(jsonOutputObject, mongoDBConnection);

                /*if (jsonOutputObject.get("html_form").toString().equals("Ontologies/SOCCOMAS/SCBasic#SC_BASIC_0000001206")) {

                    initializeMongoDB(jsonOutputObject, mongoDBConnection);

                }*/

                boolean createNewEntry = false;

                if (jsonOutputObject.has("create_new_entry")) {

                    if (jsonOutputObject.get("create_new_entry").toString().equals("true")) {

                        createNewEntry = true;

                    }

                    jsonOutputObject.remove("create_new_entry");

                }

                boolean subsequentlyWorkflowAction = false;

                String subsequentlyRoot = "";

                JSONObject keywordsToTransferJSON = new JSONObject();

                if (jsonOutputObject.has("subsequently_workflow_action")) {

                    if (jsonOutputObject.get("subsequently_workflow_action").toString().equals("true")) {

                        subsequentlyWorkflowAction = true;

                        if (jsonOutputObject.has("subsequently_root")) {

                            subsequentlyRoot = jsonOutputObject.get("subsequently_root").toString();

                            jsonInputObject.put("subsequently_root", jsonOutputObject.get("subsequently_root").toString());

                            jsonOutputObject.remove("subsequently_root");

                        }

                        if (jsonOutputObject.has("keywords_to_transfer")) {

                            keywordsToTransferJSON = jsonOutputObject.getJSONObject("keywords_to_transfer");

                            jsonInputObject.put("keywords_to_transfer", jsonOutputObject.getJSONObject("keywords_to_transfer"));

                            jsonOutputObject.remove("keywords_to_transfer");

                        }

                    }

                    jsonInputObject.put("subsequently_workflow_action", jsonOutputObject.get("subsequently_workflow_action").toString());

                    jsonOutputObject.remove("subsequently_workflow_action");

                }

                boolean subsequentlyRedirected = false;

                if (jsonOutputObject.has("subsequently_redirected")) {

                    if (jsonOutputObject.get("subsequently_redirected").toString().equals("true")) {

                        subsequentlyRedirected = true;

                        jsonInputObject.put("subsequently_redirected", jsonOutputObject.get("subsequently_redirected").toString());

                        jsonOutputObject.remove("subsequently_redirected");

                        if (jsonOutputObject.has("redirect_to_hyperlink")) {

                            jsonInputObject.put("redirect_to_hyperlink", jsonOutputObject.get("redirect_to_hyperlink").toString());

                            jsonOutputObject.remove("redirect_to_hyperlink");

                        }

                    }

                }

                jsonOutputObject = operationManager.addVersionType(jsonInputObject, jsonOutputObject, connectionToTDB);

                // convert the JSON object to a string
                jsonOutputString = jsonOutputObject.toString();

                // send output to javascript
                session.getAsyncRemote().sendText(jsonOutputString);

                System.out.println("jsonOutputObject: " + jsonOutputObject);

                if (jsonOutputObject.has("load_overlay")) {

                    if (jsonOutputObject.get("load_overlay").toString().contains(ApplicationConfigurator.getDomain() + "/resource/dummy-overlay")) {

                        //System.out.println("overlayModel = " + overlayModel);

                        Model overlayModel = operationManager.getOverlayModel();

                        String overlayNGURI = jsonOutputObject.get("load_overlay").toString();

                        soccomasOverlayHandler.create(overlayNGURI, overlayModel, connectionToTDB);

                    }

                }

                if (createNewEntry) {

                    boolean queueIsProcessed = operationManager.checkOverlayQueueInput(jsonInputObject, connectionToTDB);

                    if (queueIsProcessed) {

                        soccomasOverlayHandler.removeOverlay(jsonInputObject, connectionToTDB);

                    }

                } else if (jsonOutputObject.has("html_form")) {

                    if (jsonOutputObject.get("html_form").toString().contains("resource/dummy-overlay")) {

                        soccomasOverlayHandler.updateTimeStamp(jsonInputObject, connectionToTDB);

                    }

                }

                if (subsequentlyWorkflowAction) {

                    operationManager.checkSubsequentlyWorkflow(originalJSONInputObject, subsequentlyRoot, keywordsToTransferJSON, connectionToTDB);

                }

                if (subsequentlyRedirected) {

                    jsonOutputObject = operationManager.subsequentlyRedirected(jsonInputObject, connectionToTDB);

                    // convert the JSON object to a string
                    jsonOutputString = jsonOutputObject.toString();

                    System.out.println("jsonOutputObject: " + jsonOutputObject);

                    // send output to javascript
                    session.getAsyncRemote().sendText(jsonOutputString);

                }

                break;

            case "check_uri" :

                // calculate the start date
                executionStart = System.currentTimeMillis();

                jsonOutputObject = operationManager.checkURI(jsonInputObject, connectionToTDB);

                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time= " + queryTime);

                transferInputKeysToOutput(jsonInputObject, jsonOutputObject);

                // todo remove this if cookies works correct
                initializeMongoDB(jsonOutputObject, mongoDBConnection);

                jsonOutputObject = operationManager.addVersionType(jsonInputObject, jsonOutputObject, connectionToTDB);

                // convert the JSON object to a string
                jsonOutputString = jsonOutputObject.toString();

                // send output to javascript
                session.getAsyncRemote().sendText(jsonOutputString);


                break;

            case "generate_doc" :

                // calculate the start date
                executionStart = System.currentTimeMillis();

                jsonOutputObject = operationManager.getOutput(jsonInputObject, connectionToTDB);

                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time= " + queryTime);

                // fill output data
                jsonOutputObject.put("connectSID", jsonInputObject.get("connectSID").toString());

                // todo remove this if cookies works correct
                initializeMongoDB(jsonOutputObject, mongoDBConnection);

                // convert the JSON object to a string
                jsonOutputString = jsonOutputObject.toString();

                // send output to javascript
                session.getAsyncRemote().sendText(jsonOutputString);

                System.out.println("jsonOutputObject: " + jsonOutputObject);

                break;

            case "list_entries" :

                // calculate the start date
                executionStart = System.currentTimeMillis();

                jsonOutputObject = operationManager.checkInputForListEntry(jsonInputObject, connectionToTDB);

                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time= " + queryTime);

                transferInputKeysToOutput(jsonInputObject, jsonOutputObject);

                // todo remove this if cookies works correct
                initializeMongoDB(jsonOutputObject, mongoDBConnection);

                jsonOutputObject.put("load_page_localID", "list_entries"); // todo this is a dummy bridge

                // convert the JSON object to a string
                jsonOutputString = jsonOutputObject.toString();

                // send output to javascript
                session.getAsyncRemote().sendText(jsonOutputString);

                System.out.println("jsonOutputObject: " + jsonOutputObject);

                break;

            case "overlay" :

                // calculate the start date
                executionStart = System.currentTimeMillis();

                soccomasOverlayHandler.removeDeprecatedOverlays(jsonInputObject, connectionToTDB);

                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time= " + queryTime);

                break;

        }

        mongoDBConnection.closeConnection();

    }

    private JSONObject transferInputKeysToOutput(JSONObject jsonInputObject,JSONObject jsonOutputObject) {

        // fill output data
        if (jsonInputObject.has("html_form")) {

            jsonOutputObject.put("html_form", jsonInputObject.get("html_form").toString());

        }

        if (jsonInputObject.has("mdbueid")) {

            jsonOutputObject.put("mdbueid", jsonInputObject.get("mdbueid").toString());

        }

        if (jsonInputObject.has("mdbueid_uri")) {

            jsonOutputObject.put("mdbueid_uri", jsonInputObject.get("mdbueid_uri").toString());

        }

        if (jsonInputObject.has(SprO.sproVARIABLEKnownResourceA.toString())) {

            jsonOutputObject.put(SprO.sproVARIABLEKnownResourceA.toString(), jsonInputObject.get(SprO.sproVARIABLEKnownResourceA.toString()).toString());

        }

        if (jsonInputObject.has(SprO.sproVARIABLEKnownResourceB.toString())) {

            jsonOutputObject.put(SprO.sproVARIABLEKnownResourceB.toString(), jsonInputObject.get(SprO.sproVARIABLEKnownResourceB.toString()).toString());

        }

        if (jsonInputObject.has("partID") && !jsonOutputObject.has("partID")) {

            jsonOutputObject.put("partID", jsonInputObject.get("partID").toString());

        }

        jsonOutputObject.put("connectSID", jsonInputObject.get("connectSID").toString());

        return jsonOutputObject;

    }

    private void initializeMongoDB (JSONObject jsonOutputObject, MongoDBConnection mongoDBConnection) {

        String mongoDBKey = "MY_DUMMY_ADMIN_0000000001";

        JSONObject morphologicalDescriptionJSON = new JSONObject();

        morphologicalDescriptionJSON.put("classID", SCBasic.goToAdminPageButtonItem.toString());
        morphologicalDescriptionJSON.put("individualID", SCBasic.basicSOCCOMASCOMPONENTGoToAdminPageButton.toString());
        morphologicalDescriptionJSON.put("localID", SCBasic.basicSOCCOMASCOMPONENTGoToAdminPageButton.getLocalName());

        JSONArray identifiedResources = new JSONArray();

        identifiedResources.put(morphologicalDescriptionJSON);

        generateDummiesForPrototyp(mongoDBKey,  identifiedResources, jsonOutputObject, mongoDBConnection);

        mongoDBKey = "MY_DUMMY_DESCRIPTION_0000000001";

        morphologicalDescriptionJSON = new JSONObject();

        morphologicalDescriptionJSON.put("classID", SCMDBBasic.createMDBMorphologicalDescriptionEntryButtonItem.toString());
        morphologicalDescriptionJSON.put("individualID", SCMDBBasic.basicMDBCOMPONENTCreateMDBMorphologicalDescriptionEntryButton.toString());
        morphologicalDescriptionJSON.put("localID", SCMDBBasic.basicMDBCOMPONENTCreateMDBMorphologicalDescriptionEntryButton.getLocalName());

        identifiedResources = new JSONArray();

        identifiedResources.put(morphologicalDescriptionJSON);

        generateDummiesForPrototyp(mongoDBKey,  identifiedResources, jsonOutputObject, mongoDBConnection);

        mongoDBKey = "MY_DUMMY_LOGOUT_0000000001";

        morphologicalDescriptionJSON = new JSONObject();

        morphologicalDescriptionJSON.put("classID", SCBasic.logOutButtonItem.toString());
        morphologicalDescriptionJSON.put("individualID", SCBasic.basicSOCCOMASCOMPONENTLogOutButton.toString());
        morphologicalDescriptionJSON.put("localID", SCBasic.basicSOCCOMASCOMPONENTLogOutButton.getLocalName());

        identifiedResources = new JSONArray();

        identifiedResources.put(morphologicalDescriptionJSON);

        generateDummiesForPrototyp(mongoDBKey,  identifiedResources, jsonOutputObject, mongoDBConnection);

        mongoDBKey = "MY_DUMMY_MDB_0000000001";

        morphologicalDescriptionJSON = new JSONObject();

        morphologicalDescriptionJSON.put("classID", SCBasic.mySOCCOMASButtonItem.toString());
        morphologicalDescriptionJSON.put("individualID", SCBasic.basicSOCCOMASCOMPONENTMySOCCOMASButton.toString());
        morphologicalDescriptionJSON.put("localID", SCBasic.basicSOCCOMASCOMPONENTMySOCCOMASButton.getLocalName());

        identifiedResources = new JSONArray();

        identifiedResources.put(morphologicalDescriptionJSON);

        generateDummiesForPrototyp(mongoDBKey,  identifiedResources, jsonOutputObject, mongoDBConnection);

        mongoDBKey = "MY_DUMMY_SPECIMEN_0000000001";

        morphologicalDescriptionJSON = new JSONObject();

        morphologicalDescriptionJSON.put("classID", SCMDBBasic.createMDBSpecimenEntryButtonItem.toString());
        morphologicalDescriptionJSON.put("individualID", SCMDBBasic.basicMDBCOMPONENTCreateMDBSpecimenEntryButton.toString());
        morphologicalDescriptionJSON.put("localID", SCMDBBasic.basicMDBCOMPONENTCreateMDBSpecimenEntryButton.getLocalName());

        identifiedResources = new JSONArray();

        identifiedResources.put(morphologicalDescriptionJSON);

        generateDummiesForPrototyp(mongoDBKey,  identifiedResources, jsonOutputObject, mongoDBConnection);

    }

    private void generateDummiesForPrototyp (String mongoDBKey, JSONArray identifiedResources, JSONObject jsonOutputObject, MongoDBConnection mongoDBConnection) {

        if (mongoDBConnection.collectionExist("mdb-prototyp", "sessions")) {

            System.out.println("Collection already exist");

            if (!mongoDBConnection.documentExist("mdb-prototyp", "sessions", "session", jsonOutputObject.get("connectSID").toString())) {

                mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "sessions", "session", jsonOutputObject.get("connectSID").toString());

                mongoDBConnection.createCollection("mdb-prototyp", jsonOutputObject.get("connectSID").toString());

                mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonOutputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

            } else {

                if (mongoDBConnection.documentExistNew("mdb-prototyp", jsonOutputObject.get("connectSID").toString(), mongoDBKey)) {

                    System.out.println("There exist a document for this key!");

                    if (!mongoDBConnection.documentWithDataExist("mdb-prototyp", jsonOutputObject.get("connectSID").toString(), mongoDBKey, identifiedResources)) {

                        mongoDBConnection.putDataToMongoDB("mdb-prototyp", jsonOutputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

                    } else {

                        System.out.println("The document already exist in the collection");
                    }


                } else {

                    mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonOutputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

                }

            }

        } else {

            mongoDBConnection.createCollection("mdb-prototyp", "sessions");

            mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "sessions", "session", jsonOutputObject.get("connectSID").toString());

            mongoDBConnection.createCollection("mdb-prototyp", jsonOutputObject.get("connectSID").toString());

            mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonOutputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

        }

    }

}