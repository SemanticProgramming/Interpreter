/*
 * Created by Roman Baum on 11.12.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages.operation;

import soccomas.basic.ApplicationConfigurator;
import soccomas.basic.SOCCOMASURLEncoder;
import soccomas.basic.TDBPath;
import soccomas.mongodb.MongoDBConnection;
import soccomas.packages.JenaIOTDBFactory;
import soccomas.packages.SOCCOMASExecutionStepHandler;
import soccomas.packages.querybuilder.FilterBuilder;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.packages.querybuilder.SPARQLFilter;
import soccomas.vocabulary.*;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class OutputGenerator {

    private String pathToOntologies = ApplicationConfigurator.getPathToApplicationOntologyStore();

    private String mdbCoreID = "", mdbEntryID = "", mdbUEID = "";

    private MongoDBConnection mongoDBConnection;

    private JSONArray resourcesToCheck = new JSONArray();


    /**
     * Default constructor
     */
    public OutputGenerator(MongoDBConnection mongoDBConnection) {

        this.mongoDBConnection = mongoDBConnection;

    }


    /**
     * A constructor which provide a specific MDBUserEntryID for further calculations
     * @param mdbUEID contains the uri of the MDBUserEntryID
     */
    public OutputGenerator(String mdbUEID, MongoDBConnection mongoDBConnection) {

        this.mdbUEID = mdbUEID;

        this.mongoDBConnection = mongoDBConnection;

    }


    /**
     * A constructor which provide a specific MDBCoreID, MDBEntryID and MDBUserEntryID for further calculations
     * @param mdbCoreID contains the uri of the MDBCoreID
     * @param mdbEntryID contains the uri of the MDBEntryID
     * @param mdbUEID contains the uri of the MDBUserEntryID
     */
    public OutputGenerator(String mdbCoreID, String mdbEntryID, String mdbUEID, MongoDBConnection mongoDBConnection) {

        this.mdbCoreID = mdbCoreID;

        this.mdbEntryID = mdbEntryID;

        this.mdbUEID = mdbUEID;

        this.mongoDBConnection = mongoDBConnection;

    }


    /**
     * This method calculate the root parent URI of an input
     * @param calculateFromURI contains an start URI to find the root parent URI
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return calculates the fhe root parent URI
     */
    public String calculateRootResource(String calculateFromURI, JenaIOTDBFactory connectionToTDB) {

        FilterBuilder filterBuilder = new FilterBuilder();

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        SelectBuilder tripleSPO = new SelectBuilder();

        tripleSPO.addWhere("?s", "?p", "?o");

        selectBuilder.addVar(selectBuilder.makeVar("?o"));

        selectBuilder.addGraph("?g", tripleSPO);

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

        filterItems = filterBuilder.addItems(filterItems, "?s", "<" + calculateFromURI + ">");

        filterItems = filterBuilder.addItems(filterItems, "?p", "spro:SPRO_0000000455");

        ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

        selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

        filterItems.clear();

        String sparqlQueryString = selectBuilder.buildString();

        return connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?o");

    }

    /**
     * This method get the corresponding properties for a subject class resource from the jena tdb and save the
     * corresponding statements in an JSONObject. This method checks transitive annotation properties to find the
     * wanted statements.
     * @param resourceSubject is the URI of a individual or class resource
     * @param givenStatement contains a subject(class or individual), a property and an object for calculation
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject checkAnnotationAnnotationProperties (String resourceSubject, Statement givenStatement, JSONObject entryComponents, JenaIOTDBFactory connectionToTDB) {

        FilterBuilder filterBuilder = new FilterBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        ConstructBuilder constructBuilder = new ConstructBuilder();

        constructBuilder = prefixesBuilder.addPrefixes(constructBuilder);

        constructBuilder.addConstruct("?s", "?p", "?o");

        SelectBuilder tripleSPOConstruct = new SelectBuilder();

        tripleSPOConstruct.addWhere("?s", "?p", "?o");
        tripleSPOConstruct.addWhere("?s", "?p1", "?o1");
        tripleSPOConstruct.addWhere("?s", "?p2", "?o2");

        constructBuilder.addGraph("?g", tripleSPOConstruct);

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

        filterItems = filterBuilder.addItems(filterItems, "?p1", "owl:annotatedSource");

        filterItems = filterBuilder.addItems(filterItems, "?o1", "<" + givenStatement.getSubject().toString() + ">");

        filterItems = filterBuilder.addItems(filterItems, "?p2", "owl:annotatedProperty");

        filterItems = filterBuilder.addItems(filterItems, "?o2", "<" + givenStatement.getPredicate().toString() + ">");

        ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

        constructBuilder = filterBuilder.addFilter(constructBuilder, filter);

        filterItems.clear();

        String currNS = ResourceFactory.createResource(resourceSubject).getNameSpace();

        currNS = currNS.substring(0, currNS.length()-1);

        if (((!this.mdbCoreID.isEmpty()) && (currNS.equals(this.mdbCoreID))) ||
                ((!this.mdbEntryID.isEmpty()) && (currNS.equals(this.mdbEntryID))) ||
                ((!this.mdbUEID.isEmpty()) && (currNS.equals(this.mdbUEID)))) {

            ArrayList<String> filterRegExItems = new ArrayList<>();

            filterRegExItems.add(currNS);

            filter = sparqlFilter.getRegexSTRFilter("?g", filterRegExItems);

            constructBuilder = filterBuilder.addFilter(constructBuilder, filter);

        }

        String sparqlQueryString = constructBuilder.buildString();

        Model constructResult = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

        StmtIterator resultIterator = constructResult.listStatements();

        while (resultIterator.hasNext()) {

            Statement currStatement = resultIterator.next();

            entryComponents = managePropertyOld(resourceSubject, currStatement, entryComponents, connectionToTDB);

        }


        return entryComponents;

    }


    /**
     * This method get the corresponding properties for a subject class resource from the jena tdb and save the
     * corresponding statements in an JSONObject.
     * @param classSubject is the URI of a ontology class
     * @param resourceSubject is the URI of a resource
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject checkClassProperties (String classSubject, String resourceSubject, JSONObject entryComponents, JenaIOTDBFactory connectionToTDB) {

        FilterBuilder filterBuilder = new FilterBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        ConstructBuilder constructBuilder = new ConstructBuilder();

        constructBuilder = prefixesBuilder.addPrefixes(constructBuilder);

        constructBuilder.addConstruct("?s", "?p", "?o");

        SelectBuilder tripleSPO = new SelectBuilder();

        tripleSPO.addWhere("?s", "?p", "?o");

        constructBuilder.addGraph("?g", tripleSPO);

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

        filterItems = filterBuilder.addItems(filterItems, "?s", "<" + classSubject + ">");

        ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

        constructBuilder = filterBuilder.addFilter(constructBuilder, filter);

        filterItems.clear();

        String currNS = ResourceFactory.createResource(resourceSubject).getNameSpace();

        currNS = currNS.substring(0, currNS.length() - 1);

        if (((!this.mdbCoreID.isEmpty()) && (currNS.equals(this.mdbCoreID))) ||
                ((!this.mdbEntryID.isEmpty()) && (currNS.equals(this.mdbEntryID))) ||
                ((!this.mdbUEID.isEmpty()) && (currNS.equals(this.mdbUEID)))) {

            ArrayList<String> filterRegExItems = new ArrayList<>();

            filterRegExItems.add(currNS);

            filter = sparqlFilter.getRegexSTRFilter("?g", filterRegExItems);

            constructBuilder = filterBuilder.addFilter(constructBuilder, filter);

        }


        String sparqlQueryString = constructBuilder.buildString();

        Model constructResult = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

        StmtIterator resultIterator = constructResult.listStatements();

        while (resultIterator.hasNext()) {

            Statement currStatement = resultIterator.next();

            entryComponents = managePropertyOld(resourceSubject, currStatement, entryComponents, connectionToTDB);

        }


        return entryComponents;
    }


    /**
     * This method finds all adjacent neighbours of a resource and save them in a JSONObject
     * @param resourceToCheck contains the URI of an resource which should be checked
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with all components of an entry resource
     */
    public JSONObject checkResource (JSONObject resourceToCheck, JenaIOTDBFactory connectionToTDB) {

        FilterBuilder filterBuilder = new FilterBuilder();

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        SelectBuilder tripleSPO = new SelectBuilder();

        tripleSPO.addWhere("?s", "?p", "?o");

        selectBuilder.addVar(selectBuilder.makeVar("?o"));

        selectBuilder.addGraph("?g", tripleSPO);

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

        filterItems = filterBuilder.addItems(filterItems, "?s", "<" + resourceToCheck.get("uri").toString() + ">");

        filterItems = filterBuilder.addItems(filterItems, "?p", "rdf:type");

        ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

        selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

        filterItems.clear();

        filterItems = filterBuilder.addItems(filterItems, "?o", "owl:NamedIndividual");

        filter = sparqlFilter.getNotINFilter(filterItems);

        selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

        // todo add graph filter use ids and current resource for namespace

        String currNS = ResourceFactory.createResource(resourceToCheck.get("uri").toString()).getNameSpace();

        currNS = currNS.substring(0, currNS.length()-1);

        if (((!this.mdbCoreID.isEmpty()) && (currNS.equals(this.mdbCoreID))) ||
                ((!this.mdbEntryID.isEmpty()) && (currNS.equals(this.mdbEntryID))) ||
                ((!this.mdbUEID.isEmpty()) && (currNS.equals(this.mdbUEID)))) {

            ArrayList<String> filterRegExItems = new ArrayList<>();

            filterRegExItems.add(currNS);

            filter = sparqlFilter.getRegexSTRFilter("?g", filterRegExItems);

            selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

        }

        String sparqlQueryString = selectBuilder.buildString();

        String resourceSubject = resourceToCheck.get("uri").toString();

        String classSubject = connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?o");

        TDBPath tdbPath = new TDBPath();

        if (classSubject.isEmpty() && this.pathToOntologies.equals(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYCoreWorkspaceDirectory.toString()))) {

            this.pathToOntologies = tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString());

            classSubject = connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?o");

        } else if (classSubject.isEmpty() && this.pathToOntologies.equals(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString()))) {

            this.pathToOntologies = tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYCoreWorkspaceDirectory.toString());

            classSubject = connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?o");

        }

        JSONObject entryComponents = new JSONObject();

        entryComponents = checkResourceProperties(resourceSubject, entryComponents, connectionToTDB);

        entryComponents = checkClassProperties(classSubject, resourceSubject, entryComponents, connectionToTDB);

        entryComponents = reorderEntryComponentsValuesOld(entryComponents);

        //System.out.println("entryComponents: " + entryComponents);

        return entryComponents;

    }


    /**
     * This method get the corresponding properties for a subject class resource from the jena tdb and save the
     * corresponding statements in an JSONObject.
     * @param resourceSubject is the URI of a individual or class resource
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject checkResourceProperties (String resourceSubject, JSONObject entryComponents, JenaIOTDBFactory connectionToTDB) {


        FilterBuilder filterBuilder = new FilterBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        ConstructBuilder constructBuilder = new ConstructBuilder();

        constructBuilder = prefixesBuilder.addPrefixes(constructBuilder);

        constructBuilder.addConstruct("?s", "?p", "?o");

        SelectBuilder tripleSPO = new SelectBuilder();

        tripleSPO.addWhere("?s", "?p", "?o");

        constructBuilder.addGraph("?g", tripleSPO);

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

        filterItems = filterBuilder.addItems(filterItems, "?s", "<" + resourceSubject + ">");

        ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

        constructBuilder = filterBuilder.addFilter(constructBuilder, filter);

        filterItems.clear();

        String currNS = ResourceFactory.createResource(resourceSubject).getNameSpace();

        currNS = currNS.substring(0, currNS.length() - 1);

        if (((!this.mdbCoreID.isEmpty()) && (currNS.equals(this.mdbCoreID))) ||
                ((!this.mdbEntryID.isEmpty()) && (currNS.equals(this.mdbEntryID))) ||
                ((!this.mdbUEID.isEmpty()) && (currNS.equals(this.mdbUEID)))) {

            ArrayList<String> filterRegExItems = new ArrayList<>();

            filterRegExItems.add(currNS);

            filter = sparqlFilter.getRegexSTRFilter("?g", filterRegExItems);

            constructBuilder = filterBuilder.addFilter(constructBuilder, filter);

        }

        String sparqlQueryString = constructBuilder.buildString();

        Model constructResult = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

        StmtIterator resultIterator = constructResult.listStatements();

        while (resultIterator.hasNext()) {

            Statement currStatement = resultIterator.next();

            entryComponents = managePropertyOld(resourceSubject, currStatement, entryComponents, connectionToTDB);

        }

        return entryComponents;

    }


    /**
     * This method creates an JSONArray with the local identifier of the identified Resources and removes the URIs od the
     * hidden elements from the output
     * @param JSONToCheckForResources contains a JSONArray with potential identified Resources
     * @return an JSONArray with identified Resources as values
     */
    private JSONArray getIdentifiedResources(JSONArray JSONToCheckForResources, boolean deleteSomeKeys, JSONArray identifiedResources) {

        int startLength = JSONToCheckForResources.length();

        for (int i = (startLength - 1); i >= 0; i--) {

            if (JSONToCheckForResources.get(i) instanceof JSONObject) {

                if (JSONToCheckForResources.getJSONObject(i).has(SprO.hasEntryComponent.getLocalName())
                        || JSONToCheckForResources.getJSONObject(i).has(SCMDBMD.hasEditableLabelInNamedGraph.getLocalName())
                        || JSONToCheckForResources.getJSONObject(i).has("BFO_0000051")) {
                        // has part

                    if (JSONToCheckForResources.getJSONObject(i).has(SprO.hasEntryComponent.getLocalName())) {

                        identifiedResources = getIdentifiedResources(JSONToCheckForResources.getJSONObject(i).getJSONArray(SprO.hasEntryComponent.getLocalName()), true, identifiedResources);

                    }

                    if(JSONToCheckForResources.getJSONObject(i).has(SCMDBMD.hasEditableLabelInNamedGraph.getLocalName())) {

                        identifiedResources = getIdentifiedResources(JSONToCheckForResources.getJSONObject(i).getJSONArray(SCMDBMD.hasEditableLabelInNamedGraph.getLocalName()), true, identifiedResources);

                    }

                    if(JSONToCheckForResources.getJSONObject(i).has("BFO_0000051")) {
                        // has part

                        identifiedResources = getIdentifiedResources(JSONToCheckForResources.getJSONObject(i).getJSONArray("BFO_0000051"), false, identifiedResources);

                    }

                } else {

                    JSONObject objectToInsert = new JSONObject();

                    if (JSONToCheckForResources.getJSONObject(i).has(SprO.inputValueResourceDefinesSPrOVariableResource.getLocalName())) {

                        objectToInsert.put("keyword", JSONToCheckForResources.getJSONObject(i).get(SprO.inputValueResourceDefinesSPrOVariableResource.getLocalName()).toString());

                    }

                    if (JSONToCheckForResources.getJSONObject(i).has(SprO.inputLabelValueDefinesSPrOVariableResource.getLocalName())) {

                        objectToInsert.put("keywordLabel", JSONToCheckForResources.getJSONObject(i).get(SprO.inputLabelValueDefinesSPrOVariableResource.getLocalName()).toString());

                    }

                    if (JSONToCheckForResources.getJSONObject(i).has(SprO.inputDefinitionValueDefinesSPrOVariableResource.getLocalName())) {

                        objectToInsert.put("keywordDefinition", JSONToCheckForResources.getJSONObject(i).get(SprO.inputDefinitionValueDefinesSPrOVariableResource.getLocalName()).toString());

                    }

                    if (JSONToCheckForResources.getJSONObject(i).has("classID")) {

                        objectToInsert.put("classID", JSONToCheckForResources.getJSONObject(i).get("classID").toString());

                    }

                    if (JSONToCheckForResources.getJSONObject(i).has("individualID")) {

                        objectToInsert.put("individualID", JSONToCheckForResources.getJSONObject(i).get("individualID").toString());

                    }

                    if (JSONToCheckForResources.getJSONObject(i).has("localID")) {

                        objectToInsert.put("localID", JSONToCheckForResources.getJSONObject(i).get("localID").toString());

                    }

                    identifiedResources.put(objectToInsert);

                    }

                    if(JSONToCheckForResources.getJSONObject(i).has(SCMDBMD.hasLabelInputEntryComponent.getLocalName())
                            && !JSONToCheckForResources.getJSONObject(i).has(SCMDBMD.hasEditableLabelInNamedGraph.getLocalName())) {

                        JSONToCheckForResources.getJSONObject(i).remove(SCMDBMD.hasLabelInputEntryComponent.getLocalName());

                    }

                    if (deleteSomeKeys) {

                        // remove internal information from output
                        JSONToCheckForResources.getJSONObject(i).remove(SprO.inputValueResourceDefinesSPrOVariableResource.getLocalName());
                        JSONToCheckForResources.getJSONObject(i).remove(SprO.inputLabelValueDefinesSPrOVariableResource.getLocalName());
                        JSONToCheckForResources.getJSONObject(i).remove(SprO.inputDefinitionValueDefinesSPrOVariableResource.getLocalName());
                        JSONToCheckForResources.getJSONObject(i).remove("classID");
                        JSONToCheckForResources.getJSONObject(i).remove("individualID");

                    }

            } else {
                // delete URIs of hidden components

                JSONToCheckForResources.remove(i);

            }

        }

        return identifiedResources;

    }


    /**
     * This method gets the path of current the work directory
     * @return the path to the current ontology workspace
     */
    public String getPathToOntologies() {
        return this.pathToOntologies;
    }


    /**
     * This method reads and coordinates the output data for a panel
     * @param jsonInputObject contains the information for the calculation
     * @param uri contains the root resource for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an output JSONObject with data
     */
    public JSONObject getOutputJSONObjectOld(JSONObject jsonInputObject, JSONObject outputObject, String uri, JenaIOTDBFactory connectionToTDB) {

        JSONArray outputDataJSON = new JSONArray();

        JSONObject currResourceToCheck = new JSONObject();

        String parentURI = calculateRootResource(uri, connectionToTDB);

        if (parentURI.equals("")) {
            // input uri did not has "has MDB entry composition"

            parentURI = uri;

        }

        currResourceToCheck.put("uri", parentURI);

        this.resourcesToCheck.put(currResourceToCheck);

        while (!this.resourcesToCheck.isNull(0)) {

            // save calculated data in an JSON array
            outputDataJSON.put(checkResource(this.resourcesToCheck.getJSONObject(0), connectionToTDB));

            // remove the old key
            this.resourcesToCheck.remove(0);

        }

        outputDataJSON = orderOutputJSONOld(outputDataJSON);

        outputDataJSON.put(0, outputDataJSON.getJSONObject(0).getJSONObject(parentURI));

        System.out.println("before identifiedResources");

        JSONArray identifiedResources = getIdentifiedResources(outputDataJSON, true, new JSONArray());

        System.out.println("identifiedResources: " + identifiedResources);

        String mongoDBKey = "";

        UrlValidator keyURLValidator = new UrlValidator();

        // get a MDB url Encoder to encode the uri with utf-8
        SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

        if (keyURLValidator.isValid(soccomasURLEncoder.encodeUrl(uri, "UTF-8"))) {

            try {

                URL url = new URL(uri);

                mongoDBKey = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

            } catch (MalformedURLException e) {

                System.out.println("INFO: the variable 'mongoDBKey' contains no valid URL.");

            }

        } else {

            mongoDBKey = uri;

        }

        if (!identifiedResources.isNull(0)) {

            if (this.mongoDBConnection.collectionExist("mdb-prototyp", "sessions")) {

                System.out.println("Collection already exist");

                if (!this.mongoDBConnection.documentExist("mdb-prototyp", "sessions", "session", jsonInputObject.get("connectSID").toString())) {

                    this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "sessions", "session", jsonInputObject.get("connectSID").toString());

                    this.mongoDBConnection.createCollection("mdb-prototyp", jsonInputObject.get("connectSID").toString());

                    this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

                } else {

                    if (this.mongoDBConnection.documentExistNew("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey)) {

                        System.out.println("There exist a document for this key!");

                        if (!this.mongoDBConnection.documentWithDataExist("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources)) {

                            this.mongoDBConnection.putDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

                        } else {

                            System.out.println("The document already exist in the collection");
                        }

                    } else {

                        this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

                    }

                }

            } else {

                this.mongoDBConnection.createCollection("mdb-prototyp", "sessions");

                this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "sessions", "session", jsonInputObject.get("connectSID").toString());

                this.mongoDBConnection.createCollection("mdb-prototyp", jsonInputObject.get("connectSID").toString());

                this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

            }

            System.out.println("Connect SID: " + jsonInputObject.get("connectSID").toString());

        }

        outputObject.put("data", outputDataJSON);

        return outputObject;

    }


    /**
     * This method reads and coordinates the output data for a panel
     * @param root contains an URI
     * @param jsonInputObject contains the information for the calculation
     * @param outputDataJSON contains the output information
     */
    public void getOutputJSONObject(String root, JSONObject jsonInputObject, JSONArray outputDataJSON) {

        JSONArray identifiedResources = getIdentifiedResources(outputDataJSON, true, new JSONArray());

        //System.out.println("identifiedResources: " + identifiedResources);

        String mongoDBKey = root;

        try {

            URL url = new URL(mongoDBKey);

            mongoDBKey = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

        } catch (MalformedURLException e) {

            System.out.println("INFO: the variable 'mongoDBKey' contains no valid URL.");

        }

        if (!identifiedResources.isNull(0)) {

            if (this.mongoDBConnection.collectionExist("mdb-prototyp", "sessions")) {

                System.out.println("Collection already exist");

                if (!this.mongoDBConnection.documentExist("mdb-prototyp", "sessions", "session", jsonInputObject.get("connectSID").toString())) {

                    this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "sessions", "session", jsonInputObject.get("connectSID").toString());

                    this.mongoDBConnection.createCollection("mdb-prototyp", jsonInputObject.get("connectSID").toString());

                    this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

                } else {

                    if (this.mongoDBConnection.documentExistNew("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey)) {

                        System.out.println("There exist a document for this key!");

                        if (!this.mongoDBConnection.documentWithDataExist("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources)) {

                            this.mongoDBConnection.putDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

                        } else {

                            System.out.println("The document already exist in the collection");
                        }

                    } else {

                        this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

                    }

                }

            } else {

                this.mongoDBConnection.createCollection("mdb-prototyp", "sessions");

                this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "sessions", "session", jsonInputObject.get("connectSID").toString());

                this.mongoDBConnection.createCollection("mdb-prototyp", jsonInputObject.get("connectSID").toString());

                this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey, identifiedResources);

            }

            System.out.println("Connect SID: " + jsonInputObject.get("connectSID").toString());

        }

    }


    /**
     * This method fills the JSONObject with data of an entry component corresponding to a specific property.
     * @param resourceSubject is the URI of a individual resource
     * @param currStatement contains a subject(class or individual), a property and an object for calculation
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject managePropertyOld(String resourceSubject, Statement currStatement, JSONObject entryComponents, JenaIOTDBFactory connectionToTDB) {

        String propertyToCheck = currStatement.getPredicate().toString();

        JSONObject currComponentObject = new JSONObject();

        if (propertyToCheck.equals(SprO.hasEntryComponent.toString())) {

            JSONObject newResourceToCheck = new JSONObject();

            newResourceToCheck.put("uri", currStatement.getObject().asResource().toString());

            this.resourcesToCheck.put(newResourceToCheck);

            currComponentObject.append(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString());

            for (int i = 0; i < entryComponents.length(); i++) {

                if (entryComponents.getJSONArray(currStatement.getSubject().toString()).getJSONObject(i).has(currStatement.getPredicate().getLocalName())) {

                    entryComponents.getJSONArray(currStatement.getSubject().toString()).getJSONObject(i).accumulate(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString());

                    return entryComponents;

                }

            }

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.entryComponentOf.toString())
                || propertyToCheck.equals(SprO.hasAdditionalCSSClass.toString())
                || propertyToCheck.equals(SprO.hasGUIInputType.toString())
                || propertyToCheck.equals(SprO.inputValueResourceDefinesSPrOVariableResource.toString())
                || propertyToCheck.equals(SprO.inputLabelValueDefinesSPrOVariableResource.toString())
                || propertyToCheck.equals(SprO.inputDefinitionValueDefinesSPrOVariableResource.toString())) {

            if (currStatement.getObject().isLiteral()) {
                // todo remove this then case when the switch to page object is no longer missing

            } else {

                currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

                currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().getLocalName());

                return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);
            }

        } else if (propertyToCheck.equals(SprO.hasGUIRepresentation.toString())) {

            entryComponents = checkAnnotationAnnotationProperties(resourceSubject, currStatement, entryComponents, connectionToTDB );

            //change the subject of the current statement
            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().getLocalName());

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.hasPositionInEntryComponent.toString())
                || propertyToCheck.equals(SprO.requiredInputBOOLEAN.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            String currObject = currStatement.getObject().asLiteral().getValue().toString();

            if (currObject.contains("^^")) {

                currObject = currObject.substring(0, currObject.indexOf("^^"));

            }

            currComponentObject.put(currStatement.getPredicate().getLocalName(), currObject);

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SCBasic.hasUserGUIInputURI.toString())
                || propertyToCheck.equals(SprO.hasSelectedResource.toString())
                || propertyToCheck.equals(SprO.autocompleteForOntology.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString());

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SCBasic.hasUserGUIInputValueA.toString())
                || propertyToCheck.equals(SCBasic.hasUserGUIInputValueB.toString())
                || propertyToCheck.equals(SCBasic.hasUserGUIInputValueC.toString())
                || propertyToCheck.equals(SprO.hasVisibleLabel1.toString())
                || propertyToCheck.equals(SprO.hasVisibleLabel2.toString())
                || propertyToCheck.equals(SprO.newRowBOOLEAN.toString())
                || propertyToCheck.equals(SprO.tooltipText.toString())
                || propertyToCheck.equals(SprO.hiddenBOOLEAN.toString())
                || propertyToCheck.equals(SprO.withInformationText.toString())
                || propertyToCheck.equals(SprO.signUpComment.toString())
                || propertyToCheck.equals(SprO.isNewRowBOOLEAN.toString())
                || propertyToCheck.equals(SprO.hasDefaultPlaceholderValue.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            if (currStatement.getObject().isResource()) {

                if (currStatement.getObject().toString().equals(SprO.sproVARIABLEThisCoreID.toString())) {

                    currComponentObject.put(currStatement.getPredicate().getLocalName(), this.mdbCoreID);

                } else {

                    currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().toString());

                }

            } else {

                currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asLiteral().getLexicalForm());

            }

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SCBasic.hasUserGUIInputInputA.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            if (currStatement.getObject().asResource().toString().startsWith("mailto:")) {
                // special case mail

                currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString().substring(7));

            } else {

                currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().getLocalName());

            }

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.inputIsRestrictedToSubclassesOf.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            JSONArray selectClassData = new JSONArray();

            // create query to find value in specific composition
            String resultVarClass = "?s";

            PrefixesBuilder prefixesBuilderClass = new PrefixesBuilder();

            SelectBuilder selectBuilderClass = new SelectBuilder();

            selectBuilderClass = prefixesBuilderClass.addPrefixes(selectBuilderClass);

            SelectBuilder tripleSPOConstructClass = new SelectBuilder();

            tripleSPOConstructClass.addWhere(resultVarClass, RDFS.subClassOf, "<" + currStatement.getObject() + ">");

            selectBuilderClass.addVar(selectBuilderClass.makeVar(resultVarClass));

            selectBuilderClass.addGraph("?g", tripleSPOConstructClass);

            String sparqlQueryStringClass = selectBuilderClass.buildString();

            JSONArray classJA = connectionToTDB.pullMultipleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringClass, resultVarClass);

            for (int i = 0; i < classJA.length(); i++) {

                String resultVarLabel = "?s";

                PrefixesBuilder prefixesBuilderLabel = new PrefixesBuilder();

                SelectBuilder selectBuilderLabel = new SelectBuilder();

                selectBuilderLabel = prefixesBuilderLabel.addPrefixes(selectBuilderLabel);

                SelectBuilder tripleSPOConstructLabel = new SelectBuilder();

                tripleSPOConstructLabel.addWhere( "<" + classJA.get(i) + ">", RDFS.label, resultVarLabel);

                selectBuilderLabel.addVar(selectBuilderLabel.makeVar(resultVarLabel));

                selectBuilderLabel.addGraph("?g", tripleSPOConstructLabel);

                String sparqlQueryStringLabel = selectBuilderLabel.buildString();

                String label = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringLabel, resultVarLabel);

                // find tooltip text

                String resultVarTooltip = "?s";

                PrefixesBuilder prefixesBuilderTooltip = new PrefixesBuilder();

                SelectBuilder selectBuilderTooltip = new SelectBuilder();

                selectBuilderTooltip = prefixesBuilderTooltip.addPrefixes(selectBuilderTooltip);

                SelectBuilder tripleSPOConstructTooltip = new SelectBuilder();

                tripleSPOConstructTooltip.addWhere("<" + classJA.get(i) + ">", SprO.tooltipText, resultVarTooltip);

                selectBuilderTooltip.addVar(selectBuilderTooltip.makeVar(resultVarTooltip));

                selectBuilderTooltip.addGraph("?g", tripleSPOConstructTooltip);

                String sparqlQueryStringTooltip = selectBuilderTooltip.buildString();

                String tooltipText = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringTooltip, resultVarTooltip);

                // save date in output stream

                JSONObject currSelectIndividual = new JSONObject();

                currSelectIndividual.put("selValue", classJA.get(i));
                currSelectIndividual.put("selLabel", label);

                if (!tooltipText.isEmpty()) {

                    currSelectIndividual.put(SprO.tooltipText.getLocalName(), tooltipText);

                }

                selectClassData.put(currSelectIndividual);

            }

            currComponentObject.put(currStatement.getPredicate().getLocalName(), selectClassData);

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.inputIsRestrictedToIndividualsOf.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            JSONArray selectIndividualData = new JSONArray();

            // create query to find value in specific composition
            String resultVarValues = "?s";

            PrefixesBuilder prefixesBuilderValues = new PrefixesBuilder();

            SelectBuilder selectBuilderValues = new SelectBuilder();

            selectBuilderValues = prefixesBuilderValues.addPrefixes(selectBuilderValues);

            SelectBuilder tripleSPOConstructValues = new SelectBuilder();

            tripleSPOConstructValues.addWhere(resultVarValues , RDF.type, "<" + currStatement.getObject() + ">");

            selectBuilderValues.addVar(selectBuilderValues.makeVar(resultVarValues));

            selectBuilderValues.addGraph("?g", tripleSPOConstructValues);

            String sparqlQueryStringValues = selectBuilderValues.buildString();

            JSONArray valuesJA = connectionToTDB.pullMultipleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringValues, resultVarValues);

            for (int i = 0; i < valuesJA.length(); i++) {

                String resultVarLabel = "?s";

                PrefixesBuilder prefixesBuilderLabel = new PrefixesBuilder();

                SelectBuilder selectBuilderLabel = new SelectBuilder();

                selectBuilderLabel = prefixesBuilderLabel.addPrefixes(selectBuilderLabel);

                SelectBuilder tripleSPOConstructLabel = new SelectBuilder();

                tripleSPOConstructLabel.addWhere( "<" + valuesJA.get(i) + ">", RDFS.label, resultVarLabel);

                selectBuilderLabel.addVar(selectBuilderLabel.makeVar(resultVarLabel));

                selectBuilderLabel.addGraph("?g", tripleSPOConstructLabel);

                String sparqlQueryStringLabel = selectBuilderLabel.buildString();

                String label = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringLabel, resultVarLabel);

                // find tooltip text

                String resultVarTooltip = "?s";

                PrefixesBuilder prefixesBuilderTooltip = new PrefixesBuilder();

                SelectBuilder selectBuilderTooltip = new SelectBuilder();

                selectBuilderTooltip = prefixesBuilderTooltip.addPrefixes(selectBuilderTooltip);

                SelectBuilder tripleSPOConstructTooltip = new SelectBuilder();

                tripleSPOConstructTooltip.addWhere("<" + valuesJA.get(i) + ">", SprO.tooltipText, resultVarTooltip);
                // tooltip text

                selectBuilderTooltip.addVar(selectBuilderTooltip.makeVar(resultVarTooltip));

                selectBuilderTooltip.addGraph("?g", tripleSPOConstructTooltip);

                String sparqlQueryStringTooltip = selectBuilderTooltip.buildString();

                String tooltipText = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringTooltip, resultVarTooltip);

                // save date in output stream

                JSONObject currSelectIndividual = new JSONObject();

                currSelectIndividual.put("selValue", valuesJA.get(i));
                currSelectIndividual.put("selLabel", label);

                if (!tooltipText.isEmpty()) {

                    currSelectIndividual.put(SprO.tooltipText.getLocalName(), tooltipText);

                }

                selectIndividualData.put(currSelectIndividual);

            }

            currComponentObject.put(currStatement.getPredicate().getLocalName(), selectIndividualData);

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);


        } else if (propertyToCheck.equals(SprO.componentStatusBOOLEAN.toString())) {

            String property;

            if (currStatement.getObject().asLiteral().getLexicalForm().equals("true")) {

                property = SprO.labelStatusTrue.toString();
                // label status 'true'


            } else {

                property = SprO.labelStatusFalse.toString();
                // label status 'false'

            }

            // create query to find value in specific composition
            String resultVar = "?o";

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            SelectBuilder selectBuilder = new SelectBuilder();

            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

            SelectBuilder tripleSPOConstruct = new SelectBuilder();

            tripleSPOConstruct.addWhere("?bNode", OWL2.annotatedSource, "<" + resourceSubject + ">");
            tripleSPOConstruct.addWhere("?bNode", "<" + property + ">", "?o");

            selectBuilder.addVar(selectBuilder.makeVar(resultVar));

            selectBuilder.addGraph("?g", tripleSPOConstruct);

            String sparqlQueryString = selectBuilder.buildString();

            String value = connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, resultVar);

            currComponentObject.put(ResourceFactory.createProperty(property).getLocalName(), value);

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.executionStepTrigger.toString())
                || propertyToCheck.equals(SprO.executionStepHyperlink.toString())) {

            return checkAnnotationAnnotationProperties(resourceSubject, currStatement, entryComponents, connectionToTDB);

        } else if (propertyToCheck.equals(RDF.type.toString())) {

            if (!currStatement.getObject().toString().equals(OWL2.NamedIndividual.toString())
                    && !currStatement.getObject().toString().equals(OWL2.Axiom.toString())
                    && !currStatement.getObject().toString().equals(OWL2.Class.toString())) {

                // save the class URI of the individual
                currComponentObject.put("classID", currStatement.getObject().asResource().toString());

                // save the individual URI of the individual
                currComponentObject.put("individualID", currStatement.getSubject().asResource().toString());

                // save the local identifier of the individual
                currComponentObject.put("localID", currStatement.getSubject().asResource().getLocalName());

                return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

            }

        } else {

            // differ potential interesting properties and uninteresting properties
            if (unknownProperty(currStatement.getPredicate().toString())) {

                //System.out.println("potential Statement to process: " + currStatement);

            }

        }

        return entryComponents;

    }


    /**
     * This method fills the JSONObject with data of an entry component corresponding to a specific property.
     * @param resourceSubject is the URI of a individual resource
     * @param currStatement contains a subject(class or individual), a property and an object for calculation
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject manageProperty (String resourceSubject, Statement currStatement, JSONObject entryComponents,
                                      JSONObject jsonInputObject , JenaIOTDBFactory connectionToTDB) {

        //System.out.println("currStatement = " + currStatement);

        String propertyToCheck = currStatement.getPredicate().toString();

        JSONObject currComponentObject = new JSONObject();

        if (propertyToCheck.equals(SprO.hasEntryComponent.toString())
                || propertyToCheck.equals("http://purl.obolibrary.org/obo/BFO_0000051")) {

            currComponentObject.append(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString());

            for (int i = 0; i < entryComponents.length(); i++) {

                if (entryComponents.has(currStatement.getSubject().toString())) {

                    JSONArray innerJSONArray = entryComponents.getJSONArray(currStatement.getSubject().toString());

                    for (int j = 0; j < innerJSONArray.length(); j++) {

                        if (innerJSONArray.getJSONObject(j).has(currStatement.getPredicate().getLocalName())) {

                            entryComponents.getJSONArray(currStatement.getSubject().toString()).getJSONObject(i).accumulate(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString());

                            return entryComponents;

                        }

                    }

                }

            }

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.entryComponentOf.toString())
                || propertyToCheck.equals(SprO.hasAdditionalCSSClass.toString())
                || propertyToCheck.equals(SprO.belongsToRadioButtonGroup.toString())
                || propertyToCheck.equals(SprO.hasGUIInputType.toString())
                || propertyToCheck.equals(SprO.hasGUIRepresentation.toString())
                || propertyToCheck.equals(SprO.inputValueResourceDefinesSPrOVariableResource.toString())
                || propertyToCheck.equals(SprO.inputLabelValueDefinesSPrOVariableResource.toString())
                || propertyToCheck.equals(SprO.inputDefinitionValueDefinesSPrOVariableResource.toString())
                || propertyToCheck.equals(SCMDBMD.belongsToPartonomyView.toString())
                || propertyToCheck.equals(SCMDBMD.hasLabelInputEntryComponent.toString())) {

            if (!currStatement.getObject().isLiteral()) {
                // todo remove this then case when the switch to page object is no longer missing

                currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

                currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().getLocalName());

                return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);
            }

        } else if (propertyToCheck.equals(SprO.hasPositionInEntryComponent.toString())
                || propertyToCheck.equals(SprO.requiredInputBOOLEAN.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            String currObject = currStatement.getObject().asLiteral().getValue().toString();

            if (currObject.contains("^^")) {

                currObject = currObject.substring(0, currObject.indexOf("^^"));

            }

            currComponentObject.put(currStatement.getPredicate().getLocalName(), currObject);

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SCBasic.hasUserGUIInputURI.toString())
                || propertyToCheck.equals(SprO.hasSelectedResource.toString())
                || propertyToCheck.equals(SprO.hyperlink.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString());

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SCBasic.hasUserGUIInputValueA.toString())
                || propertyToCheck.equals(SCBasic.hasUserGUIInputValueB.toString())
                || propertyToCheck.equals(SCBasic.hasUserGUIInputValueC.toString())
                || propertyToCheck.equals(SprO.hasVisibleLabel1.toString())
                || propertyToCheck.equals(SprO.hasVisibleLabel2.toString())
                || propertyToCheck.equals(SprO.newRowBOOLEAN.toString())
                || propertyToCheck.equals(SprO.tooltipText.toString())
                || propertyToCheck.equals(SprO.label2.toString())
                || propertyToCheck.equals(SprO.hiddenBOOLEAN.toString())
                || propertyToCheck.equals(SprO.hasBooleanValueBOOLEAN.toString())
                || propertyToCheck.equals(SprO.withInformationText.toString())
                || propertyToCheck.equals(SprO.signUpComment.toString())
                || propertyToCheck.equals(SprO.isNewRowBOOLEAN.toString())
                || propertyToCheck.equals(SprO.showExpandedThisEntrySSpecificIndividualOf.toString())
                || propertyToCheck.equals(SprO.hasDefaultPlaceholderValue.toString())
                || propertyToCheck.equals(SprO.hasPartonomyLabel.toString())
                || propertyToCheck.equals(SprO.isActive.toString())
                || propertyToCheck.equals(SprO.visibleBOOLEAN.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            if (currStatement.getObject().isResource()) {

                if (currStatement.getObject().toString().equals(SprO.sproVARIABLEThisCoreID.toString())) {
                    // KEYWORD: this MDB core ID

                    currComponentObject.put(currStatement.getPredicate().getLocalName(), this.mdbCoreID);

                } else {

                    currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().toString());

                }

            } else {

                currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asLiteral().getLexicalForm());

            }

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SCBasic.hasUserGUIInputInputA.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            if (currStatement.getObject().asResource().toString().startsWith("mailto:")) {
                // special case mail

                currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString().substring(7));

            } else {

                currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().getLocalName());

            }

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SCMDBMD.hasEditableLabelInNamedGraph.toString())) {

            String mdRootElementLabelNG = currStatement.getObject().toString();

            // create query to find value in specific composition
            String rootResultVar = "?o";

            PrefixesBuilder prefixesBuilderRoot = new PrefixesBuilder();

            SelectBuilder selectBuilderRoot = new SelectBuilder();

            selectBuilderRoot = prefixesBuilderRoot.addPrefixes(selectBuilderRoot);

            SelectBuilder tripleSPOConstructRoot = new SelectBuilder();

            tripleSPOConstructRoot.addWhere("<" + mdRootElementLabelNG + ">", SCBasic.compositionInThisNamedGraphHasRootEntryComponent, rootResultVar);

            selectBuilderRoot.addVar(selectBuilderRoot.makeVar(rootResultVar));

            selectBuilderRoot.addGraph("<" + mdRootElementLabelNG + ">", tripleSPOConstructRoot);

            String sparqlQueryStringRoot = selectBuilderRoot.buildString();

            TDBPath tdbPath = new TDBPath();

            String directory;

            if (mdRootElementLabelNG.contains("-p_")) {

                directory = tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYPublishedWorkspaceDirectory.toString());

            } else {

                directory = tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString());

            }

            String root = connectionToTDB.pullSingleDataFromTDB(directory, sparqlQueryStringRoot, rootResultVar);

            if (!root.isEmpty()) {

                boolean calculateSubGraph = false;

                if (jsonInputObject.has("value")) {

                    if (jsonInputObject.get("value").toString().equals("show_localID")) {

                        if (currStatement.getSubject().toString().contains((jsonInputObject.get("localID").toString()))) {

                            calculateSubGraph = true;

                        }

                    } else if (jsonInputObject.has("localID")) {
                        // special case if the user click the 'go to description button' from an entry

                        if (jsonInputObject.get("localID").toString().contains(SCMDBMD.goToDescriptionButtonItem.getLocalName())
                                // go to description button item
                                && currStatement.getSubject().toString().contains(SCMDBMD.referentOfMorphologicalDescription.getLocalName())) {

                            calculateSubGraph = true;

                        }

                    }

                }

                if (calculateSubGraph) {

                    SOCCOMASExecutionStepHandler soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.mongoDBConnection);

                    JSONArray ngs = new JSONArray();

                    ngs.put(mdRootElementLabelNG);

                    JSONArray subGraph = soccomasExecutionStepHandler.getCompositionFromStoreForOutput(root, ngs,  directory, jsonInputObject, connectionToTDB);

                    currComponentObject.put(currStatement.getPredicate().getLocalName(), subGraph);

                }

            }

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.inputIsRestrictedToSubclassesOf.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            JSONArray selectClassData = new JSONArray();

            // create query to find value in specific composition
            String resultVarClass = "?s";

            PrefixesBuilder prefixesBuilderClass = new PrefixesBuilder();

            SelectBuilder selectBuilderClass = new SelectBuilder();

            selectBuilderClass = prefixesBuilderClass.addPrefixes(selectBuilderClass);

            SelectBuilder tripleSPOConstructClass = new SelectBuilder();

            tripleSPOConstructClass.addWhere(resultVarClass, RDFS.subClassOf, "<" + currStatement.getObject() + ">");

            selectBuilderClass.addVar(selectBuilderClass.makeVar(resultVarClass));

            selectBuilderClass.addGraph("?g", tripleSPOConstructClass);

            String sparqlQueryStringClass = selectBuilderClass.buildString();

            JSONArray classJA = connectionToTDB.pullMultipleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringClass, resultVarClass);

            for (int i = 0; i < classJA.length(); i++) {

                String resultVarLabel = "?s";

                PrefixesBuilder prefixesBuilderLabel = new PrefixesBuilder();

                SelectBuilder selectBuilderLabel = new SelectBuilder();

                selectBuilderLabel = prefixesBuilderLabel.addPrefixes(selectBuilderLabel);

                SelectBuilder tripleSPOConstructLabel = new SelectBuilder();

                tripleSPOConstructLabel.addWhere( "<" + classJA.get(i) + ">", RDFS.label, resultVarLabel);

                selectBuilderLabel.addVar(selectBuilderLabel.makeVar(resultVarLabel));

                selectBuilderLabel.addGraph("?g", tripleSPOConstructLabel);

                String sparqlQueryStringLabel = selectBuilderLabel.buildString();

                String label = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringLabel, resultVarLabel);

                // find tooltip text

                String resultVarTooltip = "?s";

                PrefixesBuilder prefixesBuilderTooltip = new PrefixesBuilder();

                SelectBuilder selectBuilderTooltip = new SelectBuilder();

                selectBuilderTooltip = prefixesBuilderTooltip.addPrefixes(selectBuilderTooltip);

                SelectBuilder tripleSPOConstructTooltip = new SelectBuilder();

                tripleSPOConstructTooltip.addWhere("<" + classJA.get(i) + ">", SprO.tooltipText, resultVarTooltip);
                // tooltip text

                selectBuilderTooltip.addVar(selectBuilderTooltip.makeVar(resultVarTooltip));

                selectBuilderTooltip.addGraph("?g", tripleSPOConstructTooltip);

                String sparqlQueryStringTooltip = selectBuilderTooltip.buildString();

                String tooltipText = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringTooltip, resultVarTooltip);

                // save date in output stream

                JSONObject currSelectIndividual = new JSONObject();

                currSelectIndividual.put("selValue", classJA.get(i));
                currSelectIndividual.put("selLabel", label);

                if (!tooltipText.isEmpty()) {

                    currSelectIndividual.put(SprO.tooltipText.getLocalName(), tooltipText);

                }

                selectClassData.put(currSelectIndividual);

            }

            currComponentObject.put(currStatement.getPredicate().getLocalName(), selectClassData);

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.hiddenForUsersWithoutRightOrRole.toString())) {

            /*System.out.println("schnap: " + currStatement);
            System.out.println("schnap: " + jsonInputObject);*/


            // todo create interface to ask right or role in user named graph http://www.morphdbase.de/resource/de46dd64#SC_BASIC_0000001378_1

            return entryComponents;

        } else if (propertyToCheck.equals(SprO.inputIsRestrictedToIndividualsOf.toString())) {

            System.out.println("in if SprO.inputRestrictedToIndividualsOf");

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            JSONArray selectIndividualData = new JSONArray();

            // create query to find value in specific composition
            String resultVarValues = "?s";

            PrefixesBuilder prefixesBuilderValues = new PrefixesBuilder();

            SelectBuilder selectBuilderValues = new SelectBuilder();

            selectBuilderValues = prefixesBuilderValues.addPrefixes(selectBuilderValues);

            SelectBuilder tripleSPOConstructValues = new SelectBuilder();

            tripleSPOConstructValues.addWhere(resultVarValues , RDF.type, "<" + currStatement.getObject() + ">");

            selectBuilderValues.addVar(selectBuilderValues.makeVar(resultVarValues));

            selectBuilderValues.addGraph("?g", tripleSPOConstructValues);

            String sparqlQueryStringValues = selectBuilderValues.buildString();

            JSONArray valuesJA = connectionToTDB.pullMultipleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringValues, resultVarValues);

            for (int i = 0; i < valuesJA.length(); i++) {

                String resultVarLabel = "?s";

                PrefixesBuilder prefixesBuilderLabel = new PrefixesBuilder();

                SelectBuilder selectBuilderLabel = new SelectBuilder();

                selectBuilderLabel = prefixesBuilderLabel.addPrefixes(selectBuilderLabel);

                SelectBuilder tripleSPOConstructLabel = new SelectBuilder();

                tripleSPOConstructLabel.addWhere( "<" + valuesJA.get(i) + ">", RDFS.label, resultVarLabel);

                selectBuilderLabel.addVar(selectBuilderLabel.makeVar(resultVarLabel));

                selectBuilderLabel.addGraph("?g", tripleSPOConstructLabel);

                String sparqlQueryStringLabel = selectBuilderLabel.buildString();

                String label = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringLabel, resultVarLabel);

                // find tooltip text

                String resultVarTooltip = "?s";

                PrefixesBuilder prefixesBuilderTooltip = new PrefixesBuilder();

                SelectBuilder selectBuilderTooltip = new SelectBuilder();

                selectBuilderTooltip = prefixesBuilderTooltip.addPrefixes(selectBuilderTooltip);

                SelectBuilder tripleSPOConstructTooltip = new SelectBuilder();

                tripleSPOConstructTooltip.addWhere("<" + valuesJA.get(i) + ">", SprO.tooltipText, resultVarTooltip);
                // tooltip text

                selectBuilderTooltip.addVar(selectBuilderTooltip.makeVar(resultVarTooltip));

                selectBuilderTooltip.addGraph("?g", tripleSPOConstructTooltip);

                String sparqlQueryStringTooltip = selectBuilderTooltip.buildString();

                String tooltipText = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryStringTooltip, resultVarTooltip);

                // save date in output stream

                JSONObject currSelectIndividual = new JSONObject();

                currSelectIndividual.put("selValue", valuesJA.get(i));
                currSelectIndividual.put("selLabel", label);

                if (!tooltipText.isEmpty()) {

                    currSelectIndividual.put(SprO.tooltipText.getLocalName(), tooltipText);

                }

                selectIndividualData.put(currSelectIndividual);

            }

            currComponentObject.put(currStatement.getPredicate().getLocalName(), selectIndividualData);

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.componentStatusBOOLEAN.toString())) {

            String property;

            if (currStatement.getObject().asLiteral().getLexicalForm().equals("true")) {

                property = SprO.labelStatusTrue.toString();

            } else {

                property = SprO.labelStatusFalse.toString();

            }

            // create query to find value in specific composition
            String resultVar = "?o";

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            SelectBuilder selectBuilder = new SelectBuilder();

            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

            SelectBuilder tripleSPOConstruct = new SelectBuilder();

            tripleSPOConstruct.addWhere("?bNode", OWL2.annotatedSource, "<" + resourceSubject + ">");
            tripleSPOConstruct.addWhere("?bNode", "<" + property + ">", "?o");

            selectBuilder.addVar(selectBuilder.makeVar(resultVar));

            selectBuilder.addGraph("?g", tripleSPOConstruct);

            String sparqlQueryString = selectBuilder.buildString();

            String value = connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, resultVar);

            currComponentObject.put(ResourceFactory.createProperty(property).getLocalName(), value);

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(SprO.executionStepTrigger.toString())
                || propertyToCheck.equals(SprO.executionStepHyperlink.toString())) {

            return checkAnnotationAnnotationProperties(resourceSubject, currStatement, entryComponents, connectionToTDB);

        } else if (propertyToCheck.equals(SprO.autocompleteForOntology.toString())
                || propertyToCheck.equals(SprO.autocompleteFor.toString())) {

            currStatement = ResourceFactory.createStatement(ResourceFactory.createResource(resourceSubject), currStatement.getPredicate(), currStatement.getObject());

            if (entryComponents.has(currStatement.getSubject().toString())) {

                JSONArray currEntryComponentJSONArray = entryComponents.getJSONArray(currStatement.getSubject().toString());

                for (int i = 0; i < currEntryComponentJSONArray.length(); i++) {

                    JSONObject currEntryComponentJSONObject = currEntryComponentJSONArray.getJSONObject(i);

                    if (currEntryComponentJSONObject.has(currStatement.getPredicate().getLocalName())) {

                        JSONArray currComponentValueJSON = new JSONArray();

                        if (currEntryComponentJSONObject.get(currStatement.getPredicate().getLocalName()) instanceof JSONArray) {

                            currComponentValueJSON = currEntryComponentJSONObject.getJSONArray(currStatement.getPredicate().getLocalName());

                        } else if (currEntryComponentJSONObject.get(currStatement.getPredicate().getLocalName()) instanceof String) {

                            currComponentValueJSON.put(currEntryComponentJSONObject.get(currStatement.getPredicate().getLocalName()).toString());

                        }

                        currComponentValueJSON.put(currStatement.getObject().asResource().toString());

                        currEntryComponentJSONObject.put(currStatement.getPredicate().getLocalName(), currComponentValueJSON);

                        return entryComponents;

                    }

                }

            }

            currComponentObject.put(currStatement.getPredicate().getLocalName(), currStatement.getObject().asResource().toString());

            return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

        } else if (propertyToCheck.equals(RDF.type.toString())) {

            if (!currStatement.getObject().toString().equals(OWL2.NamedIndividual.toString())
                    && !currStatement.getObject().toString().equals(OWL2.Axiom.toString())
                    && !currStatement.getObject().toString().equals(OWL2.Class.toString())) {

                // save the class URI of the individual
                currComponentObject.put("classID", currStatement.getObject().asResource().toString());

                // save the individual URI of the individual
                currComponentObject.put("individualID", currStatement.getSubject().asResource().toString());

                // save the local identifier of the individual
                currComponentObject.put("localID", currStatement.getSubject().asResource().getLocalName());

                return entryComponents.append(currStatement.getSubject().toString(), currComponentObject);

            }

        } else {

            // differ potential interesting properties and uninteresting properties
            if (unknownProperty(currStatement.getPredicate().toString())) {

                //System.out.println("potential Statement to process: " + currStatement);

            }

        }

        return entryComponents;

    }


    /**
     * This method orders an JSONArray related to their position resource
     * @param entryComponentJSONArray this JSONArray contains entry component specific data
     * @param JSONArrayData contains unordered data
     * @param inputPosition the input position of the JSONArray
     * @param parentURI the URI of the parent Resource
     * @return a JSONArray with the data of an entry component
     */
    public JSONArray orderEntryComponents(JSONArray entryComponentJSONArray, JSONArray JSONArrayData, int inputPosition, String parentURI) {

        ArrayList<Integer> entriesToDelete = new ArrayList<>();

        JSONArray entriesOrder = new JSONArray();

        for (int i = 0; i < entryComponentJSONArray.length(); i++) {

            entriesOrder.put("");

        }

        JSONObject alreadyFoundURIIndex = new JSONObject();

        for (int i = 0; i < JSONArrayData.length(); i++) {

            Iterator keyIter = JSONArrayData.getJSONObject(i).keys();

            boolean keyNotFound = true;

            while (keyIter.hasNext() && keyNotFound) {

                String currKey = keyIter.next().toString();

                for (int j = (entryComponentJSONArray.length()-1); j >= 0; j--) {

                    if ((currKey.equals(entryComponentJSONArray.get(j).toString())) &&
                            (!(entryComponentJSONArray.get(j).toString()).equals(parentURI)) &&
                            (!alreadyFoundURIIndex.has(currKey))) {

                        if (JSONArrayData.getJSONObject(i).getJSONObject(currKey).has(SprO.hiddenBOOLEAN.getLocalName())) {

                            boolean hidden = Boolean.parseBoolean(JSONArrayData.getJSONObject(i).getJSONObject(currKey).get(SprO.hiddenBOOLEAN.getLocalName()).toString());

                            if (!hidden) {
                                // show only not hidden parts

                                entriesOrder.put((Integer.parseInt(JSONArrayData.getJSONObject(i).getJSONObject(currKey).get(SprO.hasPositionInEntryComponent.getLocalName()).toString()) - 1), JSONArrayData.getJSONObject(i).getJSONObject(currKey));

                                entriesToDelete.add(i);

                                alreadyFoundURIIndex.put(entryComponentJSONArray.get(j).toString(), entriesToDelete.indexOf(i));

                                keyNotFound = false;

                            } else {

                                entriesToDelete.add(i);

                            }

                        } else {

                            entriesOrder.put((Integer.parseInt(JSONArrayData.getJSONObject(i).getJSONObject(currKey).get(SprO.hasPositionInEntryComponent.getLocalName()).toString()) - 1), JSONArrayData.getJSONObject(i).getJSONObject(currKey));

                            entriesToDelete.add(i);

                            alreadyFoundURIIndex.put(entryComponentJSONArray.get(j).toString(), entriesToDelete.indexOf(i));

                            keyNotFound = false;

                        }

                    } else if ((currKey.equals(entryComponentJSONArray.get(j).toString())) &&
                            (!(entryComponentJSONArray.get(j).toString()).equals(parentURI))) {
                        // if there are multiple occurrence of a resource find the last index in the delete arraylist

                        entriesToDelete.set(alreadyFoundURIIndex.getInt(currKey), i);

                    }

                }

            }

        }

        for (int i = (entriesOrder.length()-1); i >= 0; i--) {

            if (entriesOrder.get(i).equals("")) {

                entriesOrder.remove(i);

            } else if (entriesOrder.isNull(i)) {

                entriesOrder.remove(i);

            }

        }

        //System.out.println("entriesOrder " + entriesOrder);

        JSONArrayData.getJSONObject(inputPosition).getJSONObject(parentURI).put(SprO.hasEntryComponent.getLocalName(), entriesOrder);

        // sort the array from small to large for the case of multiple occurrence of a resource in the arraylist
        Collections.sort(entriesToDelete, Integer::compareTo);

        int arraySizeBeforeDelete = JSONArrayData.length();

        // delete deprecated information
        for (int i = (entriesToDelete.size()-1); i >= 0; i--) {

            System.out.println("entriesToDelete.get(" + i + ") = " + entriesToDelete.get(i));

            if (arraySizeBeforeDelete >= JSONArrayData.length()) {

                JSONArrayData.remove(entriesToDelete.get(i));

            }

        }

        return entryComponentJSONArray;

    }


    /**
     * This method organize an input JSONArray in a nested and ordered JSONArray
     * @param JSONArrayData contains unordered data
     * @return a nested and ordered JSONArray
     */
    public JSONArray orderOutputJSONOld(JSONArray JSONArrayData) {

        for (int i = (JSONArrayData.length()-1); i >= 0; i--) {

            Iterator allKeys = JSONArrayData.getJSONObject(i).keys();

            while (allKeys.hasNext()) {

                String currKey = allKeys.next().toString();

                if (JSONArrayData.getJSONObject(i).has(currKey)) {

                    if (JSONArrayData.getJSONObject(i).getJSONObject(currKey).has(SprO.hasEntryComponent.getLocalName())) {

                        //System.out.println("key value = " + JSONArrayData.getJSONObject(i).getJSONObject(currKey));

                        JSONArray entryComponentJSONArray = JSONArrayData.getJSONObject(i).getJSONObject(currKey).getJSONArray(SprO.hasEntryComponent.getLocalName());

                        orderEntryComponents(entryComponentJSONArray, JSONArrayData, i, currKey);

                    }

                }

            }

        }

        return JSONArrayData;

    }


    /**
     * This method organize an input JSONArray in a nested and ordered JSONArray
     * @param rootURI contains the URI of the root element
     * @param JSONArrayData contains unordered flat JSONArray
     * @return a nested and ordered JSONArray
     */
    public JSONArray orderOutputJSON(String rootURI, JSONArray JSONArrayData) {

        JSONArray outputTreeDataJSON = new JSONArray();

        boolean wasNotFound = true;// todo remove this part if the user database was reset

        for (int i = (JSONArrayData.length()-1); i >= 0; i--) {

            if (JSONArrayData.getJSONObject(i).has(rootURI)) {

                wasNotFound = false;// todo remove this part if the user database was reset

                int position;
                // has position in MDB entry componentURI

                if (!JSONArrayData.getJSONObject(i).getJSONObject(rootURI).has(SprO.hasPositionInEntryComponent.getLocalName())) {

                    System.out.println("Error: the following component has no position: " + rootURI);

                    position = 1;// todo remove the then branch if the user database was reset

                } else {

                    position = Integer.parseInt(JSONArrayData.getJSONObject(i).getJSONObject(rootURI).get(SprO.hasPositionInEntryComponent.getLocalName()).toString());

                }

                JSONArray childrenOfComponent = new JSONArray();

                if (JSONArrayData.getJSONObject(i).getJSONObject(rootURI).has(SprO.hasEntryComponent.getLocalName())) {

                    childrenOfComponent = JSONArrayData.getJSONObject(i).getJSONObject(rootURI).getJSONArray(SprO.hasEntryComponent.getLocalName());

                    int numberOfChildren = JSONArrayData.getJSONObject(i).getJSONObject(rootURI).getJSONArray(SprO.hasEntryComponent.getLocalName()).length();

                    if (numberOfChildren >= 0) {

                        JSONArray childrenOfComponentPlaceholder = new JSONArray();

                        for (int j = 0; j < numberOfChildren; j++) {

                            childrenOfComponentPlaceholder.put(j);

                        }

                        JSONArrayData.getJSONObject(i).getJSONObject(rootURI).put(SprO.hasEntryComponent.getLocalName(), childrenOfComponentPlaceholder);

                    }

                }

                outputTreeDataJSON = putComponentInTree(rootURI, JSONArrayData, childrenOfComponent, position, outputTreeDataJSON);

                if (outputTreeDataJSON.getJSONObject(position - 1).has(SprO.hasEntryComponent.getLocalName())) {

                    // check if some root children are hidden, if true remove the placeholder from the output tree
                    JSONArray rootChildren = outputTreeDataJSON.getJSONObject(position - 1).getJSONArray(SprO.hasEntryComponent.getLocalName());

                    for (int j = (rootChildren.length()-1); j >= 0; j--) {

                        if (!(rootChildren.get(j) instanceof JSONObject)) {

                            rootChildren.remove(j);

                        }

                    }

                }

                if (JSONArrayData.getJSONObject(i).getJSONObject(rootURI).has("BFO_0000051")) {

                    childrenOfComponent = JSONArrayData.getJSONObject(i).getJSONObject(rootURI).getJSONArray("BFO_0000051");

                    int numberOfChildren = JSONArrayData.getJSONObject(i).getJSONObject(rootURI).getJSONArray("BFO_0000051").length();

                    if (numberOfChildren >= 0) {

                        JSONArray childrenOfComponentPlaceholder = new JSONArray();

                        for (int j = 0; j < numberOfChildren; j++) {

                            childrenOfComponentPlaceholder.put(j);

                        }

                        JSONArrayData.getJSONObject(i).getJSONObject(rootURI).put("BFO_0000051", childrenOfComponentPlaceholder);

                    }

                    outputTreeDataJSON = putPartonomyComponentInTree(rootURI, JSONArrayData, childrenOfComponent, position, outputTreeDataJSON);

                    if (outputTreeDataJSON.getJSONObject(position - 1).has("BFO_0000051")) {

                        // check if some root children are hidden, if true remove the placeholder from the output tree
                        JSONArray rootChildren = outputTreeDataJSON.getJSONObject(position - 1).getJSONArray("BFO_0000051");

                        for (int j = (rootChildren.length()-1); j >= 0; j--) {

                            if (!(rootChildren.get(j) instanceof JSONObject)) {

                                rootChildren.remove(j);

                            }

                        }

                    }

                }

            }

        }

        // todo check if the special cases for the draft and admin workspaces are really done. if there were no errors
        // todo after ontology merging everything is cool and the todo could be deleted

        return outputTreeDataJSON;

    }


    /**
     * This method organize an input JSONArray in a nested and ordered JSONArray
     * @param rootURI contains the URI of the root element
     * @param JSONArrayData contains unordered flat JSONArray
     * @return a nested and ordered JSONArray
     */
    public JSONArray orderSubCompositionOutputJSON(String rootURI, JSONArray JSONArrayData) {

        JSONArray outputTreeDataJSON = new JSONArray();


        for (int i = (JSONArrayData.length()-1); i >= 0; i--) {

            if (JSONArrayData.getJSONObject(i).has(rootURI)) {

                int position = 1; // there is only one position for each sub composition tree

                JSONArray childrenOfComponent = new JSONArray();

                if (JSONArrayData.getJSONObject(i).getJSONObject(rootURI).has(SprO.hasEntryComponent.getLocalName())) {

                    childrenOfComponent = JSONArrayData.getJSONObject(i).getJSONObject(rootURI).getJSONArray(SprO.hasEntryComponent.getLocalName());

                    int numberOfChildren = JSONArrayData.getJSONObject(i).getJSONObject(rootURI).getJSONArray(SprO.hasEntryComponent.getLocalName()).length();

                    if (numberOfChildren >= 0) {

                        JSONArray childrenOfComponentPlaceholder = new JSONArray();

                        for (int j = 0; j < numberOfChildren; j++) {

                            childrenOfComponentPlaceholder.put(j);

                        }

                        JSONArrayData.getJSONObject(i).getJSONObject(rootURI).put(SprO.hasEntryComponent.getLocalName(), childrenOfComponentPlaceholder);

                    }

                }

                outputTreeDataJSON = putComponentInTree(rootURI, JSONArrayData, childrenOfComponent, position, outputTreeDataJSON);

                // check if some root children are hidden, if true remove the placeholder from the output tree
                JSONArray rootChildren = outputTreeDataJSON.getJSONObject(position - 1).getJSONArray(SprO.hasEntryComponent.getLocalName());

                for (int j = (rootChildren.length()-1); j >= 0; j--) {

                    if (!(rootChildren.get(j) instanceof JSONObject)) {

                        rootChildren.remove(j);

                    }

                }

            }

        }

        return outputTreeDataJSON;

    }


    /**
     * This method constructs a nested and ordered JSONArray from a flat input JSON Array
     * @param componentURI contains the URI of the current tree component
     * @param JSONArrayData contains unordered flat JSONArray
     * @param childrenOfComponent contains the URIs of the children
     * @param position contains the position of the component
     * @param outputTreeDataJSON contains a nested and ordered JSONArray
     * @return a nested and ordered JSONArray
     */
    private JSONArray putComponentInTree(String componentURI, JSONArray JSONArrayData, JSONArray childrenOfComponent,
                                        int position, JSONArray outputTreeDataJSON) {

        // put the current data in the output tree JSON and remove the data afterwards from the flat JSON
        for (int i = 0; i < JSONArrayData.length(); i++) {

            if (JSONArrayData.getJSONObject(i).has(componentURI)) {

                outputTreeDataJSON.put(position - 1, JSONArrayData.getJSONObject(i).getJSONObject(componentURI));

            }

        }

        if (childrenOfComponent.length() > 0) {

            for (int i = 0; i < childrenOfComponent.length(); i++) {

                if (childrenOfComponent.get(i) instanceof String) {

                    String newComponentURI = childrenOfComponent.get(i).toString();

                    int currPosition = -1;

                    JSONArray currChildrenOfComponent = new JSONArray();

                    boolean hidden = false;

                    for (int j = 0; j < JSONArrayData.length(); j++) {

                        if (JSONArrayData.getJSONObject(j).has(newComponentURI)) {

                            if (!JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).has(SprO.hasPositionInEntryComponent.getLocalName())) {

                                System.out.println("Error: the following component has no position: " + newComponentURI);

                            } else {

                                currPosition = Integer.parseInt(JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).get(SprO.hasPositionInEntryComponent.getLocalName()).toString());

                            }

                            if (JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).has(SprO.hasEntryComponent.getLocalName())) {

                                currChildrenOfComponent = JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).getJSONArray(SprO.hasEntryComponent.getLocalName());

                            }

                            if (JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).has(SprO.hiddenBOOLEAN.getLocalName())) {

                                hidden = Boolean.parseBoolean(JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).get(SprO.hiddenBOOLEAN.getLocalName()).toString());

                            }

                        }

                    }

                    if (!hidden) {

                        // it is important to clone the current JSONArray
                        JSONArray currOutputTreeDataJSON = new JSONArray(outputTreeDataJSON.getJSONObject(position - 1).getJSONArray(SprO.hasEntryComponent.getLocalName()).toString());

                        currOutputTreeDataJSON = putComponentInTree(newComponentURI, JSONArrayData, currChildrenOfComponent, currPosition, currOutputTreeDataJSON);

                        outputTreeDataJSON.getJSONObject(position - 1).put(SprO.hasEntryComponent.getLocalName(), currOutputTreeDataJSON);

                    }

                }

            }

        }

        return outputTreeDataJSON;

    }


    /**
     * This method constructs a nested and ordered JSONArray from a flat input JSON Array
     * @param componentURI contains the URI of the current tree component
     * @param JSONArrayData contains unordered flat JSONArray
     * @param childrenOfComponent contains the URIs of the children
     * @param position contains the position of the component
     * @param outputTreeDataJSON contains a nested and ordered JSONArray
     * @return a nested and ordered JSONArray
     */
    private JSONArray putPartonomyComponentInTree(String componentURI, JSONArray JSONArrayData,
                                                  JSONArray childrenOfComponent, int position,
                                                  JSONArray outputTreeDataJSON) {

        // put the current data in the output tree JSON and remove the data afterwards from the flat JSON
        for (int i = 0; i < JSONArrayData.length(); i++) {

            if (JSONArrayData.getJSONObject(i).has(componentURI)) {

                outputTreeDataJSON.put(position - 1, JSONArrayData.getJSONObject(i).getJSONObject(componentURI));

            }

        }

        if (childrenOfComponent.length() > 0) {

            for (int i = 0; i < childrenOfComponent.length(); i++) {

                if (childrenOfComponent.get(i) instanceof String) {

                    String newComponentURI = childrenOfComponent.get(i).toString();

                    int currPosition = -1;

                    JSONArray currChildrenOfComponent = new JSONArray();

                    boolean hidden = false;

                    for (int j = 0; j < JSONArrayData.length(); j++) {

                        if (JSONArrayData.getJSONObject(j).has(newComponentURI)) {

                            if (!JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).has(SprO.hasPositionInEntryComponent.getLocalName())) {

                                System.out.println("Error: the following component has no position: " + newComponentURI);

                            }  else {

                                currPosition = Integer.parseInt(JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).get(SprO.hasPositionInEntryComponent.getLocalName()).toString());

                            }

                            if (JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).has("BFO_0000051")) {

                                currChildrenOfComponent = JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).getJSONArray("BFO_0000051");

                            }

                            if (JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).has(SprO.hiddenBOOLEAN.getLocalName())) {

                                hidden = Boolean.parseBoolean(JSONArrayData.getJSONObject(j).getJSONObject(newComponentURI).get(SprO.hiddenBOOLEAN.getLocalName()).toString());

                            }

                        }

                    }

                    if (!hidden) {

                        // it is important to clone the current JSONArray
                        JSONArray currOutputTreeDataJSON = new JSONArray(outputTreeDataJSON.getJSONObject(position - 1).getJSONArray("BFO_0000051").toString());

                        currOutputTreeDataJSON = putPartonomyComponentInTree(newComponentURI, JSONArrayData, currChildrenOfComponent, currPosition, currOutputTreeDataJSON);

                        outputTreeDataJSON.getJSONObject(position - 1).put("BFO_0000051", currOutputTreeDataJSON);

                    }

                }

            }

        }

        return outputTreeDataJSON;

    }



    /**
     * This method reordered the entry component JSONObject
     * @param entryComponents contains the data of an entry resource
     * @return a reordered JSONObject for later calculation
     */
    public JSONObject reorderEntryComponentsValuesOld(JSONObject entryComponents) {

        Iterator entryComponentsIter = entryComponents.keys();

        String key = entryComponentsIter.next().toString();

        JSONArray entryComponentsArray = entryComponents.getJSONArray(key);

        JSONObject allComponentsInOneObject = new JSONObject();

        for (int i = 0; i < entryComponentsArray.length(); i++) {

            Iterator entryInnerComponentsIter = entryComponentsArray.getJSONObject(i).keys();

            while (entryInnerComponentsIter.hasNext()) {

                String currInnerKey = entryInnerComponentsIter.next().toString();

                Object currInnerValue = entryComponentsArray.getJSONObject(i).get(currInnerKey);

                if (allComponentsInOneObject.has(currInnerKey)) {

                    if (currInnerValue instanceof JSONArray) {

                        JSONArray currInnerValueJSONArray = (JSONArray) currInnerValue;

                        allComponentsInOneObject.append(currInnerKey, currInnerValueJSONArray.get(0));

                    }

                } else {

                    allComponentsInOneObject.put(currInnerKey, currInnerValue);

                }

            }

        }

        return entryComponents.put(key, allComponentsInOneObject);

    }


    /**
     * This method reordered the entry component JSONObject
     * @param entryComponents contains the data of an entry resource
     * @return a reordered JSONObject for later calculation
     */
    public JSONObject reorderEntryComponentsValues(JSONObject entryComponents) {

        Iterator entryComponentsIter = entryComponents.keys();

        while (entryComponentsIter.hasNext()) {

            String key = entryComponentsIter.next().toString();

            JSONArray entryComponentsArray = entryComponents.getJSONArray(key);

            JSONObject allComponentsInOneObject = new JSONObject();

            for (int i = 0; i < entryComponentsArray.length(); i++) {

                Iterator entryInnerComponentsIter = entryComponentsArray.getJSONObject(i).keys();

                while (entryInnerComponentsIter.hasNext()) {

                    String currInnerKey = entryInnerComponentsIter.next().toString();

                    Object currInnerValue = entryComponentsArray.getJSONObject(i).get(currInnerKey);

                    if (allComponentsInOneObject.has(currInnerKey)) {

                        if (currInnerValue instanceof JSONArray) {

                            JSONArray allChildComponentsInOneArray = (JSONArray) currInnerValue;

                            allComponentsInOneObject.accumulate(currInnerKey, allChildComponentsInOneArray.get(0));

                        }

                    } else {

                        allComponentsInOneObject.put(currInnerKey, currInnerValue);

                    }

                }

            }

            entryComponents.put(key, allComponentsInOneObject);

        }

        return entryComponents;

    }



    /**
     * This method sets the path of the current work directory
     * @param pathToOntologies contains the path to the ontology workspace
     */
    public void setPathToOntologies(String pathToOntologies) {
        this.pathToOntologies = pathToOntologies;
    }


    /**
     * This method checks the not processed properties.
     * @param property contains the data to check.
     * @return true if the property is a potential candidate for processing otherwise false
     */
    private boolean unknownProperty(String property) {

        if (property.equals("http://purl.obolibrary.org/obo/IAO_0000115")
            // definition
            || property.equals("http://www.geneontology.org/formats/oboInOwl#id")
            // id
            || property.equals(SprO.hasAssociatedInstanceResourceInputA.getNameSpace())
            || property.equals(SprO.entryCompositionOf.getNameSpace())
            || property.equals(SprO.executionStepSaveDeleteTripleStatementS.getNameSpace())
            || property.equals(SprO.triggersWorkflowAction.getNameSpace())
            || property.equals(SprO.objectSOCCOMAS.getNameSpace())
            || property.equals(SprO.propertySOCCOMAS.getNameSpace())
            || property.equals(SprO.subjectSOCCOMAS.getNameSpace())
            || property.equals(SprO.mandatoryEntryComponentBOOLEAN.getNameSpace())
            || property.equals(SprO.loadFromSaveToUpdateInNamedGraph.getNameSpace())
            || property.equals(SprO.namedGraphBelongsToWorkspace.getNameSpace())
            || property.equals(SprO.executionStepCommand.getNameSpace())
            || property.equals(SprO.thenSOCCOMAS.getNameSpace())
            || property.equals(SprO.elseSOCCOMAS.getNameSpace())
            || property.equals(SprO.executionStepIfThenElseStatement.getNameSpace())
            || property.equals(SprO.executionStepCopyAndSaveTripleStatementS.getNameSpace())
            || property.equals(SprO.copyFromNamedGraphOfClass.getNameSpace())
            || property.equals(SprO.updateWithResourceValue.getNameSpace())
            || property.equals(SprO.executionStepUpdateTripleStatementS.getNameSpace())
            || property.equals(SprO.inputRestrictedToIndividualsOf.getNameSpace())
            || property.equals(SprO.deleteTripleStatementBOOLEAN.getNameSpace())
            || property.equals(SprO.requirementForTriggeringAWorkflowAction.getNameSpace())
            || property.equals(SprO.executionStepDecisionDialogue.getNameSpace())
            || property.equals(SprO.subsequentInputThroughGUIModule.getNameSpace())
            || property.equals(SprO.position.getNameSpace())
            || property.equals(SprO.label1.getNameSpace())
            || property.equals(SprO.hasTargetEntryComponent.getNameSpace())
            || property.equals(SprO.executionStepTriggerWorkflowAction.getNameSpace())
            || property.equals(SprO.subjectThisEntrySSpecificIndividualOf.getNameSpace())
            || property.equals(SprO.waitForExecutionBOOLEAN.getNameSpace())
            || property.equals(SprO.goToExecutionStep.getNameSpace())
            || property.equals(SprO.switchToPage.getNameSpace())
            || property.equals(SprO.labelStatusTrue.getNameSpace())
            || property.equals(SprO.labelStatusFalse.getNameSpace())
            || property.equals(SprO.executionStepSendEmail.getNameSpace())
            || property.equals(SprO.emailToMbox.getNameSpace())
            || property.equals(SprO.emailText1.getNameSpace())
            || property.equals(SprO.emailText2.getNameSpace())
            || property.equals(SprO.emailSubject.getNameSpace())
            || property.equals(SprO.executionStepTriggered.getNameSpace())
            || property.equals(SprO.requiresGUIInputValueResource.getNameSpace())
            || property.equals(SprO.requirementForTriggeringTheExecutionStep.getNameSpace())
            || property.equals(SprO.guiModuleInputType.getNameSpace())
            || property.equals(SprO.guiModuleInputUsedInEntryComponent.getNameSpace())
            || property.equals(SprO.hasIFInputValue.getNameSpace())
            || property.equals(SprO.hasIFOperation.getNameSpace())
            || property.equals(SprO.hasIFTargetValue.getNameSpace())
            || property.equals(SprO.executionStepTriggersUserInput.getNameSpace())
            || property.equals(SprO.executionStepCloseModule.getNameSpace())
            || property.equals(SprO.closeModuleBOOLEAN.getNameSpace())
            || property.equals(SprO.executionStepSearchTripleStore.getNameSpace())
            || property.equals(SprO.searchTargetDefinesSPrOVariable.getNameSpace())
            || property.equals(SprO.userInputSPrOVariableDefinedByInput.getNameSpace())
            || property.equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.getNameSpace())
            || property.equals(SprO.executionStepDefineVariables.getNameSpace())
            || property.equals(RDF.type.getNameSpace())
            || property.equals(RDFS.label.getNameSpace())
            || property.equals(RDFS.subClassOf.getNameSpace())
            || property.equals(OWL2.annotatedProperty.getNameSpace())
            || property.equals(OWL2.annotatedSource.getNameSpace())
            || property.equals(OWL2.annotatedTarget.getNameSpace())) {

            return false;

        } else {

            return true;

        }

    }

}