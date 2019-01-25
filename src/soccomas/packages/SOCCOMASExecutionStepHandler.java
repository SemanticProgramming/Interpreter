/*
 * Created by Roman Baum on 10.04.15.
 * Last modified by Roman Baum on 24.01.19.
 */

package soccomas.packages;

import soccomas.basic.*;
import soccomas.mongodb.MongoDBConnection;
import soccomas.packages.operation.OperationManager;
import soccomas.packages.operation.OutputGenerator;
import soccomas.packages.querybuilder.FilterBuilder;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.packages.querybuilder.SPARQLFilter;
import soccomas.vocabulary.*;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;


public class SOCCOMASExecutionStepHandler {

    private String pathToOntologies = ApplicationConfigurator.getPathToApplicationOntologyStore();

    // create a model to get all annotation properties
    private AnnotationPropertySet annotationPropertySet = new AnnotationPropertySet(this.pathToOntologies);
    private JSONObject annotationPropertiesInJSON = this.annotationPropertySet.createJSONObject();

    // create a model to get all data properties
    private DataPropertySet dataPropertySet = new DataPropertySet(this.pathToOntologies);
    private JSONObject dataPropertiesInJSON = this.dataPropertySet.createJSONObject();

    // create a model to get all object properties
    private ObjectPropertySet objectPropertySet = new ObjectPropertySet(this.pathToOntologies);
    private JSONObject objectPropertiesInJSON = this.objectPropertySet.createJSONObject();

    private SOCCOMASDate soccomasDat = new SOCCOMASDate();

    private JSONObject bNodeIdentifier = new JSONObject();

    // create a model to get all classes
    private ClassSet classSet = new ClassSet(this.pathToOntologies);
    private Model classModel = this.classSet.createModel();

    private String mdbCoreID = "", mdbEntryID = "", mdbUEID = "", currentFocus = "", parentRoot = "", createOverlayNG,
            tabToUse, previousCalculatedNG, tabToUseURI, newPositionCompositionPart = "", executionStepFocus = "";

    private boolean mdbCoreIDNotEmpty = false, mdbEntryIDNotEmpty = false, mdbUEIDNotEmpty = false,
            focusHasNewNS = false, parentRootExist = false, hasCreateOverlayInput = false, updateComposition = false,
            useTab = false, multipleExecutionStepFocus = false, hasExecutionStepFocus = false;

    private int parentRootPosition;

    private JSONObject generatedResources = new JSONObject(), identifiedResources = new JSONObject(),
            infoInput = new JSONObject(), numberOfClassInstances = new JSONObject(),
            classOverlayMapping = new JSONObject(), numberOfClassInstancesOverlay = new JSONObject(),
            entrySpecificAndDefaultResourcesMap = new JSONObject(), rootResourcesOfCompositions = new JSONObject(),
            parentComponents = new JSONObject(), compositionUpdateJSON = new JSONObject();

    private JSONArray executionStepFocuses = new JSONArray();

    private Model overlayModel;

    private MongoDBConnection mongoDBConnection;

    public SOCCOMASExecutionStepHandler(MongoDBConnection mongoDBConnection) {

        this.mongoDBConnection = mongoDBConnection;

    }

    public SOCCOMASExecutionStepHandler(JSONObject identifiedResources, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.identifiedResources = identifiedResources;
        this.overlayModel = overlayModel;
        this.mongoDBConnection = mongoDBConnection;

    }

    public SOCCOMASExecutionStepHandler(JSONObject identifiedResources, JSONObject infoInput, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.identifiedResources = identifiedResources;
        this.infoInput = infoInput;
        this.overlayModel = overlayModel;
        this.mongoDBConnection = mongoDBConnection;

    }

    public SOCCOMASExecutionStepHandler(String mdbUEID, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.mdbUEID = mdbUEID;
        this.currentFocus = mdbUEID;
        this.mdbUEIDNotEmpty = true;
        this.overlayModel = overlayModel;
        this.mongoDBConnection = mongoDBConnection;

    }

    public SOCCOMASExecutionStepHandler(String mdbUEID, JSONObject identifiedResources, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.mdbUEID = mdbUEID;
        this.currentFocus = mdbUEID;
        this.identifiedResources = identifiedResources;
        this.mdbUEIDNotEmpty = true;
        this.overlayModel = overlayModel;
        this.mongoDBConnection = mongoDBConnection;

    }

    public SOCCOMASExecutionStepHandler(String mdbUEID, JSONObject identifiedResources, JSONObject infoInput, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.mdbUEID = mdbUEID;
        this.currentFocus = mdbUEID;
        this.identifiedResources = identifiedResources;
        this.infoInput = infoInput;
        this.mdbUEIDNotEmpty = true;
        this.overlayModel = overlayModel;
        this.mongoDBConnection = mongoDBConnection;

    }

    public SOCCOMASExecutionStepHandler(String mdbCoreID, String mdbEntryID, String mdbUEID, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.mdbCoreID = mdbCoreID;
        this.mdbEntryID = mdbEntryID;
        this.currentFocus = mdbEntryID;
        this.mdbUEID = mdbUEID;
        this.mdbCoreIDNotEmpty = true;
        this.mdbEntryIDNotEmpty = true;
        this.mdbUEIDNotEmpty = true;
        this.overlayModel = overlayModel;
        this.mongoDBConnection = mongoDBConnection;

    }

    public SOCCOMASExecutionStepHandler(String mdbCoreID, String mdbEntryID, String mdbUEID, JSONObject identifiedResources, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.mdbCoreID = mdbCoreID;
        this.mdbEntryID = mdbEntryID;
        this.currentFocus = mdbEntryID;
        this.mdbUEID = mdbUEID;
        this.identifiedResources = identifiedResources;
        this.mdbCoreIDNotEmpty = true;
        this.mdbEntryIDNotEmpty = true;
        this.mdbUEIDNotEmpty = true;
        this.overlayModel = overlayModel;
        this.mongoDBConnection = mongoDBConnection;

    }

    public SOCCOMASExecutionStepHandler(String mdbCoreID, String mdbEntryID, String mdbUEID, JSONObject identifiedResources, JSONObject infoInput, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.mdbCoreID = mdbCoreID;
        this.mdbEntryID = mdbEntryID;
        this.currentFocus = mdbEntryID;
        this.mdbUEID = mdbUEID;
        this.identifiedResources = identifiedResources;
        this.infoInput = infoInput;
        this.mdbCoreIDNotEmpty = true;
        this.mdbEntryIDNotEmpty = true;
        this.mdbUEIDNotEmpty = true;
        this.overlayModel = overlayModel;
        this.mongoDBConnection = mongoDBConnection;

    }




    /**
     * This method calculates a default entry composition of an individual
     * @param individualToCheck contains the root individual, which should be use for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an unnamed model with a default entry composition
     */
    private Model calculateDefaultEntryComposition(JSONArray individualToCheck, JenaIOTDBFactory connectionToTDB) {

        String root = individualToCheck.get(0).toString();

        Model defaultComposition = ModelFactory.createDefaultModel();

        JSONArray allClassesFromTDB = new JSONArray();

        while (!individualToCheck.isNull(0)) {

            FilterBuilder filterBuilder = new FilterBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            ConstructBuilder constructBuilder = new ConstructBuilder();

            constructBuilder = prefixesBuilder.addPrefixes(constructBuilder);

            constructBuilder.addConstruct("?s", "?p", "?o");

            SelectBuilder tripleSPOConstruct = new SelectBuilder();

            tripleSPOConstruct.addWhere("?s", "?p", "?o");

            SPARQLFilter sparqlFilter = new SPARQLFilter();

            ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

            filterItems = filterBuilder.addItems(filterItems, "?s", "<" + individualToCheck.get(0).toString() + ">");

            ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

            constructBuilder = filterBuilder.addFilter(constructBuilder, filter);

            filterItems.clear();

            constructBuilder.addGraph("?g", tripleSPOConstruct);

            String sparqlQueryString = constructBuilder.buildString();

            Model currIndividualModel = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

            defaultComposition.add(currIndividualModel);

            StmtIterator resultIterator = currIndividualModel.listStatements();

            if (currIndividualModel.contains(ResourceFactory.createResource(individualToCheck.get(0).toString()), SCBasic.isRootEntryComponentOfCompositionContainedInNamedGraph) &&
                    (!individualToCheck.get(0).toString().equals(root))) {

                System.out.println("The individual " + individualToCheck.get(0).toString() + " is another root element");


            } else {

                while (resultIterator.hasNext()) {

                    Statement currStmt = resultIterator.nextStatement();

                    Property currProperty = currStmt.getPredicate();

                    if (currProperty.equals(SprO.hasEntryComponent)) {

                        individualToCheck.put(currStmt.getObject().toString());

                    } else if (currProperty.equals(SprO.belongsToRadioButtonGroup)) {
                        // belongs to radio button group

                        individualToCheck.put(currStmt.getObject().toString());

                    } else if (currProperty.equals(RDF.type)) {
                        // rdf:type

                        if (!((currStmt.getObject()).equals(OWL2.NamedIndividual))) {

                            allClassesFromTDB.put(currStmt.getObject().toString());

                        }

                    }

                }
            }

            // find axiom for this individual
            PrefixesBuilder prefixesAxiomBuilder = new PrefixesBuilder();

            ConstructBuilder constructAxiomBuilder = new ConstructBuilder();

            constructAxiomBuilder = prefixesAxiomBuilder.addPrefixes(constructAxiomBuilder);

            constructAxiomBuilder.addConstruct("?s", "?p", "?o");

            SelectBuilder tripleAxiomSPOConstruct = new SelectBuilder();

            tripleAxiomSPOConstruct.addWhere("?s", "?p", "?o");
            tripleAxiomSPOConstruct.addWhere("?s", OWL2.annotatedSource, "<" + individualToCheck.get(0).toString() + ">");

            constructAxiomBuilder.addGraph("?g", tripleAxiomSPOConstruct);

            sparqlQueryString = constructAxiomBuilder.buildString();

            Model currIndividualAxiomModel = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

            // add axioms individual statements
            defaultComposition.add(currIndividualAxiomModel);

            // remove the old key
            individualToCheck.remove(0);

        }

        for (int i = 0; i < allClassesFromTDB.length(); i++) {

            // find the statements for the corresponding class
            FilterBuilder filterBuilder = new FilterBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            ConstructBuilder constructBuilder = new ConstructBuilder();

            constructBuilder = prefixesBuilder.addPrefixes(constructBuilder);

            constructBuilder.addConstruct("?s", "?p", "?o");

            SelectBuilder tripleSPOConstruct = new SelectBuilder();

            tripleSPOConstruct.addWhere("?s", "?p", "?o");

            constructBuilder.addGraph("?g", tripleSPOConstruct);

            SPARQLFilter sparqlFilter = new SPARQLFilter();

            ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

            filterItems = filterBuilder.addItems(filterItems, "?s", "<" + allClassesFromTDB.get(i) + ">");

            ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

            constructBuilder = filterBuilder.addFilter(constructBuilder, filter);

            filterItems.clear();

            String sparqlQueryString = constructBuilder.buildString();

            Model currClassModel = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

            defaultComposition.add(currClassModel);


            // find axiom for the corresponding class
            FilterBuilder filterAxiomBuilder = new FilterBuilder();

            PrefixesBuilder prefixesAxiomBuilder = new PrefixesBuilder();

            ConstructBuilder constructAxiomBuilder = new ConstructBuilder();

            constructAxiomBuilder = prefixesAxiomBuilder.addPrefixes(constructAxiomBuilder);

            constructAxiomBuilder.addConstruct("?s", "?p", "?o");

            SelectBuilder tripleAxiomSPOConstruct = new SelectBuilder();

            tripleAxiomSPOConstruct.addWhere("?s", "?p", "?o");
            tripleAxiomSPOConstruct.addWhere("?s", "?p1", "?o1");

            constructAxiomBuilder.addGraph("?g", tripleAxiomSPOConstruct);

            SPARQLFilter sparqlAxiomFilter = new SPARQLFilter();

            ArrayList<ArrayList<String>> filterAxiomItems = new ArrayList<>();

            filterAxiomItems = filterAxiomBuilder.addItems(filterAxiomItems, "?p1", "<" + OWL2.annotatedSource.toString() + ">");

            filterAxiomItems = filterAxiomBuilder.addItems(filterAxiomItems, "?o1", "<" + allClassesFromTDB.get(i) + ">");

            ArrayList<String> axiomFilter = sparqlAxiomFilter.getINFilter(filterAxiomItems);

            constructAxiomBuilder = filterAxiomBuilder.addFilter(constructAxiomBuilder, axiomFilter);

            filterAxiomItems.clear();

            sparqlQueryString = constructAxiomBuilder.buildString();

            currClassModel = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

            defaultComposition.add(currClassModel);

        }

        return defaultComposition;


    }

    /**
     * This method checks if an input URI is a keyword
     * @param potentialKeyword contains an URI
     * @return the value of a keyword or the input URI
     */
    private String calculateValueForKeyword(String potentialKeyword) {

        if (potentialKeyword.contains("__SPRO_")) {

            String localNamePropertyInObject = potentialKeyword.substring(potentialKeyword.indexOf("__") + 2);

            Iterator<String> keyIterator = this.generatedResources.keys();

            while (keyIterator.hasNext()) {

                String currKey = keyIterator.next();

                // get local name of a key
                String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                if (localNameOfKey.equals(localNamePropertyInObject)) {
                    // get ng from generated resources

                    if (this.mdbCoreIDNotEmpty
                            && this.mdbUEIDNotEmpty
                            && !this.mdbEntryIDNotEmpty) {


                    } else if (this.mdbEntryIDNotEmpty
                            && this.mdbUEIDNotEmpty) {

                        return this.generatedResources.get(currKey).toString();


                    } else if (this.mdbUEIDNotEmpty) {

                        return this.generatedResources.get(currKey).toString();

                    }

                }

            }

            // check identified resources
            Iterator<String> identifiedResIterator = this.identifiedResources.keys();

            while (identifiedResIterator.hasNext()) {

                String currKey = identifiedResIterator.next();

                if (currKey.equals(potentialKeyword)) {
                    // get already identified resource from cache

                    if (EmailValidator.getInstance().isValid(this.identifiedResources.get(currKey).toString())) {
                        // convert mail to a complete uri

                        return "mailto:" + this.identifiedResources.get(currKey).toString();

                    }

                    return this.identifiedResources.get(currKey).toString();

                }

            }

            // check info input
            Iterator<String> infoInputKeys = this.infoInput.keys();

            while (infoInputKeys.hasNext()) {

                String currKey = infoInputKeys.next();

                if (currKey.equals(potentialKeyword)) {

                    return this.infoInput.get(currKey).toString();

                }

            }

        } else {

            // check identified resources
            Iterator<String> identifiedResIterator = this.identifiedResources.keys();

            while (identifiedResIterator.hasNext()) {

                String currKey = identifiedResIterator.next();

                if (currKey.equals(potentialKeyword)) {
                    // get already identified resource from cache

                    return this.identifiedResources.get(currKey).toString();

                }

            }

            // check info input
            Iterator<String> infoInputKeys = this.infoInput.keys();

            while (infoInputKeys.hasNext()) {

                String currKey = infoInputKeys.next();

                if (currKey.equals(potentialKeyword)) {

                    return this.infoInput.get(currKey).toString();

                }

            }

        }

        // return object from input
        return potentialKeyword;

    }


    private void calculateFocusForExecutionStep(String uriForFocusCalculation, JenaIOTDBFactory connectionToTDB) {

        if (uriForFocusCalculation.equals(SprO.sproVARIABLEThisUserEntryID.toString())) {

            this.executionStepFocus = this.mdbUEID;

        } else if (checkValueOfKeywordIsJSONArray(uriForFocusCalculation)) {

            this.multipleExecutionStepFocus = true;

            this.executionStepFocuses = calculateValueListForKeyword(uriForFocusCalculation);

        } else {

            System.out.println("uriForFocusCalculation = " + uriForFocusCalculation);

            System.out.println("calculateValueForKeyword(uriForFocusCalculation) = " + calculateValueForKeyword(uriForFocusCalculation));

            SOCCOMASIDFinder soccomasIDFinder = new SOCCOMASIDFinder(calculateValueForKeyword(uriForFocusCalculation), connectionToTDB);

            boolean noMDBIDWasFound = true;

            if (soccomasIDFinder.hasMDBEntryID()) {

                this.executionStepFocus = soccomasIDFinder.getMDBEntryID();

                noMDBIDWasFound = false;

            }

            if (!soccomasIDFinder.hasMDBEntryID()
                    && soccomasIDFinder.hasMDBCoreID()) {

                this.executionStepFocus = soccomasIDFinder.getMDBCoreID();

                noMDBIDWasFound = false;

            }

            if (!soccomasIDFinder.hasMDBEntryID()
                    && !soccomasIDFinder.hasMDBCoreID()
                    && soccomasIDFinder.hasMDBUEID()) {

                this.executionStepFocus = soccomasIDFinder.getMDBUEID();

                noMDBIDWasFound = false;

            }

            if (noMDBIDWasFound) {

                SOCCOMASIDChecker soccomasIDChecker = new SOCCOMASIDChecker();

                soccomasIDChecker.isMDBID(calculateValueForKeyword(uriForFocusCalculation), connectionToTDB);

                if (soccomasIDChecker.isMDBEntryID()) {

                    this.executionStepFocus = soccomasIDChecker.getMDBEntryID();

                }

                if (!soccomasIDChecker.isMDBEntryID()
                        && soccomasIDChecker.isMDBCoreID()) {

                    this.executionStepFocus = soccomasIDChecker.getMDBCoreID();

                }

                if (!soccomasIDChecker.isMDBEntryID()
                        && !soccomasIDChecker.isMDBCoreID()
                        && soccomasIDChecker.isMDBUEID()) {

                    this.executionStepFocus = soccomasIDChecker.getMDBUEID();

                }

            }

        }

        this.hasExecutionStepFocus = true;

        System.out.println("INFO: An execution step specific focus was set.");

    }

    /**
     * This method checks if a keyword has a JSONArray as value or not.
     * @param potentialKeyword contains an URI
     * @return 'true' if value has type JSONArray, else 'false'
     */
    private boolean checkValueOfKeywordIsJSONArray(String potentialKeyword) {

        if (potentialKeyword.contains("__SPRO_")) {

            String localNamePropertyInObject = potentialKeyword.substring(potentialKeyword.indexOf("__") + 2);

            Iterator<String> keyIterator = this.generatedResources.keys();

            while (keyIterator.hasNext()) {

                String currKey = keyIterator.next();

                // get local name of a key
                String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                if (localNameOfKey.equals(localNamePropertyInObject)) {
                    // get ng from generated resources

                    return this.generatedResources.get(currKey) instanceof JSONArray;

                }

            }

            // check identified resources
            Iterator<String> identifiedResIterator = this.identifiedResources.keys();

            while (identifiedResIterator.hasNext()) {

                String currKey = identifiedResIterator.next();

                if (currKey.equals(potentialKeyword)) {
                    // get already identified resource from cache

                    return this.identifiedResources.get(currKey) instanceof JSONArray;

                }

            }

            // check info input
            Iterator<String> infoInputKeys = this.infoInput.keys();

            while (infoInputKeys.hasNext()) {

                String currKey = infoInputKeys.next();

                if (currKey.equals(potentialKeyword)) {

                    return this.infoInput.get(currKey) instanceof JSONArray;

                }

            }

        } else {

            // check identified resources
            Iterator<String> identifiedResIterator = this.identifiedResources.keys();

            while (identifiedResIterator.hasNext()) {

                String currKey = identifiedResIterator.next();

                if (currKey.equals(potentialKeyword)) {
                    // get already identified resource from cache

                    return this.identifiedResources.get(currKey) instanceof JSONArray;

                }

            }

            // check info input
            Iterator<String> infoInputKeys = this.infoInput.keys();

            while (infoInputKeys.hasNext()) {

                String currKey = infoInputKeys.next();

                if (currKey.equals(potentialKeyword)) {

                    return this.infoInput.get(currKey) instanceof JSONArray;

                }

            }

        }

        // return object from input
        return false;

    }


    /**
     * This method checks if an input URI is a keyword
     * @param potentialKeyword contains an URI
     * @return the list of a keyword or the input URI
     */
    private JSONArray calculateValueListForKeyword(String potentialKeyword) {

        if (potentialKeyword.contains("__SPRO_")) {

            String localNamePropertyInObject = potentialKeyword.substring(potentialKeyword.indexOf("__") + 2);

            Iterator<String> keyIterator = this.generatedResources.keys();

            while (keyIterator.hasNext()) {

                String currKey = keyIterator.next();

                // get local name of a key
                String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                if (localNameOfKey.equals(localNamePropertyInObject)) {
                    // get ng from generated resources

                    if (this.mdbCoreIDNotEmpty
                            && this.mdbUEIDNotEmpty
                            && !this.mdbEntryIDNotEmpty) {


                    } else if (this.mdbEntryIDNotEmpty
                            && this.mdbUEIDNotEmpty) {

                        return this.generatedResources.getJSONArray(currKey);


                    } else if (this.mdbUEIDNotEmpty) {

                        return this.generatedResources.getJSONArray(currKey);

                    }

                }

            }

            // check identified resources
            Iterator<String> identifiedResIterator = this.identifiedResources.keys();

            while (identifiedResIterator.hasNext()) {

                String currKey = identifiedResIterator.next();

                if (currKey.equals(potentialKeyword)) {
                    // get already identified resource from cache

                    return this.identifiedResources.getJSONArray(currKey);

                }

            }

            // check info input
            Iterator<String> infoInputKeys = this.infoInput.keys();

            while (infoInputKeys.hasNext()) {

                String currKey = infoInputKeys.next();

                if (currKey.equals(potentialKeyword)) {

                    return this.infoInput.getJSONArray(currKey);

                }

            }

        } else {

            // check identified resources
            Iterator<String> identifiedResIterator = this.identifiedResources.keys();

            while (identifiedResIterator.hasNext()) {

                String currKey = identifiedResIterator.next();

                if (currKey.equals(potentialKeyword)) {
                    // get already identified resource from cache

                    return this.identifiedResources.getJSONArray(currKey);

                }

            }

            // check info input
            Iterator<String> infoInputKeys = this.infoInput.keys();

            while (infoInputKeys.hasNext()) {

                String currKey = infoInputKeys.next();

                if (currKey.equals(potentialKeyword)) {

                    return this.infoInput.getJSONArray(currKey);

                }

            }

        }

        System.out.println("WARN: There is no known JSONArray for the keyword: " + potentialKeyword);

        // return object from input
        return new JSONArray();

    }

    /**
     * This method calculate the named graph for a statement
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the URI of a named graph
     */
    private String calculateNG(JSONArray currExecStep, JSONObject currComponentObject, JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        // handle special case create overlay
        if (this.hasCreateOverlayInput) {

            return this.createOverlayNG;

        }

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {

                String ng = currExecStep.getJSONObject(i).get("object").toString();

                if (ng.contains("__SPRO_")) {

                    String localNamePropertyInObject = ng.substring(ng.indexOf("__") + 2);

                    Iterator<String> keyIterator = this.generatedResources.keys();

                    while (keyIterator.hasNext()) {

                        String currKey = keyIterator.next();

                        // get local name of a key
                        String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                        if (localNameOfKey.equals(localNamePropertyInObject)) {
                            // get ng from generated resources

                            if (this.mdbCoreIDNotEmpty
                                    && this.mdbUEIDNotEmpty
                                    && !this.mdbEntryIDNotEmpty) {


                            } else if (this.mdbEntryIDNotEmpty
                                    && this.mdbUEIDNotEmpty) {

                                return this.generatedResources.get(currKey).toString();


                            } else if (this.mdbUEIDNotEmpty) {

                                return this.generatedResources.get(currKey).toString();

                            }

                        }

                    }

                    // check identified resources
                    Iterator<String> identifiedResIterator = this.identifiedResources.keys();

                    while (identifiedResIterator.hasNext()) {

                        String currKey = identifiedResIterator.next();

                        if (currKey.equals(ng)) {
                            // get already identified resource from cache

                            if (EmailValidator.getInstance().isValid(this.identifiedResources.get(currKey).toString())) {
                                // convert mail to a complete uri

                                return "mailto:" + this.identifiedResources.get(currKey).toString();

                            }

                            return this.identifiedResources.get(currKey).toString();

                        }

                    }

                    // check info input
                    Iterator<String> infoInputKeys = this.infoInput.keys();

                    while (infoInputKeys.hasNext()) {

                        String currKey = infoInputKeys.next();

                        if (currKey.equals(ng)) {

                            return this.infoInput.get(currKey).toString();

                        }

                    }


                } else {

                    if (ng.equals(SprO.sproVARIABLEFindNamedGraph.toString())) {

                        if (this.previousCalculatedNG.isEmpty()) {

                            return "Error: There was no named graph calculated earlier in the transition.";

                        } else {
                            // create sparql query to find the named graph in combination of active_tab and this.previousCalculatedNG

                            String workspace = calculateWorkspaceDirectory(currExecStep);

                            SelectBuilder selectWhereBuilder = new SelectBuilder();

                            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                            selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                            if (this.useTab && jsonInputObject.get("value").toString().equals("show_localID")) {
                                // identify named graph with active tab

                                selectWhereBuilder.addWhere("<" + this.mdbEntryID + "#" + jsonInputObject.get("localID").toString() + ">", "<" + this.tabToUseURI + ">", "?o");

                            } else {

                                selectWhereBuilder.addWhere("<" + this.mdbEntryID + "#" + jsonInputObject.get("localID").toString() + ">", "?p", "?o");

                            }

                            SelectBuilder selectBuilder = new SelectBuilder();

                            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                            ExprVar exprVar = new ExprVar("o");

                            selectBuilder.addVar(exprVar);

                            selectBuilder.addGraph("<" + this.previousCalculatedNG + ">", selectWhereBuilder);

                            String sparqlQueryString = selectBuilder.buildString();

                            return connectionToTDB.pullSingleDataFromTDB(workspace, sparqlQueryString, "?o");

                        }

                    }

                    // check identified resources
                    Iterator<String> identifiedResIterator = this.identifiedResources.keys();

                    while (identifiedResIterator.hasNext()) {

                        String currKey = identifiedResIterator.next();

                        if (currKey.equals(ng)) {
                            // get already identified resource from cache

                            return this.identifiedResources.get(currKey).toString();

                        }

                    }

                    // check info input
                    Iterator<String> infoInputKeys = this.infoInput.keys();

                    while (infoInputKeys.hasNext()) {

                        String currKey = infoInputKeys.next();

                        if (currKey.equals(ng)) {

                            return this.infoInput.get(currKey).toString();

                        }

                    }

                    // return object from input
                    return currExecStep.getJSONObject(i).get("object").toString();

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())) {

                boolean useNGForALaterExecutionStep = false;

                if (currExecStep.getJSONObject(i).get("object").toString().equals(SCMDBMD.morphologicalDescriptionPartonomyNamedGraph.toString())) {

                    useNGForALaterExecutionStep = true;

                }

                if (this.mdbCoreIDNotEmpty
                        && this.mdbUEIDNotEmpty
                        && !this.mdbEntryIDNotEmpty) {


                } else if (this.mdbEntryIDNotEmpty
                        && this.mdbUEIDNotEmpty) {

                    for (int j = 0; j < currExecStep.length(); j++) {

                        if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.namedGraphBelongsToEntryID.toString())) {

                            IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                            String workspace = calculateWorkspaceDirectory(currExecStep);

                            return individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                        }

                    }

                    JSONArray objectsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("object_data");
                    // todo maybe advance this calculation for multiple instances of one class

                    // check if object already was generated in execution step 'copy and save triple statement(s)'
                    for (int j = 0; j < objectsInJSONArray.length(); j++) {

                        if (objectsInJSONArray.get(j).toString().equals(currExecStep.getJSONObject(i).get("object").toString())) {

                            if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(j).toString().equals(RDF.type.toString())
                                    && currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(j).toString().equals("s")) {

                                return currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(j).toString();

                            }

                        }

                    }

                    IndividualURI individualURI = new IndividualURI(this.mdbEntryID);

                    String workspace = calculateWorkspaceDirectory(currExecStep);

                    String calculatedNG = individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                    if (useNGForALaterExecutionStep) {

                        this.previousCalculatedNG = calculatedNG;

                    }

                    return calculatedNG;

                } else if (this.mdbUEIDNotEmpty) {

                }



            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                boolean useNGForALaterExecutionStep = false;

                if (currExecStep.getJSONObject(i).get("object").toString().equals(SCMDBMD.morphologicalDescriptionPartonomyNamedGraph.toString())) {

                    useNGForALaterExecutionStep = true;

                }

                if (this.mdbCoreIDNotEmpty
                        && this.mdbUEIDNotEmpty
                        && !this.mdbEntryIDNotEmpty) {


                } else if (this.mdbEntryIDNotEmpty
                        && this.mdbUEIDNotEmpty) {

                    IndividualURI individualURI;

                    if (this.hasExecutionStepFocus) {

                        individualURI = new IndividualURI(this.executionStepFocus);

                    } else {

                        individualURI = new IndividualURI(this.mdbEntryID);

                    }

                    String workspace = calculateWorkspaceDirectory(currExecStep);

                    String calculatedNG;

                    if (jsonInputObject.has("partID")) {

                        calculatedNG = individualURI.getThisURIForAnIndividualWithPartID(currExecStep.getJSONObject(i).get("object").toString(), jsonInputObject.get("partID").toString(), workspace, connectionToTDB);

                    } else {

                        calculatedNG = individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                    }

                    if (useNGForALaterExecutionStep) {

                        this.previousCalculatedNG = calculatedNG;

                    }

                    return calculatedNG;


                } else if (this.mdbUEIDNotEmpty) {

                    IndividualURI individualURI;

                    if (this.hasExecutionStepFocus) {

                        individualURI = new IndividualURI(this.executionStepFocus);

                    } else {

                        individualURI = new IndividualURI(this.mdbUEID);

                    }

                    String workspace = calculateWorkspaceDirectory(currExecStep);

                    String calculatedNG = individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                    if (useNGForALaterExecutionStep) {

                        this.previousCalculatedNG = calculatedNG;

                    }

                    return calculatedNG;

                }

            }

        }

        return null;

    }


    /**
     * This method calculates the named graph URI of a current execution step ('property','object')-key pair.
     * @param propertyURI contains the property URI of the current execution step
     * @param objectURI contains the object URI of the current execution step
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the URI of a named graph
     */
    private String calculateNGWithMultipleInput(String propertyURI, String objectURI, JSONArray currExecStep, JSONObject currComponentObject, JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB){

        JSONArray ngSpecificExecStep = new JSONArray();

        JSONObject ngSpecificExecStepObject = new JSONObject();

        ngSpecificExecStepObject.put("property", propertyURI);

        ngSpecificExecStepObject.put("object", objectURI);

        ngSpecificExecStep.put(ngSpecificExecStepObject);

        boolean copyFromWorkspace = false;

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.copyFromWorkspace.toString())) {

                ngSpecificExecStepObject = new JSONObject();

                ngSpecificExecStepObject.put("property", SprO.namedGraphBelongsToWorkspace.toString());

                ngSpecificExecStepObject.put("object", currExecStep.getJSONObject(i).get("object").toString());

                ngSpecificExecStep.put(ngSpecificExecStepObject);

                copyFromWorkspace = true;

            }

        }

        if (!copyFromWorkspace) {

            for (int i = 0; i < currExecStep.length(); i++) {

                if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                    ngSpecificExecStepObject = new JSONObject();

                    ngSpecificExecStepObject.put("property", currExecStep.getJSONObject(i).get("property").toString());

                    ngSpecificExecStepObject.put("object", currExecStep.getJSONObject(i).get("object").toString());

                    ngSpecificExecStep.put(ngSpecificExecStepObject);

                }

            }

        }

        return calculateNG(ngSpecificExecStep, currComponentObject, jsonInputObject, connectionToTDB);

    }


    /**
     * This method modifies a list of named graphs and maybe adds some new named graphs to the list of named graphs.
     * @param keywordOfNGList contains an URI of an already known keyword list
     * @param ngsJSONArray contains a list of named graphs
     * @return a list of named graphs
     */
    private JSONArray calculateNGListWithMultipleInput(String keywordOfNGList, JSONArray ngsJSONArray) {

        Iterator<String> identifiedResourcesIter = this.identifiedResources.keys();

        boolean keyWasFound = false;

        while (identifiedResourcesIter.hasNext() && !keyWasFound) {

            String currKey = identifiedResourcesIter.next();

            if (currKey.equals(keywordOfNGList)) {

                if (this.identifiedResources.get(keywordOfNGList) instanceof JSONArray) {

                    JSONArray ngListJSONArray = this.identifiedResources.getJSONArray(keywordOfNGList);

                    for (int i = 0; i < ngListJSONArray.length(); i++) {

                        ngsJSONArray.put(ngListJSONArray.get(i).toString());

                    }

                    keyWasFound = true;

                }

            }

        }

        return ngsJSONArray;

    }

    /**
     * This method calculate the property for a statement
     * @param currExecStep contains all information from the ontology for the current execution step
     * @return the specific uri of a property
     */
    private String calculateProperty(JSONArray currExecStep) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.propertySOCCOMAS.toString())) {
                // property

                String property = currExecStep.getJSONObject(i).get("object").toString();

                if (this.infoInput.has(property)) {

                    return this.infoInput.get(property).toString();

                }

                return currExecStep.getJSONObject(i).get("object").toString();

            }

        }

        System.out.println("Error: Can't calculate property.");

        return "Error: Can't calculate property.";

    }


    /**
     * This method calculate the object for a property
     * @param dataToFindObjectInTDB contains information to find a potential object in a jena tdb
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param objectType contains "a", "l" or "r"
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the specific value of the object resource
     */
    private String calculateObject(JSONObject dataToFindObjectInTDB, JSONArray currExecStep, JSONObject currComponentObject, JSONObject jsonInputObject, String objectType, JenaIOTDBFactory connectionToTDB) {

        switch (objectType) {

            case "a" :

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())) {

                        String object = currExecStep.getJSONObject(i).get("object").toString();

                        if (object.equals(SprO.sproVARIABLEThisEntryID.toString())) {

                            return hasExecutionStepFocus ? this.executionStepFocus : this.mdbEntryID;

                        } else if (object.equals(SprO.sproVARIABLEThisUserID.toString())) {

                            // the user ID is the combination of the ueid and the local identifier SCBasic.user
                            return this.mdbUEID + "#" + SCBasic.user.getLocalName() + "_1";

                        } else if (object.equals(SprO.sproVARIABLEThisCoreID.toString())) {

                            return this.mdbCoreID;

                        } else if (object.equals(SprO.sproVARIABLEQuestionMark.toString())) {

                            SelectBuilder selectBuilder = new SelectBuilder();

                            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                            SelectBuilder tripleSPO = new SelectBuilder();

                            String subject = "?s";

                            if (dataToFindObjectInTDB.has("subject")) {

                                subject = "<" + dataToFindObjectInTDB.get("subject").toString() + ">";

                            }

                            String property = "?p";

                            if (dataToFindObjectInTDB.has("property")) {

                                property = "<" + dataToFindObjectInTDB.get("property").toString() + ">";

                            }

                            tripleSPO.addWhere(subject, property, "?o");

                            selectBuilder.addVar(selectBuilder.makeVar("?o"));

                            String ng = "?g";

                            if (dataToFindObjectInTDB.has("ng")) {

                                ng = "<" + dataToFindObjectInTDB.get("ng").toString() + ">";

                            }

                            selectBuilder.addGraph(ng, tripleSPO);

                            String sparqlQueryString = selectBuilder.buildString();

                            return connectionToTDB.pullSingleDataFromTDB(dataToFindObjectInTDB.get("directory").toString(), sparqlQueryString, "?o");

                        } else if (object.equals(SprO.sproVARIABLEThisUserEntryID.toString())) {

                            return this.mdbUEID;

                        } else if (object.equals(SprO.sproVARIABLEMaxPosition1.toString())) {
                            // SPrO_VARIABLE: max position +1

                            if (this.newPositionCompositionPart.isEmpty()) {

                                SelectBuilder selectBuilder = new SelectBuilder();

                                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                                SelectBuilder tripleSPO = new SelectBuilder();

                                ExprVar exprVar = new ExprVar("o");

                                Aggregator aggregator = AggregatorFactory.createCountExpr(true, exprVar.getExpr());

                                ExprAggregator exprAggregator = new ExprAggregator(exprVar.asVar(), aggregator);

                                selectBuilder.addVar(exprAggregator.getExpr(), "?count");

                                String property = "?p";

                                if (dataToFindObjectInTDB.has("property")) {

                                    property = "<" + dataToFindObjectInTDB.get("property").toString() + ">";

                                }

                                tripleSPO.addWhere("?s", property, "?o");

                                for (int j = 0; j < currComponentObject.length(); j++) {

                                    if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())
                                            || currExecStep.getJSONObject(j).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {

                                        tripleSPO.addWhere("?s", RDF.type, "<" + currExecStep.getJSONObject(j).get("object").toString() + ">");

                                    }

                                }

                                String ng = "?g";

                                if (dataToFindObjectInTDB.has("ng")) {

                                    ng = "<" + dataToFindObjectInTDB.get("ng").toString() + ">";

                                }

                                selectBuilder.addGraph(ng, tripleSPO);

                                String sparqlQueryString = selectBuilder.buildString();

                                String numberOfPositions = connectionToTDB.pullSingleDataFromTDB(dataToFindObjectInTDB.get("directory").toString(), sparqlQueryString, "?count");

                                this.newPositionCompositionPart = numberOfPositions.isEmpty() ? "1" : String.valueOf(Integer.parseInt(numberOfPositions) + 1);

                            }

                            return this.newPositionCompositionPart;

                        } else if (object.equals(SprO.sproVARIABLEEntryCurrentlyInFocus.toString())) {

                            return this.currentFocus;

                        } else if (object.equals(SprO.sproVARIABLEKnownResourceA.toString())) {

                            boolean hasConstraint = false;
                            String constraint = "";

                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.hasConstraint.toString())) {

                                    constraint = currExecStep.getJSONObject(j).get("object").toString();
                                    hasConstraint = true;

                                }

                            }

                            if (hasConstraint) {

                                if (constraint.equals(SprO.sproVARIABLEThisCookie.toString())) {

                                    return jsonInputObject.get(SprO.sproVARIABLEKnownResourceA.toString()).toString();

                                }

                            }

                        } else if (object.equals(SprO.sproVARIABLEKnownResourceB.toString())) {

                            boolean hasConstraint = false;
                            String constraint = "";

                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.hasConstraint.toString())) {

                                    constraint = currExecStep.getJSONObject(j).get("object").toString();
                                    hasConstraint = true;

                                }

                            }

                            if (hasConstraint) {

                                if (constraint.equals(SprO.sproVARIABLEThisCookie.toString())) {

                                    return jsonInputObject.get(SprO.sproVARIABLEKnownResourceB.toString()).toString();

                                }

                            }

                        } else if (object.equals(SprO.sproVARIABLEEntryTypeIconCorrespondingToInputOfTypeInfoInput1.toString())) {
                            // todo recalculate this part when the icon has an uri

                            return object;

                        } else if (object.equals(SprO.sproVARIABLEThisEntryVersionNumber.toString())) {

                            return this.currentFocus.substring((this.currentFocus.lastIndexOf("-") + 1));

                        } else if (object.contains(SprO.identifiedWorkflowAction.getLocalName())) {

                            return jsonInputObject.get(SprO.identifiedWorkflowAction.getLocalName()).toString();

                        } else {

                            // check identified resources
                            Iterator<String> identifiedResIterator = this.identifiedResources.keys();

                            while (identifiedResIterator.hasNext()) {

                                String currKey = identifiedResIterator.next();

                                if (currKey.equals(object)) {
                                    // get already identified resource from cache

                                    return this.identifiedResources.get(currKey).toString();

                                }

                            }

                            // check info input
                            Iterator<String> infoInputKeys = this.infoInput.keys();

                            while (infoInputKeys.hasNext()) {

                                String currKey = infoInputKeys.next();

                                if (currKey.equals(object)) {

                                    return this.infoInput.get(currKey).toString();

                                }

                            }

                            if (object.contains("__SPRO_")) {

                                String localNamePropertyInObject = object.substring(object.indexOf("__") + 2);

                                Iterator<String> genResIterator = this.generatedResources.keys();

                                while (genResIterator.hasNext()) {

                                    String currKey = genResIterator.next();

                                    // get local name of a key
                                    String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                                    if (localNameOfKey.equals(localNamePropertyInObject)) {
                                        // get already generated resource from cache

                                        return this.generatedResources.get(currKey).toString();

                                    }

                                }

                            }

                            if (jsonInputObject.has("localIDs")) {

                                JSONArray currJSONArray = jsonInputObject.getJSONArray("localIDs");

                                for (int j = 0; j < currJSONArray.length(); j++) {

                                    JSONObject currJSONObject = currJSONArray.getJSONObject(j);

                                    if (currJSONObject.has("keyword")) {

                                        if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keyword").toString()) &&
                                                jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                            if (EmailValidator.getInstance().isValid(currJSONObject.get("value").toString())) {

                                                return "mailto:" + currJSONObject.get("value").toString();

                                            } else {

                                                return currJSONObject.get("value").toString();

                                            }

                                        } else if (currJSONObject.has("keywordLabel")) {

                                            if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keywordLabel").toString()) &&
                                                    jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                                if (EmailValidator.getInstance().isValid(currJSONObject.get("valueLabel").toString())) {

                                                    return "mailto:" + currJSONObject.get("valueLabel").toString();

                                                } else {

                                                    return currJSONObject.get("valueLabel").toString();

                                                }

                                            }

                                        } else if (currJSONObject.has("keywordDefinition")) {

                                            if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keywordDefinition").toString()) &&
                                                    jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                                if (EmailValidator.getInstance().isValid(currJSONObject.get("valueDefinition").toString())) {

                                                    return "mailto:" + currJSONObject.get("valueDefinition").toString();

                                                } else {

                                                    return currJSONObject.get("valueDefinition").toString();

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                            return object;

                        }

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectCopiedIndividualOf.toString())) {

                        if (this.mdbCoreIDNotEmpty
                                && this.mdbUEIDNotEmpty
                                && !this.mdbEntryIDNotEmpty) {


                        } else if (this.mdbEntryIDNotEmpty
                                && this.mdbUEIDNotEmpty) {

                            // check if object already exist in another workspace
                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.objectBelongsToWorkspace.toString())) {

                                    IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                                    String workspace = calculateWorkspaceDirectory(currExecStep);

                                    return individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                                }

                            }

                            JSONArray objectsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("object_data");
                            // todo maybe advance this calculation for multiple instances of one class

                            // check if object already was generated in execution step 'copy and save triple statement(s)'
                            for (int j = 0; j < objectsInJSONArray.length(); j++) {

                                if (objectsInJSONArray.get(j).toString().equals(currExecStep.getJSONObject(i).get("object").toString())) {

                                    if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(j).toString().equals(RDF.type.toString())
                                            && currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(j).toString().equals("s")) {

                                        return currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(j).toString();

                                    }

                                }

                            }

                        } else if (this.mdbUEIDNotEmpty) {

                            JSONArray objectsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("object_data");

                            for (int j = 0; j < objectsInJSONArray.length(); j++) {

                                if (objectsInJSONArray.get(j).toString().equals(currExecStep.getJSONObject(i).get("object").toString())) {

                                    if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(j).toString().equals(RDF.type.toString())
                                            && currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(j).toString().equals("s")) {

                                        return currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(j).toString();

                                    }

                                }

                            }

                        }


                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectThisEntrySSpecificIndividualOf.toString())) {

                        for (int j = 0; j < currExecStep.length(); j++) {

                            if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                                // input contains for example this.mdbUEID
                                IndividualURI individualURI = new IndividualURI(this.executionStepFocus);

                                String workspace = calculateWorkspaceDirectory(currExecStep);

                                return individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                            }

                        }

                        IndividualURI individualURI = new IndividualURI(this.currentFocus);

                        String workspace = calculateWorkspaceDirectory(currExecStep);

                        return individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);


                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectUniqueIndividualOf.toString())) {

                        String correspondingNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        String correspondingDirectory = calculateWorkspaceDirectory(currExecStep);

                        SelectBuilder selectWhereBuilder = new SelectBuilder();

                        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                        selectWhereBuilder.addWhere("?s", RDF.type, "<" + currExecStep.getJSONObject(i).get("object").toString() + ">");

                        SelectBuilder selectBuilder = new SelectBuilder();

                        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                        selectBuilder.addVar("?s");

                        selectBuilder.addGraph("<" + correspondingNG + ">", selectWhereBuilder);

                        String sparqlQueryString = selectBuilder.buildString();

                        return connectionToTDB.pullSingleDataFromTDB(correspondingDirectory, sparqlQueryString, "?s");

                    }

                }

                break;

            case "l" :

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())) {

                        String object = currExecStep.getJSONObject(i).get("object").toString();

                        if (object.equals(SprO.sproVARIABLEQuestionMark.toString())) {

                            SelectBuilder selectBuilder = new SelectBuilder();

                            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                            SelectBuilder tripleSPO = new SelectBuilder();

                            String subject = "?s";

                            if (dataToFindObjectInTDB.has("subject")) {

                                subject = "<" + dataToFindObjectInTDB.get("subject").toString() + ">";

                            }

                            String property = "?p";

                            if (dataToFindObjectInTDB.has("property")) {

                                property = "<" + dataToFindObjectInTDB.get("property").toString() + ">";

                            }

                            tripleSPO.addWhere(subject, property, "?o");

                            selectBuilder.addVar(selectBuilder.makeVar("?o"));

                            String ng = "?g";

                            if (dataToFindObjectInTDB.has("ng")) {

                                ng = "<" + dataToFindObjectInTDB.get("ng").toString() + ">";

                            }

                            selectBuilder.addGraph(ng, tripleSPO);

                            String sparqlQueryString = selectBuilder.buildString();

                            return connectionToTDB.pullSingleDataFromTDB(dataToFindObjectInTDB.get("directory").toString(), sparqlQueryString, "?o");

                        } else if (object.equals(SprO.sproVARIABLEThisUserID.toString())) {

                            // the user ID is the combination of the ueid and the local identifier SCBasic.user
                            return this.mdbUEID + "#" + SCBasic.user.getLocalName() + "_1";

                        } else if (object.equals(SprO.sproVARIABLEThisCoreID.toString())) {

                            return this.mdbCoreID;

                        } else if (object.equals(SprO.sproVARIABLEThisUserEntryID.toString())) {

                            return this.mdbUEID;

                        } else if (object.equals(SprO.sproVARIABLEMaxPosition1.toString())) {
                            // SPrO_VARIABLE: max position +1

                            if (this.newPositionCompositionPart.isEmpty()) {

                                SelectBuilder selectBuilder = new SelectBuilder();

                                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                                SelectBuilder tripleSPO = new SelectBuilder();

                                ExprVar exprVar = new ExprVar("o");

                                Aggregator aggregator = AggregatorFactory.createCountExpr(true, exprVar.getExpr());

                                ExprAggregator exprAggregator = new ExprAggregator(exprVar.asVar(), aggregator);

                                selectBuilder.addVar(exprAggregator.getExpr(), "?count");

                                String property = "?p";

                                if (dataToFindObjectInTDB.has("property")) {

                                    property = "<" + dataToFindObjectInTDB.get("property").toString() + ">";

                                }

                                tripleSPO.addWhere("?s", property, "?o");

                                for (int j = 0; j < currComponentObject.length(); j++) {

                                    if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())
                                            || currExecStep.getJSONObject(j).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {

                                        tripleSPO.addWhere("?s", RDF.type, "<" + currExecStep.getJSONObject(j).get("object").toString() + ">");

                                    }

                                }

                                String ng = "?g";

                                if (dataToFindObjectInTDB.has("ng")) {

                                    ng = "<" + dataToFindObjectInTDB.get("ng").toString() + ">";

                                }

                                selectBuilder.addGraph(ng, tripleSPO);

                                String sparqlQueryString = selectBuilder.buildString();

                                String numberOfPositions = connectionToTDB.pullSingleDataFromTDB(dataToFindObjectInTDB.get("directory").toString(), sparqlQueryString, "?count");

                                this.newPositionCompositionPart = numberOfPositions.isEmpty() ? "1" : String.valueOf(Integer.parseInt(numberOfPositions) + 1);

                            }

                            return this.newPositionCompositionPart;

                        } else if (object.equals(SprO.iNPUTCONTROLDateTimeStamp.toString())) {

                            return this.soccomasDat.getDate();

                        } else if (object.equals(SprO.sproVARIABLENameOfThisUserID.toString())) {

                            TDBPath tdbPath = new TDBPath();

                            String firstName = getLiteralFromStore(this.mdbUEID, (FOAF.firstName).toString(), tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), connectionToTDB);

                            String lastName = getLiteralFromStore(this.mdbUEID, (FOAFAdvanced.lastName).toString(), tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), connectionToTDB);

                            return firstName + " " + lastName;

                        } else if (object.equals(SprO.sproVARIABLEThisEntryVersionNumber.toString())) {

                            return this.currentFocus.substring((this.currentFocus.lastIndexOf("-") + 1));

                        } else if ( object.equals("true") ||
                                object.equals("false")) {

                            return object;

                        } else if (object.contains(SprO.identifiedWorkflowAction.getLocalName())) {

                            return jsonInputObject.get(SprO.identifiedWorkflowAction.getLocalName()).toString();

                        } else {
                            // check identified resources

                            //System.out.println("object = " + currExecStep.getJSONObject(i).get("object").toString());

                            Iterator<String> identifiedResIterator = this.identifiedResources.keys();

                            while (identifiedResIterator.hasNext()) {

                                String currKey = identifiedResIterator.next();

                                if (currKey.equals(object)) {
                                    // get already identified resource from cache

                                    return this.identifiedResources.get(currKey).toString();

                                }

                            }

                            // check info input
                            Iterator<String> infoInputKeys = this.infoInput.keys();

                            while (infoInputKeys.hasNext()) {

                                String currKey = infoInputKeys.next();

                                if (currKey.equals(object)) {

                                    return this.infoInput.get(currKey).toString();

                                }

                            }

                            if (object.contains("__SPRO_")) {

                                String localNamePropertyInObject = object.substring(object.indexOf("__") + 2);

                                Iterator<String> genResIterator = this.generatedResources.keys();

                                while (genResIterator.hasNext()) {

                                    String currKey = genResIterator.next();

                                    // get local name of a key
                                    String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                                    if (localNameOfKey.equals(localNamePropertyInObject)) {
                                        // get already generated resource from cache

                                        return this.generatedResources.get(currKey).toString();

                                    }

                                }

                                if (jsonInputObject.has("localIDs")) {

                                    JSONArray currJSONArray = jsonInputObject.getJSONArray("localIDs");

                                    for (int j = 0; j < currJSONArray.length(); j++) {

                                        JSONObject currJSONObject = currJSONArray.getJSONObject(j);

                                        if (currJSONObject.has("keyword")) {

                                            if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keyword").toString()) &&
                                                    jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                                return currJSONObject.get("value").toString();

                                            } else if (currJSONObject.has("keywordLabel")) {

                                                if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keywordLabel").toString()) &&
                                                        jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                                    return currJSONObject.get("valueLabel").toString();

                                                }

                                            } else if (currJSONObject.has("keywordDefinition")) {

                                                if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keywordDefinition").toString()) &&
                                                        jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                                    return currJSONObject.get("valueDefinition").toString();

                                                }

                                            }

                                        }

                                    }

                                }

                            } else if (object.equals(SprO.sproVARIABLEEntryCurrentlyInFocus.toString())) {

                                return this.currentFocus;

                            } else if (object.equals(SprO.sproVARIABLEKnownResourceA.toString())) {

                                boolean hasConstraint = false;
                                String constraint = "";

                                for (int j = 0; j < currExecStep.length(); j++) {

                                    if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.hasConstraint.toString())) {

                                        constraint = currExecStep.getJSONObject(j).get("object").toString();
                                        hasConstraint = true;

                                    }

                                }

                                if (hasConstraint) {

                                    if (constraint.equals(SprO.sproVARIABLEThisCookie.toString())) {

                                        return jsonInputObject.get(SprO.sproVARIABLEKnownResourceA.toString()).toString();

                                    }

                                }

                            } else if (object.equals(SprO.sproVARIABLEKnownResourceB.toString())) {

                                boolean hasConstraint = false;
                                String constraint = "";

                                for (int j = 0; j < currExecStep.length(); j++) {

                                    if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.hasConstraint.toString())) {

                                        constraint = currExecStep.getJSONObject(j).get("object").toString();
                                        hasConstraint = true;

                                    }

                                }

                                if (hasConstraint) {

                                    if (constraint.equals(SprO.sproVARIABLEThisCookie.toString())) {

                                        return jsonInputObject.get(SprO.sproVARIABLEKnownResourceB.toString()).toString();

                                    }

                                }

                            } else if (object.equals(SprO.sproVARIABLECalculatePositionInPartonomy.toString())) {

                                object = calculatePositionInPartonomy(dataToFindObjectInTDB, currExecStep, connectionToTDB);

                                return object;

                            }

                            return object;

                        }

                    }

                }

                break;

            case "r" :

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())) {

                        String object = currExecStep.getJSONObject(i).get("object").toString();

                        if (object.equals(SprO.sproVARIABLEQuestionMark.toString())) {

                            SelectBuilder selectBuilder = new SelectBuilder();

                            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                            SelectBuilder tripleSPO = new SelectBuilder();

                            String subject = "?s";

                            if (dataToFindObjectInTDB.has("subject")) {

                                subject = "<" + dataToFindObjectInTDB.get("subject").toString() + ">";

                            }

                            String property = "?p";

                            if (dataToFindObjectInTDB.has("property")) {

                                property = "<" + dataToFindObjectInTDB.get("property").toString() + ">";

                            }

                            tripleSPO.addWhere(subject, property, "?o");

                            selectBuilder.addVar(selectBuilder.makeVar("?o"));

                            String ng = "?g";

                            if (dataToFindObjectInTDB.has("ng")) {

                                ng = "<" + dataToFindObjectInTDB.get("ng").toString() + ">";

                            }

                            selectBuilder.addGraph(ng, tripleSPO);

                            String sparqlQueryString = selectBuilder.buildString();

                            String queryResult = connectionToTDB.pullSingleDataFromTDB(dataToFindObjectInTDB.get("directory").toString(), sparqlQueryString, "?o");

                            // return 'SPrO_VARIABLE: empty' - URI (if result is empty) or the URI from the jena tdb
                            return queryResult.equals("") ? SprO.sproVARIABLEEmpty.toString() : queryResult;

                        } else if (object.equals(SprO.sproVARIABLEThisEntryID.toString())) {

                            return hasExecutionStepFocus ? this.executionStepFocus : this.mdbEntryID;

                        } else if (object.equals(SprO.sproVARIABLEThisUserID.toString())) {

                            // the user ID is the combination of the ueid and the local identifier SCBasic.user
                            return this.mdbUEID + "#" + SCBasic.user.getLocalName() + "_1";

                        } else if (object.equals(SprO.sproVARIABLEThisCoreID.toString())) {

                            return this.mdbCoreID;

                        } else if (object.equals(SprO.sproVARIABLEMaxPosition1.toString())) {
                            // SPrO_VARIABLE: max position +1

                            if (this.newPositionCompositionPart.isEmpty()) {

                                SelectBuilder selectBuilder = new SelectBuilder();

                                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                                SelectBuilder tripleSPO = new SelectBuilder();

                                ExprVar exprVar = new ExprVar("o");

                                Aggregator aggregator = AggregatorFactory.createCountExpr(true, exprVar.getExpr());

                                ExprAggregator exprAggregator = new ExprAggregator(exprVar.asVar(), aggregator);

                                selectBuilder.addVar(exprAggregator.getExpr(), "?count");

                                String property = "?p";

                                if (dataToFindObjectInTDB.has("property")) {

                                    property = "<" + dataToFindObjectInTDB.get("property").toString() + ">";

                                }

                                tripleSPO.addWhere("?s", property, "?o");

                                for (int j = 0; j < currComponentObject.length(); j++) {

                                    if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())
                                            || currExecStep.getJSONObject(j).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {

                                        tripleSPO.addWhere("?s", RDF.type, "<" + currExecStep.getJSONObject(j).get("object").toString() + ">");

                                    }

                                }

                                String ng = "?g";

                                if (dataToFindObjectInTDB.has("ng")) {

                                    ng = "<" + dataToFindObjectInTDB.get("ng").toString() + ">";

                                }

                                selectBuilder.addGraph(ng, tripleSPO);

                                String sparqlQueryString = selectBuilder.buildString();

                                String numberOfPositions = connectionToTDB.pullSingleDataFromTDB(dataToFindObjectInTDB.get("directory").toString(), sparqlQueryString, "?count");

                                this.newPositionCompositionPart = numberOfPositions.isEmpty() ? "1" : String.valueOf(Integer.parseInt(numberOfPositions) + 1);

                            }

                            return this.newPositionCompositionPart;

                        } else if (object.equals(SprO.sproVARIABLEThisUserEntryID.toString())) {

                            return this.mdbUEID;

                        } else if (object.equals(SprO.sproVARIABLEThisEntryComponent.toString())) {

                            if (jsonInputObject.has("precedingKeywords")) {

                                return jsonInputObject.get("mdbentryid").toString() + "#" + jsonInputObject.get("localID").toString();

                            } else {

                                JSONObject jsonFromMongoDB = this.mongoDBConnection.pullDataFromMongoDBWithLocalID(jsonInputObject);

                                return jsonFromMongoDB.get("individualID").toString();

                            }

                        } else if (object.equals(SprO.sproVARIABLEEntryCurrentlyInFocus.toString())) {

                            return this.currentFocus;

                        } else if (object.equals(SprO.sproVARIABLEKnownResourceA.toString())) {

                            boolean hasConstraint = false;
                            String constraint = "";

                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.hasConstraint.toString())) {

                                    constraint = currExecStep.getJSONObject(j).get("object").toString();
                                    hasConstraint = true;

                                }

                            }

                            if (hasConstraint) {

                                if (constraint.equals(SprO.sproVARIABLEThisCookie.toString())) {

                                    return jsonInputObject.get(SprO.sproVARIABLEKnownResourceA.toString()).toString();

                                }

                            }

                        } else if (object.equals(SprO.sproVARIABLEKnownResourceB.toString())) {

                            boolean hasConstraint = false;
                            String constraint = "";

                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.hasConstraint.toString())) {

                                    constraint = currExecStep.getJSONObject(j).get("object").toString();
                                    hasConstraint = true;

                                }

                            }

                            if (hasConstraint) {

                                if (constraint.equals(SprO.sproVARIABLEThisCookie.toString())) {

                                    return jsonInputObject.get(SprO.sproVARIABLEKnownResourceB.toString()).toString();

                                }

                            }

                        } else if (object.equals(SprO.sproVARIABLEResourceDescribedWithThisDescriptionFormComposition.toString())) {

                            if (jsonInputObject.has("partID")) {

                                return jsonInputObject.get("mdbentryid").toString() + "#" + jsonInputObject.get("partID").toString();

                            } else {

                                System.out.println("WARN: There is no partID in Input!");

                            }

                        } else if (object.equals(SprO.sproVARIABLEThisEntryVersionNumber.toString())) {

                            return this.currentFocus.substring((this.currentFocus.lastIndexOf("-") + 1));

                        } else if (object.contains(SprO.identifiedWorkflowAction.getLocalName())) {

                            return jsonInputObject.get(SprO.identifiedWorkflowAction.getLocalName()).toString();

                        } else if (object.contains("__SPRO_")) {

                            String localNamePropertyInObject = object.substring(object.indexOf("__") + 2);

                            Iterator<String> genResIterator = this.generatedResources.keys();

                            while (genResIterator.hasNext()) {

                                String currKey = genResIterator.next();

                                // get local name of a key
                                String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                                if (localNameOfKey.equals(localNamePropertyInObject)) {
                                    // get already generated resource from cache

                                    return this.generatedResources.get(currKey).toString();

                                }

                            }

                            if (jsonInputObject.has("localIDs")) {

                                JSONArray currJSONArray = jsonInputObject.getJSONArray("localIDs");

                                for (int j = 0; j < currJSONArray.length(); j++) {

                                    JSONObject currJSONObject = currJSONArray.getJSONObject(j);

                                    if (currJSONObject.has("keyword")) {

                                        if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keyword").toString()) &&
                                                jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                            if (EmailValidator.getInstance().isValid(currJSONObject.get("value").toString())) {

                                                return "mailto:" + currJSONObject.get("value").toString();

                                            } else {

                                                return currJSONObject.get("value").toString();

                                            }

                                        } else if (currJSONObject.has("keywordLabel")) {

                                            if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keywordLabel").toString()) &&
                                                    jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                                if (EmailValidator.getInstance().isValid(currJSONObject.get("valueLabel").toString())) {

                                                    return "mailto:" + currJSONObject.get("valueLabel").toString();

                                                } else {

                                                    return currJSONObject.get("valueLabel").toString();

                                                }

                                            }

                                        } else if (currJSONObject.has("keywordDefinition")) {

                                            if (ResourceFactory.createResource(object).getLocalName().equals(currJSONObject.get("keywordDefinition").toString()) &&
                                                    jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                                if (EmailValidator.getInstance().isValid(currJSONObject.get("valueDefinition").toString())) {

                                                    return "mailto:" + currJSONObject.get("valueDefinition").toString();

                                                } else {

                                                    return currJSONObject.get("valueDefinition").toString();

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                        // check identified resources
                        Iterator<String> identifiedResIterator = this.identifiedResources.keys();

                        while (identifiedResIterator.hasNext()) {

                            String currKey = identifiedResIterator.next();

                            if (currKey.equals(object)) {
                                // get already identified resource from cache

                                if (EmailValidator.getInstance().isValid(this.identifiedResources.get(currKey).toString())) {
                                    // convert mail to a complete uri

                                    return "mailto:" + this.identifiedResources.get(currKey).toString();

                                }

                                return this.identifiedResources.get(currKey).toString();

                            }

                        }

                        // check info input
                        Iterator<String> infoInputKeys = this.infoInput.keys();

                        while (infoInputKeys.hasNext()) {

                            String currKey = infoInputKeys.next();

                            if (currKey.equals(object)) {

                                return this.infoInput.get(currKey).toString();

                            }

                        }

                        return object;

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectCopiedIndividualOf.toString())) {

                        System.out.println("here1");

                        String focusForNewIndividualOfClass = "";

                        if (this.mdbCoreIDNotEmpty
                                && this.mdbUEIDNotEmpty
                                && !this.mdbEntryIDNotEmpty) {


                        } else if (this.mdbEntryIDNotEmpty
                                && this.mdbUEIDNotEmpty) {

                            System.out.println("here2");

                            // check if object already exist in another workspace
                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.objectBelongsToWorkspace.toString())) {

                                    IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                                    String workspace = calculateWorkspaceDirectory(currExecStep);

                                    return individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                                }

                            }

                            System.out.println("here3");

                            JSONArray objectsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("object_data");

                            // check if object already was generated in execution step 'copy and save triple statement(s)'
                            for (int j = 0; j < objectsInJSONArray.length(); j++) {

                                if (objectsInJSONArray.get(j).toString().equals(currExecStep.getJSONObject(i).get("object").toString())) {

                                    if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(j).toString().equals(RDF.type.toString())
                                            && currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(j).toString().equals("s")) {

                                        System.out.println("here4");

                                        return currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(j).toString();

                                    }

                                }

                            }

                            focusForNewIndividualOfClass = this.mdbEntryID;

                        } else if (this.mdbUEIDNotEmpty) {

                            JSONArray objectsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("object_data");

                            for (int j = 0; j < objectsInJSONArray.length(); j++) {

                                if (objectsInJSONArray.get(j).toString().equals(currExecStep.getJSONObject(i).get("object").toString())) {

                                    if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(j).toString().equals(RDF.type.toString())
                                            && currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(j).toString().equals("s")) {

                                        return currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(j).toString();

                                    }

                                }

                            }

                            focusForNewIndividualOfClass = this.mdbUEID;

                        }

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectThisEntrySSpecificIndividualOf.toString())) {

                        for (int j = 0; j < currExecStep.length(); j++) {

                            if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {
                                // set new focus on MDB entry ID for this execution step

                                // input contains for example this.mdbUEID
                                IndividualURI individualURI = new IndividualURI(this.executionStepFocus);

                                String workspace = calculateWorkspaceDirectory(currExecStep);

                                return individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                            }

                        }

                        IndividualURI individualURI = new IndividualURI(this.currentFocus);

                        String workspace = calculateWorkspaceDirectory(currExecStep);

                        return individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectUniqueIndividualOf.toString())) {

                        String correspondingNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        String correspondingDirectory = calculateWorkspaceDirectory(currExecStep);

                        SelectBuilder selectWhereBuilder = new SelectBuilder();

                        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                        selectWhereBuilder.addWhere("?s", RDF.type, "<" + currExecStep.getJSONObject(i).get("object").toString() + ">");

                        SelectBuilder selectBuilder = new SelectBuilder();

                        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                        selectBuilder.addVar("?s");

                        selectBuilder.addGraph("<" + correspondingNG + ">", selectWhereBuilder);

                        String sparqlQueryString = selectBuilder.buildString();

                        return connectionToTDB.pullSingleDataFromTDB(correspondingDirectory, sparqlQueryString, "?s");

                    }

                }

                break;

        }

        System.out.println();
        System.out.println();
        System.out.println("Error: jsonInputObject = " + jsonInputObject);
        System.out.println();
        System.out.println();
        System.out.println("Error: currExecStep = " + currExecStep);
        return "Error: Can't calculate object.";

    }


    /**
     * This method calculate a list of objects.
     * @param dataToFindObjectInTDB contains information to find a potential object in a jena tdb
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the specific value of the object resource
     */
    private JSONArray calculateObjectList(JSONObject dataToFindObjectInTDB, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        SelectBuilder tripleSPO = new SelectBuilder();

        String subject = "?s";

        if (dataToFindObjectInTDB.has("subject")
                && !subject.equals(dataToFindObjectInTDB.get("subject").toString())) {

            subject = "<" + dataToFindObjectInTDB.get("subject").toString() + ">";

        }

        String property = "?p";

        if (dataToFindObjectInTDB.has("property")
                && !property.equals(dataToFindObjectInTDB.get("property").toString())) {

            property = "<" + dataToFindObjectInTDB.get("property").toString() + ">";

        }

        tripleSPO.addWhere(subject, property, "?o");

        selectBuilder.addVar(selectBuilder.makeVar("?o"));

        String ng = "?g";

        if (dataToFindObjectInTDB.has("ng")
                && !ng.equals(dataToFindObjectInTDB.get("ng").toString())) {

            ng = "<" + dataToFindObjectInTDB.get("ng").toString() + ">";

        }

        selectBuilder.addGraph(ng, tripleSPO);

        String sparqlQueryString = selectBuilder.buildString();

        return connectionToTDB.pullMultipleDataFromTDB(dataToFindObjectInTDB.get("directory").toString(), sparqlQueryString, "?o");

    }

    /**
     * This method calculate a list of subjects.
     * @param dataToFindSubjectInTDB contains information to find a potential subject in a jena tdb
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a list of subject resource(s)
     */
    private JSONArray calculateSubjectList(JSONObject dataToFindSubjectInTDB, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        SelectBuilder tripleSPO = new SelectBuilder();

        String object = "?o";

        if (dataToFindSubjectInTDB.has("object")
                && !object.equals(dataToFindSubjectInTDB.get("object").toString())) {

            object = "<" + dataToFindSubjectInTDB.get("object").toString() + ">";

        }

        String property = "?p";

        if (dataToFindSubjectInTDB.has("property")
                && !property.equals(dataToFindSubjectInTDB.get("property").toString())) {

            property = "<" + dataToFindSubjectInTDB.get("property").toString() + ">";

        }

        tripleSPO.addWhere("?s", property, object);

        selectBuilder.addVar(selectBuilder.makeVar("?s"));

        String ng = "?g";

        if (dataToFindSubjectInTDB.has("ng")
                && !ng.equals(dataToFindSubjectInTDB.get("ng").toString())) {

            ng = "<" + dataToFindSubjectInTDB.get("ng").toString() + ">";

        }

        selectBuilder.addGraph(ng, tripleSPO);

        String sparqlQueryString = selectBuilder.buildString();

        return connectionToTDB.pullMultipleDataFromTDB(dataToFindSubjectInTDB.get("directory").toString(), sparqlQueryString, "?s");

    }


    /**
     * This method calculate the object type for a property
     * @param property contains the input for the calculation of the type
     * @return "a", "l" or "r" according to the input property
     */
    private String calculateObjectType(String property) {

        boolean objectPropertyCheck = this.objectPropertySet.objectPropertyExist(this.objectPropertiesInJSON, property);

        if (objectPropertyCheck ||
                property.equals((RDF.type).toString()) ||
                property.equals((RDFS.subClassOf).toString())) {
            // object is a resource

            return "r";

        } else {

            boolean dataPropertyCheck = this.dataPropertySet.dataPropertyExist(this.dataPropertiesInJSON, property);

            if (dataPropertyCheck) {
                // object is a literal

                return "l";

            } else {

                boolean annotationPropertyCheck = this.annotationPropertySet.annotationPropertyExist(this.annotationPropertiesInJSON, property);

                if (annotationPropertyCheck ||
                        property.equals((RDFS.label).toString()) ||
                        property.equals((OWL2.annotatedProperty).toString()) ||
                        property.equals((OWL2.annotatedSource).toString()) ||
                        property.equals((OWL2.annotatedTarget).toString()) ||
                        property.equals((OWL2.equivalentClass).toString())) {

                    return "a";

                }
            }
        }

        return "Error: Can't calculate property type.";

    }


    /**
     * This method calculate the object type for an annotation property
     * @param object contains an object value
     * @param objectType contains "a", "l" or "r"
     * @return "l" or "r" according to the input object
     */
    private String calculateObjectTypeForAnnotationProperty(String object, String objectType) {

        if (objectType.equals("a") && UrlValidator.getInstance().isValid(object)) {

            return  "r";

        } else if (objectType.equals("a")) {

            return  "l";

        } else if (!objectType.equals("a")) {

            return objectType;

        }

        return "Error: Can't calculate object type from annotation Property";

    }


    /**
     * This method calculate the operation for a statement
     * @param currExecStep contains all information from the ontology for the current execution step
     * @return "s" for save or "d" for delete
     */
    private String calculateOperation(JSONArray currExecStep) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.deleteTripleStatementBOOLEAN.toString())) {

                if (currExecStep.getJSONObject(i).get("object").toString().equals("true")) {

                    return "d";

                } else {

                    return "s";

                }

            }

        }

        return "s";

    }


    /**
     * This method calculates the position of a new generated part in the partonomy.
     * @param dataToFindObjectInTDB contains information to find a potential object in a jena tdb
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the position in the partonomy
     */
    private String calculatePositionInPartonomy(JSONObject dataToFindObjectInTDB, JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        String parent = "?parent";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.parentRequiredForSPrOVARIABLECalculatePositionInPartonomy.toString())) {

                boolean parentWasFound = false;

                // check info input
                Iterator<String> infoInputKeys = this.infoInput.keys();

                while (infoInputKeys.hasNext() && (!parentWasFound)) {

                    String currKey = infoInputKeys.next();

                    if (currKey.equals(currExecStep.getJSONObject(i).get("object").toString())) {

                        parent = "<" + this.infoInput.get(currKey).toString() + ">";

                        parentWasFound = true;

                    }

                }

                if (!parentWasFound) {

                    parent = "<" + currExecStep.getJSONObject(i).get("object").toString() + ">";

                }

            }

        }

        String posProperty = "?posProperty";

        if (dataToFindObjectInTDB.has("property")) {

            posProperty = "<" + dataToFindObjectInTDB.get("property").toString() + ">";

        }

        String ng = "?g";

        if (dataToFindObjectInTDB.has("ng")) {

            ng = "<" + dataToFindObjectInTDB.get("ng").toString() + ">";

        }

        String directory = dataToFindObjectInTDB.get("directory").toString();

        SelectBuilder selectWhereBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

        selectWhereBuilder.addWhere(parent, "<http://purl.obolibrary.org/obo/BFO_0000051>", "?children");
        // has part

        selectWhereBuilder.addWhere("?children", posProperty, "?o");

        SelectBuilder selectBuilder = new SelectBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        ExprVar exprVar = new ExprVar("children");

        Aggregator aggregator = AggregatorFactory.createCountExpr(true, exprVar.getExpr());

        ExprAggregator exprAggregator = new ExprAggregator(exprVar.asVar(), aggregator);

        selectBuilder.addVar(exprAggregator.getExpr(), "?count");

        selectBuilder.addGraph(ng, selectWhereBuilder);

        String sparqlQueryString = selectBuilder.buildString();

        String numberOfKnownPositions = connectionToTDB.pullSingleDataFromTDB(directory, sparqlQueryString, "?count");

        return "" + (Integer.parseInt(numberOfKnownPositions) + 1);

    }


    /**
     * This method calculate the subject for a statement
     * @param dataToFindSubjectInTDB contains information to find a potential subject in a jena tdb
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the specific uri of a subject
     */
    private String calculateSubject(JSONObject dataToFindSubjectInTDB, JSONArray currExecStep, JSONObject currComponentObject, JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())) {

                String subject = currExecStep.getJSONObject(i).get("object").toString();

                if (subject.equals(SprO.sproVARIABLEThisEntryID.toString())) {

                    return hasExecutionStepFocus ? this.executionStepFocus : this.mdbEntryID;

                } else if (subject.equals(SprO.sproVARIABLEThisUserID.toString())) {

                    // the user ID is the combination of the ueid and the local identifier SCBasic.user
                    return this.mdbUEID + "#" + SCBasic.user.getLocalName() + "_1";

                } else if (subject.equals(SprO.sproVARIABLEThisCoreID.toString())) {

                    return this.mdbCoreID;

                } else if (subject.equals(SprO.sproVARIABLEQuestionMark.toString())) {

                    SelectBuilder selectBuilder = new SelectBuilder();

                    PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                    selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                    SelectBuilder tripleSPO = new SelectBuilder();

                    String property = "?p";

                    if (dataToFindSubjectInTDB.has("property")) {

                        property = "<" + dataToFindSubjectInTDB.get("property").toString() + ">";

                    }

                    String object = "?o";

                    if (dataToFindSubjectInTDB.has("object")) {

                        object = "<" + dataToFindSubjectInTDB.get("object").toString() + ">";

                    }

                    tripleSPO.addWhere("?s", property, object);

                    selectBuilder.addVar(selectBuilder.makeVar("?s"));

                    String ng = "?g";

                    if (dataToFindSubjectInTDB.has("ng")) {

                        ng = "<" + dataToFindSubjectInTDB.get("ng").toString() + ">";

                    }

                    selectBuilder.addGraph(ng, tripleSPO);

                    String sparqlQueryString = selectBuilder.buildString();

                    String queryResult = connectionToTDB.pullSingleDataFromTDB(dataToFindSubjectInTDB.get("directory").toString(), sparqlQueryString, "?s");

                    // return 'SPrO_VARIABLE: empty' - URI (if result is empty) or the URI from the jena tdb
                    return queryResult.equals("") ? SprO.sproVARIABLEEmpty.toString() : queryResult;

                } else if (subject.equals(SprO.sproVARIABLEThisEntryComponent.toString())) {

                    if (jsonInputObject.get("value").toString().equals("show_localID")) {
                        // identify named graph with new active part

                        return this.mdbEntryID + "#" + jsonInputObject.get("localID").toString();

                    } else {

                        JSONObject jsonFromMongoDB = this.mongoDBConnection.pullDataFromMongoDBWithLocalID(jsonInputObject);

                        return jsonFromMongoDB.get("individualID").toString();

                    }

                } else if (subject.equals(SprO.sproVARIABLEThisUserEntryID.toString())) {

                    return this.mdbUEID;

                } else if (subject.equals(SprO.sproVARIABLEEntryCurrentlyInFocus.toString())) {

                    return this.currentFocus;

                } else if (subject.equals(SprO.sproVARIABLEResourceDescribedWithThisDescriptionFormComposition.toString())) {

                    if (jsonInputObject.has("partID")) {

                        return jsonInputObject.get("mdbentryid").toString() + "#" + jsonInputObject.get("partID").toString();

                    } else {

                        System.out.println("WARN: There is no partID in Input!");

                    }

                } else if (subject.contains(SprO.identifiedWorkflowAction.getLocalName())) {

                    return jsonInputObject.get(SprO.identifiedWorkflowAction.getLocalName()).toString();

                } else if (subject.contains("__SPRO_")) {

                    String localNamePropertyInObject = subject.substring(subject.indexOf("__") + 2);

                    Iterator<String> keyIterator = this.generatedResources.keys();

                    while (keyIterator.hasNext()) {

                        String currKey = keyIterator.next();

                        // get local name of a key
                        String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                        if (localNameOfKey.equals(localNamePropertyInObject)) {
                            // get already generated resource from cache

                            return this.generatedResources.get(currKey).toString();

                        }

                    }

                    if (jsonInputObject.has("localIDs")) {

                        JSONArray currJSONArray = jsonInputObject.getJSONArray("localIDs");

                        for (int j = 0; j < currJSONArray.length(); j++) {

                            JSONObject currJSONObject = currJSONArray.getJSONObject(j);

                            if (currJSONObject.has("keyword")) {

                                if (ResourceFactory.createResource(subject).getLocalName().equals(currJSONObject.get("keyword").toString()) &&
                                        jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                    if (EmailValidator.getInstance().isValid(currJSONObject.get("value").toString())) {

                                        return "mailto:" + currJSONObject.get("value").toString();

                                    } else {

                                        return currJSONObject.get("value").toString();

                                    }

                                }

                            }

                        }

                    }

                    // check identified resources
                    Iterator<String> identifiedResIterator = this.identifiedResources.keys();

                    while (identifiedResIterator.hasNext()) {

                        String currKey = identifiedResIterator.next();

                        if (currKey.equals(subject)) {
                            // get already identified resource from cache

                            if (EmailValidator.getInstance().isValid(this.identifiedResources.get(currKey).toString())) {
                                // convert mail to a complete uri

                                return "mailto:" + this.identifiedResources.get(currKey).toString();

                            }

                            return this.identifiedResources.get(currKey).toString();

                        }

                    }

                    // check info input
                    Iterator<String> infoInputKeys = this.infoInput.keys();

                    while (infoInputKeys.hasNext()) {

                        String currKey = infoInputKeys.next();

                        if (currKey.equals(subject)) {

                            return this.infoInput.get(currKey).toString();

                        }

                    }

                } else if (subject.equals(SprO.sproVARIABLEKnownResourceA.toString())) {

                    boolean hasConstraint = false;
                    String constraint = "";

                    for (int j = 0; j < currExecStep.length(); j++) {

                        if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.hasConstraint.toString())) {

                            constraint = currExecStep.getJSONObject(j).get("object").toString();
                            hasConstraint = true;

                        }

                    }

                    if (hasConstraint) {

                        if (constraint.equals(SprO.sproVARIABLEThisCookie.toString())) {

                            return jsonInputObject.get(SprO.sproVARIABLEKnownResourceA.toString()).toString();

                        }

                    }

                } else if (subject.equals(SprO.sproVARIABLEKnownResourceB.toString())) {

                    boolean hasConstraint = false;
                    String constraint = "";

                    for (int j = 0; j < currExecStep.length(); j++) {

                        if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.hasConstraint.toString())) {

                            constraint = currExecStep.getJSONObject(j).get("object").toString();
                            hasConstraint = true;

                        }

                    }

                    if (hasConstraint) {

                        if (constraint.equals(SprO.sproVARIABLEThisCookie.toString())) {

                            return jsonInputObject.get(SprO.sproVARIABLEKnownResourceB.toString()).toString();

                        }

                    }

                } else {

                    // check identified resources
                    Iterator<String> identifiedResIterator = this.identifiedResources.keys();

                    while (identifiedResIterator.hasNext()) {

                        String currKey = identifiedResIterator.next();

                        if (currKey.equals(subject)) {
                            // get already identified resource from cache

                            return this.identifiedResources.get(currKey).toString();

                        }

                    }

                    // check info input
                    Iterator<String> infoInputKeys = this.infoInput.keys();

                    while (infoInputKeys.hasNext()) {

                        String currKey = infoInputKeys.next();

                        if (currKey.equals(subject)) {

                            return this.infoInput.get(currKey).toString();

                        }

                    }

                    return subject;

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {

                String subject = currExecStep.getJSONObject(i).get("object").toString();

                if (this.infoInput.has(subject)) {

                    subject = this.infoInput.get(subject).toString();

                }

                if (this.mdbCoreIDNotEmpty
                        && this.mdbUEIDNotEmpty
                        && !this.mdbEntryIDNotEmpty) {


                } else if (this.mdbEntryIDNotEmpty
                        && this.mdbUEIDNotEmpty) {

                    // check if subject already exist in another workspace
                    for (int j = 0; j < currExecStep.length(); j++) {

                        if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.subjectBelongsToWorkspace.toString())) {

                            IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                            String workspace = calculateWorkspaceDirectory(currExecStep);

                            return individualURI.getThisURIForAnIndividual(subject, workspace, connectionToTDB);

                        }

                    }

                    JSONArray objectsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("object_data");

                    // check if subject already was generated in execution step 'copy and save triple statement(s)'
                    for (int j = 0; j < objectsInJSONArray.length(); j++) {

                        if (objectsInJSONArray.get(j).toString().equals(subject)) {

                            if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(j).toString().equals(RDF.type.toString())
                                    && currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(j).toString().equals("s")) {

                                return currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(j).toString();

                            }

                        }

                    }

                } else if (this.mdbUEIDNotEmpty) {

                    JSONArray objectsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("object_data");

                    for (int j = 0; j < objectsInJSONArray.length(); j++) {

                        if (objectsInJSONArray.get(j).toString().equals(subject)) {

                            if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(j).toString().equals(RDF.type.toString())
                                    && currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(j).toString().equals("s")) {

                                return currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(j).toString();

                            }

                        }

                    }

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectThisEntrySSpecificIndividualOf.toString())) {

                for (int j = 0; j < currExecStep.length(); j++) {

                    if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                        // input contains for example this.mdbUEID
                        IndividualURI individualURI = new IndividualURI(this.executionStepFocus);

                        String workspace = calculateWorkspaceDirectory(currExecStep);

                        return individualURI.getThisURIForAnIndividual(currExecStep.getJSONObject(i).get("object").toString(), workspace, connectionToTDB);

                    }

                }

                if (this.mdbCoreIDNotEmpty
                        && this.mdbUEIDNotEmpty
                        && !(this.mdbEntryIDNotEmpty)) {


                } else if (this.mdbEntryIDNotEmpty
                        && this.mdbUEIDNotEmpty) {

                    FilterBuilder filterBuilder = new FilterBuilder();

                    SelectBuilder selectBuilder = new SelectBuilder();

                    PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                    selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                    SelectBuilder tripleSPO = new SelectBuilder();

                    tripleSPO.addWhere("?s", RDF.type, "<" + currExecStep.getJSONObject(i).get("object").toString() + ">");

                    selectBuilder.addVar(selectBuilder.makeVar("?s"));

                    selectBuilder.addGraph("?g", tripleSPO);

                    SPARQLFilter sparqlFilter = new SPARQLFilter();

                    ArrayList<String> filterItems = new ArrayList<>();

                    filterItems.add(this.mdbEntryID);

                    ArrayList<String> filter = sparqlFilter.getRegexSTRFilter("?s", filterItems);

                    selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

                    String sparqlQueryString = selectBuilder.buildString();

                    String directory = calculateWorkspaceDirectory(currExecStep);

                    return connectionToTDB.pullSingleDataFromTDB(directory, sparqlQueryString, "?s");
                    // draft workspace directory

                } else if (this.mdbUEIDNotEmpty) {

                    FilterBuilder filterBuilder = new FilterBuilder();

                    SelectBuilder selectBuilder = new SelectBuilder();

                    PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                    selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                    SelectBuilder tripleSPO = new SelectBuilder();

                    tripleSPO.addWhere("?s", RDF.type, "<" + currExecStep.getJSONObject(i).get("object").toString() + ">");

                    selectBuilder.addVar(selectBuilder.makeVar("?s"));

                    selectBuilder.addGraph("?g", tripleSPO);

                    SPARQLFilter sparqlFilter = new SPARQLFilter();

                    ArrayList<String> filterItems = new ArrayList<>();

                    filterItems.add(jsonInputObject.get("mdbueid").toString());

                    ArrayList<String> filter = sparqlFilter.getRegexSTRFilter("?s", filterItems);

                    selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

                    String sparqlQueryString = selectBuilder.buildString();

                    TDBPath tdbPath = new TDBPath();

                    return connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), sparqlQueryString, "?s");

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectUniqueIndividualOf.toString())) {

                String correspondingNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                String correspondingDirectory = calculateWorkspaceDirectory(currExecStep);

                SelectBuilder selectWhereBuilder = new SelectBuilder();

                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                selectWhereBuilder.addWhere("?s", RDF.type, "<" + currExecStep.getJSONObject(i).get("object").toString() + ">");

                SelectBuilder selectBuilder = new SelectBuilder();

                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                selectBuilder.addVar("?s");

                selectBuilder.addGraph("<" + correspondingNG + ">", selectWhereBuilder);

                String sparqlQueryString = selectBuilder.buildString();

                return connectionToTDB.pullSingleDataFromTDB(correspondingDirectory, sparqlQueryString, "?s");

            }

        }

        return "Error: Can't calculate subject.";

    }


    /**
     * This method translates the output for an active tab in the mdb partonomy widget
     * @param uriToCheck contains are URI with potential tab information
     * @param jsonInputObject contains the information for the calculation
     * @return a value for the JSON key 'active_tab'
     */
    private String calculateTabToUse (String uriToCheck, JSONObject jsonInputObject) {

        if (uriToCheck.equals(SprO.sproVARIABLEUseActiveTab.toString())) {

            if (jsonInputObject.has("active_tab")) {

                switch (jsonInputObject.get("active_tab").toString()) {

                    case "image" :

                        this.tabToUseURI = SCMDBMD.hasDescriptionFormInNamedGraph.toString();
                        // todo change this when the graph for the image tab was defined

                        break;

                    case "text" :

                        this.tabToUseURI = SCMDBMD.hasFreeTextDescriptionCompositionInNamedGraph.toString();

                        break;

                    case "metadata" :

                        this.tabToUseURI = SCMDBMD.hasDescriptionFormInNamedGraph.toString();
                        // todo change this when the graph for the metadata tab was defined

                        break;

                    case "form" :

                        this.tabToUseURI = SCMDBMD.hasDescriptionFormInNamedGraph.toString();

                        break;

                    case "graph" :

                        this.tabToUseURI = SCMDBMD.hasDescriptionFormInNamedGraph.toString();

                        break;

                }

                return jsonInputObject.get("active_tab").toString();

            } else {

                return "ERROR: There is no key 'active_tab' in the jsonInputObject.";

            }

        } else if (uriToCheck.equals(SCMDBMD.hasImageAnnotationInNamedGraph.toString())) {

            return "image";

        } else if (uriToCheck.equals(SCMDBMD.hasFreeTextDescriptionInNamedGraph.toString())) {

            return "text";

        } else if (uriToCheck.equals(SCMDBMD.hasMetadataFormInNamedGraph.toString())) {

            return "meta";

        } else if (uriToCheck.equals(SCMDBMD.hasDescriptionFormInNamedGraph.toString())) {

            return "form";

        } else {

            return "ERROR: Not possible to handle the input to provide an information for the active tab.";

        }

    }

    /**
     * This method calculate the corresponding workspace directory for an execution step
     * @param currExecStep contains all information from the ontology for the current execution step
     * @return the path to the workspace directory
     */
    private String calculateWorkspaceDirectory(JSONArray currExecStep) {

        for (int i = 0; i < currExecStep.length();i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                TDBPath tdbPath = new TDBPath();

                if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEFindWorkspace.toString())
                        || currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEThisEntrySWorkspace.toString())) {
                    // TODO: one of these KEYWORDs is redundant and must be deleted in the future


                    String uriWorkspaceIdentifier = this.mdbEntryID.substring((this.mdbEntryID.lastIndexOf("-") + 1), this.mdbEntryID.indexOf("_"));

                    switch (uriWorkspaceIdentifier) {
                        // todo: advanced this calculation if we need it for the general workspace calculation (e.g. core or admin workspace)

                        case "d" :
                            // draft

                            return tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString());

                        case "p" :
                            // publish

                            return tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYPublishedWorkspaceDirectory.toString());

                        default:

                            return "Error: Can't calculate directory from MDBEntryID.";

                    }

                } else {

                    String workspaceURI = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                    return tdbPath.getPathToTDB(workspaceURI);

                }

            }

        }

        System.out.println("can't calculate directory = " + currExecStep);

        return "Error: Can't calculate directory.";

    }


    /**
     * This method gets the corresponding source workspace directory
     * @param currExecStep contains all information from the ontology for the current execution step
     * @return the path to the source workspace directory
     */
    private String copyFromWorkspace(JSONArray currExecStep) {

        for (int i = 0; i < currExecStep.length();i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.copyFromWorkspace.toString())) {

                TDBPath tdbPath = new TDBPath();

                return tdbPath.getPathToTDB(currExecStep.getJSONObject(i).get("object").toString());

            }

        }

        return "Error: Can't find directory.";

    }


    /**
     * This method identifies a workspace directory.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @return the path to a workspace directory
     */
    private String saveNewURIsToWorkspace(JSONArray currExecStep) {

        for (int i = 0; i < currExecStep.length();i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.saveNewURIsToWorkspace.toString())) {

                TDBPath tdbPath = new TDBPath();

                return tdbPath.getPathToTDB(currExecStep.getJSONObject(i).get("object").toString());

            }

        }

        return "Error: Can't find directory.";

    }

    /**
     * This method generates a hashmap with key,value-pair (IDspace, namespace)
     * @param keywordURI contains the URI of a keyword
     * @param idSpaceNamespaceHashMap contains a JSONObject for save the key,value-pair(s)
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with key,value-pair(s)
     */
    private JSONObject useOntologyIDSpaceMapping(String keywordURI, JSONObject idSpaceNamespaceHashMap, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectWhereBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

        ExprVar idSpaceVar = new ExprVar("IDspace");

        ExprVar namespaceVar = new ExprVar("namespace");

        selectWhereBuilder.addWhere("<" + keywordURI + ">", SprO.hasOntologyIDspace, idSpaceVar.toString());

        selectWhereBuilder.addWhere("?axiom", OWL2.annotatedTarget, idSpaceVar.toString());

        selectWhereBuilder.addWhere("?axiom", SprO.hasOntologyNamespace, namespaceVar.toString());

        SelectBuilder selectBuilder = new SelectBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        selectBuilder.addVar(idSpaceVar);

        selectBuilder.addVar(namespaceVar);

        selectBuilder.addGraph("?g", selectWhereBuilder);

        String sparqlQueryString = selectBuilder.buildString();

        ResultSet queryResultSet = connectionToTDB.pullMultipleSelectDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryString);

        while (queryResultSet.hasNext()) {

            QuerySolution currSolution = queryResultSet.nextSolution();

            idSpaceNamespaceHashMap.put(currSolution.getLiteral(idSpaceVar.toString()).getLexicalForm(), currSolution.getResource(namespaceVar.toString()).toString());

        }

        return idSpaceNamespaceHashMap;

    }


    /**
     * This method convert the input multiple ArrayList to a JSONObject, which can use as input for the
     * SOCCOMASExecutionStepHandler
     * @param generatedCoreIDData contains multiple data
     * @return a JSON Object, which can use as Input for the SOCCOMASExecutionStepHandler
     */
    public JSONObject convertArrayListToJSONObject (ArrayList<ArrayList<String>> generatedCoreIDData) {

        ListIterator<ArrayList<String>> generatedCoreIDDataLI = generatedCoreIDData.listIterator();

        JSONObject datasetsJSONObject = new JSONObject();

        JSONObject ngsJSONObject = new JSONObject();

        ArrayList<String> datasetsArrayList = new ArrayList<>();

        ArrayList<String> ngsArrayList = new ArrayList<>();

        // transform the generated data to json format
        while (generatedCoreIDDataLI.hasNext()) {

            JSONObject datasetJSONObject = new JSONObject();
            JSONObject ngJSONObject = new JSONObject();
            JSONObject triplesAndOperationJSONObject = new JSONObject();
            JSONObject objectJSONObject = new JSONObject();

            ArrayList<String> currGeneratedCoreIDData = generatedCoreIDDataLI.next();

            String currDataset = currGeneratedCoreIDData.get(6);
            String currNG = currGeneratedCoreIDData.get(5);

            if (!datasetsArrayList.contains(currDataset)) {

                datasetsArrayList.add(currDataset);

                datasetJSONObject.put("dataset", currDataset);


                if (!ngsArrayList.contains(currNG)) {

                    ngsArrayList.add(currNG);

                    triplesAndOperationJSONObject.put("subject", currGeneratedCoreIDData.get(0));
                    triplesAndOperationJSONObject.put("property", currGeneratedCoreIDData.get(1));

                    objectJSONObject.put("object_data", currGeneratedCoreIDData.get(2));
                    objectJSONObject.put("object_type", currGeneratedCoreIDData.get(3));

                    triplesAndOperationJSONObject.put("object", objectJSONObject);
                    triplesAndOperationJSONObject.put("operation", currGeneratedCoreIDData.get(4));

                    ngJSONObject.put("ng", currNG);

                    ngJSONObject.append("triples", triplesAndOperationJSONObject);

                    ngsJSONObject.append("ngs", ngJSONObject);

                }

                datasetJSONObject.append("ngs", ngJSONObject);

                datasetsJSONObject.append("datasets", datasetJSONObject);


            } else if (datasetsArrayList.contains(currDataset)) {

                JSONArray datasetsJSONArray= datasetsJSONObject.getJSONArray("datasets");

                for (int i = 0; i < datasetsJSONArray.length(); i++) {

                    if (datasetsJSONArray.getJSONObject(i).get("dataset").equals(currDataset)) {

                        if (!ngsArrayList.contains(currNG)) {

                            ngsArrayList.add(currNG);

                            ngJSONObject.put("ng", currNG);

                            triplesAndOperationJSONObject.put("subject", currGeneratedCoreIDData.get(0));
                            triplesAndOperationJSONObject.put("property", currGeneratedCoreIDData.get(1));

                            objectJSONObject.put("object_data", currGeneratedCoreIDData.get(2));
                            objectJSONObject.put("object_type", currGeneratedCoreIDData.get(3));

                            triplesAndOperationJSONObject.put("object", objectJSONObject);
                            triplesAndOperationJSONObject.put("operation", currGeneratedCoreIDData.get(4));

                            ngJSONObject.append("triples", triplesAndOperationJSONObject);

                            datasetsJSONObject.getJSONArray("datasets").getJSONObject(i).append("ngs", ngJSONObject);

                        } else {

                            JSONArray ngsJSONArray= datasetsJSONObject.getJSONArray("datasets").getJSONObject(i).getJSONArray("ngs");

                            for (int j = 0; j < ngsJSONArray.length(); j++) {

                                if (ngsJSONArray.getJSONObject(j).get("ng").equals(currNG)) {

                                    triplesAndOperationJSONObject.put("subject", currGeneratedCoreIDData.get(0));
                                    triplesAndOperationJSONObject.put("property", currGeneratedCoreIDData.get(1));

                                    objectJSONObject.put("object_data", currGeneratedCoreIDData.get(2));
                                    objectJSONObject.put("object_type", currGeneratedCoreIDData.get(3));

                                    triplesAndOperationJSONObject.put("object", objectJSONObject);
                                    triplesAndOperationJSONObject.put("operation", currGeneratedCoreIDData.get(4));

                                    datasetsJSONObject.getJSONArray("datasets").getJSONObject(i).getJSONArray("ngs").getJSONObject(j).append("triples", triplesAndOperationJSONObject);

                                }

                            }

                        }


                    }

                }
            }

        }

        return datasetsJSONObject;

    }


    /**
     * This method provides code to differ action for different execution step properties.
     * @param sortedKBJSONArray contains the sorted knowledge base order
     * @param sortedKBIndicesJSONArray contains the sorted knowledge base order indices
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the expanded current component information
     */
    public JSONObject convertKBToJSONObject(JSONArray sortedKBJSONArray, JSONArray sortedKBIndicesJSONArray,
                                            JSONObject currComponentObject, JSONObject jsonInputObject,
                                            JenaIOTDBFactory connectionToTDB) {


        for (int i = 0;  i < sortedKBJSONArray.length(); i++) {

            JSONArray currExecStep = sortedKBJSONArray.getJSONArray(i);

            String currExecStepIndex = sortedKBIndicesJSONArray.get(i).toString();

            String annotatedProperty = "";

            for (int j = 0; j < currExecStep.length(); j++) {

                if (currExecStep.getJSONObject(j).has("annotatedProperty")) {

                    annotatedProperty = currExecStep.getJSONObject(j).get("annotatedProperty").toString();

                    currExecStep.remove(j);

                }

            }

            // calculate the start date
            long executionStart = System.currentTimeMillis();

            // calculate the query time
            long queryTime;

            if (annotatedProperty.equals(SprO.executionStepSaveDeleteTripleStatementS.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                    && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepSaveDeleteTripleStatements
                        (currExecStep, currComponentObject, jsonInputObject, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time1= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepIfThenElseStatement.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                String nextStep = executionStepIfThenElseStatement(jsonInputObject, currExecStep, connectionToTDB);



                if  (UrlValidator.getInstance().isValid(nextStep)) {
                    // case nextStep is a resource

                    if (nextStep.equals(SprO.sproOPERATIONEndAction.toString())) {
                        // true case

                        System.out.println("Finished execution step iteration.");

                        return currComponentObject.put("valid", "true");

                    } else if (nextStep.equals(SprO.sproOPERATIONERROREndAction.toString())) {
                        // false case

                        return currComponentObject.put("valid", "false");

                    }

                } else {
                    // case nextStep is a literal

                    for (int j = 0; j < sortedKBIndicesJSONArray.length(); j++) {

                        if (sortedKBIndicesJSONArray.get(j).toString().equals(nextStep)) {
                            // get next execution step

                            i = j - 1;

                        }

                    }
                }

                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time2= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepCopyAndSaveTripleStatementS.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println(currExecStep);

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepCopyAndSaveTripleStatements
                        (currExecStep, currComponentObject, jsonInputObject, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time3= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepUpdateTripleStatementS.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println(currExecStep);

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepUpdateTripleStatements
                        (currExecStep, currComponentObject, jsonInputObject, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time4= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepDeleteNamedGraphs.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println(currExecStep);

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepDeleteNamedGraphs
                        (currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time5= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepDecisionDialogue.toString())) {

                currComponentObject = executionStepDecisionDialogue
                        (currComponentObject, jsonInputObject, currExecStep);

                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time6= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepTriggerWorkflowAction.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepTriggerWorkflowAction
                        (currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time7= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepGenerateResources.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepGenerateResources
                        (currExecStep, currComponentObject, jsonInputObject, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time8= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepExecuteNow.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepExecuteNow
                        (currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time9= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepHyperlink.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepHyperlink(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time10= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepCloseModule.toString())) {

                return currComponentObject;

            } else if (annotatedProperty.equals(SprO.executionStepSearchTripleStore.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                executionStepSearchTripleStore(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time11= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepApplicationOperation.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepApplicationOperation(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time12= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepDefineVariables.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepDefineVariables(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time13= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepDeleteAllTriplesOfNamedGraph.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                executionStepDeleteAllTriplesOfNamedGraph(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);


                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time14= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepExtractAndSaveEntryComposition.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                executionStepExtractAndSaveEntryComposition(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time15= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepSpecificationsAndAllocationsForHyperlink.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepSpecificationsAndAllocationsForHyperlink(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time16= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepDeleteMultipleTripleStatements.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepDeleteMultipleTripleStatements(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time17= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepDeletePartOfComposition.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepDeletePartOfComposition(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time18= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepCopyNamedGraphs.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                currComponentObject = executionStepCopyNamedGraphs(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);



                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time19= " + queryTime);

            } else if (annotatedProperty.equals(SprO.executionStepGetDOI.toString())) {

                System.out.println("currExecStepIndex: " + currExecStepIndex);
                System.out.println();
                //System.out.println("currExecStep: " + currExecStep);
                //System.out.println();

                if (currComponentObject.has("input_data")
                        && currComponentObject.getJSONObject("input_data").has("subject")) {

                }

                executionStepGetDOI(jsonInputObject, currExecStep, connectionToTDB);


                // calculate the query time
                queryTime = System.currentTimeMillis() - executionStart;

                System.out.println("query time20= " + queryTime);

            } else {

                //System.out.println("annotatedProperty: " + annotatedProperty + " in step: " + i);

                //currComponentObject.put("valid", "false");

            }

            // check if there exist the property "go to execution step" in the current execution step or end action operation
            for (int j = 0; j < currExecStep.length(); j++) {


                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.goToExecutionStep.toString())) {

                    for (int k = 0; k < sortedKBIndicesJSONArray.length(); k++) {

                        if (sortedKBIndicesJSONArray.get(k).toString().equals(currExecStep.getJSONObject(j).get("object").toString())) {
                            // get next execution step

                            i = k - 1;

                        }

                    }

                } else if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.endActionOperation.toString())) {

                    i = sortedKBJSONArray.length();

                }

            }

        }

        if (!currComponentObject.has("valid")) {

            currComponentObject.put("valid", "true");

        }

        return currComponentObject;

    }


    /**
     * This method removes statement(s) in a jena tdb
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return additional modified information about the current object
     */
    public JSONObject executionStepCopyNamedGraphs(JSONObject currComponentObject, JSONObject jsonInputObject,
                                                   JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        String newFocusURI = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryID.toString())) {

                newFocusURI = setFocusOnIndividual(currExecStep.getJSONObject(i).get("object").toString(), currExecStep, jsonInputObject, newFocusURI, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        JSONArray ngsJSONArray = new JSONArray(), localIDsOfIgnoredURIs = new JSONArray();

        JSONObject idSpaceNamespaceHashMap = new JSONObject();

        String newDirectory = "", oldDirectory = "", newcreatedURIsWorkspace="", newNS = "", oldNS = "", coreIndividualsNG = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())
                    || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                String ng = calculateNGWithMultipleInput(currExecStep.getJSONObject(i).get("property").toString(), currExecStep.getJSONObject(i).get("object").toString(), currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                if (!ng.equals(SprO.sproVARIABLEEmpty)) {

                    ngsJSONArray.put(ng);

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                newDirectory = calculateWorkspaceDirectory(currExecStep);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.copyFromWorkspace.toString())) {

                oldDirectory = copyFromWorkspace(currExecStep);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.updateURIsOfAndInNamedGraphsUsingNamespaceOfEntryID.toString())) {

                newNS = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.updateAllURIsThatShareNamespaceWith.toString())) {

                oldNS = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.doNotUpdateURIOf.toString())) {

                String classURI = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                localIDsOfIgnoredURIs.put(ResourceFactory.createResource(classURI).getLocalName());

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphsOfThisSPrOVariableList.toString())) {

                ngsJSONArray = calculateNGListWithMultipleInput(currExecStep.getJSONObject(i).get("object").toString(), ngsJSONArray);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.useOntologyIDspaceMapping.toString())) {

                idSpaceNamespaceHashMap = useOntologyIDSpaceMapping(currExecStep.getJSONObject(i).get("object").toString(), idSpaceNamespaceHashMap, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.saveNewURIsToWorkspace.toString())) {

                newcreatedURIsWorkspace = saveNewURIsToWorkspace(currExecStep);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.saveNewURIsToNamedGraph.toString())) {

                // swap property URI for calculation

                // todo change to load from/save to/update in named graph
                coreIndividualsNG = calculateNGWithMultipleInput(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString(), currExecStep.getJSONObject(i).get("object").toString(), currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

            }

        }

        for (int i = 0; i < ngsJSONArray.length(); i++) {

            Model newModel = ModelFactory.createDefaultModel();

            newModel.add(connectionToTDB.pullNamedModelFromTDB(oldDirectory, ngsJSONArray.get(i).toString()));

            // handle subject resources

            ResIterator resIterator = newModel.listSubjects();

            while (resIterator.hasNext()) {

                Resource resource = resIterator.next();

                if (resource.isURIResource()) {

                    if (resource.toString().contains(oldNS)) {

                        if (resource.toString().equals(oldNS)) {

                            ResourceUtils.renameResource(resource, newNS);

                        } else {

                            boolean updateResource = true;

                            for (int j=0; j < localIDsOfIgnoredURIs.length(); j++) {

                                if (resource.toString().contains(localIDsOfIgnoredURIs.get(j).toString())) {

                                    System.out.println("Don't update the resource = " + resource);

                                    updateResource = false;

                                }

                            }

                            if (updateResource) {

                                ResourceUtils.renameResource(resource, newNS + "#" + resource.asResource().getLocalName());

                            }

                        }

                    }

                }

            }

            // handle object resources

            NodeIterator objectIter = newModel.listObjects();

            while (objectIter.hasNext()) {

                RDFNode rdfNode = objectIter.next();

                if (rdfNode.isURIResource()) {

                    Resource resource = rdfNode.asResource();

                    if (resource.toString().contains(oldNS)) {

                        if (resource.toString().equals(oldNS)) {

                            ResourceUtils.renameResource(resource, newNS);

                        } else {

                            boolean updateResource = true;

                            for (int j=0; j < localIDsOfIgnoredURIs.length(); j++) {

                                if (resource.toString().contains(localIDsOfIgnoredURIs.get(j).toString())) {

                                    System.out.println("Don't update the resource = " + resource);

                                    updateResource = false;

                                }

                            }

                            if (updateResource) {

                                ResourceUtils.renameResource(resource, newNS + "#" + resource.asResource().getLocalName());

                            }

                        }

                    }

                }

            }

            // todo list statements and adds them to currComponentObject with new named graph and new directory all operation s calculate object type

            StmtIterator stmtIter = newModel.listStatements();

            while (stmtIter.hasNext()) {

                Statement stmt = stmtIter.nextStatement();

                currComponentObject.getJSONObject("input_data").append("subject", stmt.getSubject().toString());
                currComponentObject.getJSONObject("input_data").append("property", stmt.getPredicate().toString());
                currComponentObject.getJSONObject("input_data").append("ng", newNS + "#" + ResourceFactory.createResource(ngsJSONArray.get(i).toString()).getLocalName());
                currComponentObject.getJSONObject("input_data").append("directory", newDirectory);
                currComponentObject.getJSONObject("input_data").append("object_data", stmt.getObject().toString());

                if (stmt.getObject().isLiteral()) {

                    currComponentObject.getJSONObject("input_data").append("object_type", "l");

                } else {

                    currComponentObject.getJSONObject("input_data").append("object_type", "r");

                }

                currComponentObject.getJSONObject("input_data").append("operation", "s");

            }

            if (!ResourceFactory.createResource(ngsJSONArray.get(i).toString()).equals(SprO.sproVARIABLEEmpty)) {

                // save new ng in core individuals named graph

                String localIDClass = ResourceFactory.createResource(ngsJSONArray.get(i).toString()).getLocalName();

                localIDClass = localIDClass.substring(0, localIDClass.lastIndexOf("_"));

                String currIDSpace = localIDClass.substring(0, localIDClass.lastIndexOf("_"));

                if (idSpaceNamespaceHashMap.has(currIDSpace)) {

                    String classURI = idSpaceNamespaceHashMap.get(currIDSpace).toString() + "#" + localIDClass;

                    currComponentObject.getJSONObject("input_data").append("subject", newNS + "#" + ResourceFactory.createResource(ngsJSONArray.get(i).toString()).getLocalName());
                    currComponentObject.getJSONObject("input_data").append("property", RDF.type.toString());
                    currComponentObject.getJSONObject("input_data").append("ng", coreIndividualsNG);
                    currComponentObject.getJSONObject("input_data").append("directory", newcreatedURIsWorkspace);
                    currComponentObject.getJSONObject("input_data").append("object_data", classURI);
                    currComponentObject.getJSONObject("input_data").append("object_type", "r");
                    currComponentObject.getJSONObject("input_data").append("operation", "s");

                }

            }

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }


    /**
     * This method adds a decision to the currComponentObject and typical stops the workflow or transition.
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @return additional modified information about the current object
     */
    public JSONObject executionStepDecisionDialogue(JSONObject currComponentObject, JSONObject jsonInputObject,
                                                    JSONArray currExecStep) {

        for (int j = 0; j < currExecStep.length(); j++) {

            if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.applicationDialogueMessage.toString())) {

                String outputKey = ResourceFactory.createResource(currExecStep.getJSONObject(j).get("property").toString()).getLocalName();

                currComponentObject.put(outputKey, currExecStep.getJSONObject(j).get("object").toString());

            } else if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.applicationErrorMessage.toString())) {

                String outputKey = ResourceFactory.createResource(currExecStep.getJSONObject(j).get("property").toString()).getLocalName();

                currComponentObject.put(outputKey, currExecStep.getJSONObject(j).get("object").toString());

            }

        }

        if (jsonInputObject.has("localID")) {
            // todo remove this after implementing AddNewPartCountInputFieldItem

            if ((jsonInputObject.get("localID").toString().equals(SCMDBMD.rootElementAddNewPartCountInputFieldItem.getLocalName() + "_1")
                    && !currComponentObject.has(SprO.applicationErrorMessage.getLocalName()))
                    || (jsonInputObject.get("localID").toString().contains(SCMDBMD.anatomicalStructureAddNewPartCountInputFieldItem.getLocalName())
                    && !currComponentObject.has(SprO.applicationErrorMessage.getLocalName()))) {

                currComponentObject.put("valid", "true");

            } else {

                currComponentObject.put("valid", "false");

            }

        }

        return currComponentObject;

    }

    /**
     * This method add the information for a new hyperlink of a current object
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return additional modified information about the current object
     */
    public JSONObject executionStepSpecificationsAndAllocationsForHyperlink(JSONObject currComponentObject,
                                                                            JSONObject jsonInputObject, JSONArray currExecStep,
                                                                            JenaIOTDBFactory connectionToTDB) {

        System.out.println("in method executionStepSpecificationsAndAllocationsForHyperlink");

        String directory = "", rootComponentOfComposition = "", componentOfOntology = "", compositionFromEntry = "",
                rootComponentOfUnionComposition = "", propertyForResourcesToShowExpanded = "";
        int position = -1;
        JSONArray ngs = new JSONArray(), outputDataJSON = new JSONArray(), resourcesToShowExpanded = new JSONArray(),
                activeTabComponents = new JSONArray(), activeTargetComponents = new JSONArray();
        boolean useComponentFromComposition = false, useComponentFromOntology = false, useCompositionFromEntry = false,
                useUnionOfCompositionsWithParentRoot = false, showExpanded = false;

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.useTab.toString())) {

                this.useTab = true;
                this.tabToUse = calculateTabToUse(currExecStep.getJSONObject(i).get("object").toString(), jsonInputObject);

            }

        }

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())
                    || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                ngs.put(calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB));

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                directory = calculateWorkspaceDirectory(currExecStep);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.useRootElement.toString())) {

                rootComponentOfComposition = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());
                useComponentFromComposition = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.useEntryComponent.toString())) {

                componentOfOntology = currExecStep.getJSONObject(i).get("object").toString();
                useComponentFromOntology = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.position.toString())) {

                position = Integer.parseInt(currExecStep.getJSONObject(i).get("object").toString());

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.useHierarchyCompositionFromEntry.toString())) {

                compositionFromEntry = currExecStep.getJSONObject(i).get("object").toString();
                useCompositionFromEntry = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.useUnionOfCompositionsWithParentRootEntryComponent.toString())) {

                rootComponentOfUnionComposition = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());
                useUnionOfCompositionsWithParentRoot = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.updateStoreBOOLEAN.toString())) {

                //System.out.println("currComponentObject before save store" + currComponentObject);

                saveToStores(currComponentObject, jsonInputObject, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.showExpandedThisEntrySSpecificIndividualOf.toString())) {

                resourcesToShowExpanded.put(currExecStep.getJSONObject(i).get("object").toString());
                propertyForResourcesToShowExpanded = currExecStep.getJSONObject(i).get("property").toString();
                showExpanded = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.withActiveMetadataTarget.toString())
                    || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.withActiveCoreTarget.toString())) {

                String activeTargetComponent = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                activeTargetComponents.put(activeTargetComponent);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.withActiveMetadataTab.toString())
                    || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.withActiveCoreTab.toString())) {

                String activeTabComponent = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                activeTabComponents.put(activeTabComponent);

            }

        }

        if ((useComponentFromComposition ||useUnionOfCompositionsWithParentRoot || useCompositionFromEntry)
                && this.parentRootExist
                && (position == this.parentRootPosition)) {

            System.out.println("rootComponentOfUnionComposition = " + this.parentRoot);

            Model unionNGModel = ModelFactory.createDefaultModel(), entryComponentsModel = ModelFactory.createDefaultModel();

            for (int j = 0; j < ngs.length(); j++) {

                unionNGModel = unionNGModel.union(connectionToTDB.pullNamedModelFromTDB(directory, ngs.get(j).toString()));

            }

            ResIterator resIter = unionNGModel.listSubjects();

            while (resIter.hasNext()) {

                Resource entryComponentURI = resIter.next();

                if (unionNGModel.contains(entryComponentURI, RDF.type, OWL2.NamedIndividual)) {

                    Selector tripleSelector = new SimpleSelector(entryComponentURI, null, null, "");

                    StmtIterator tripleStmts = unionNGModel.listStatements(tripleSelector);

                    while (tripleStmts.hasNext()) {

                        Statement stmt = tripleStmts.nextStatement();

                        Resource currSubject = stmt.getSubject();

                        Property currProperty = stmt.getPredicate();

                        Resource currObject;

                        if (stmt.getObject().isURIResource()) {

                            currObject = stmt.getObject().asResource();

                            if (currSubject.equals(entryComponentURI)
                                    && currProperty.equals(RDF.type)
                                    && !currObject.equals(OWL2.NamedIndividual)) {

                                Selector classSelector = new SimpleSelector(currObject, null, null, "");

                                StmtIterator classStmts = unionNGModel.listStatements(classSelector);

                                Resource classSubject = null;

                                while (classStmts.hasNext()) {

                                    Statement classStmt = classStmts.nextStatement();

                                    classSubject = classStmt.getSubject();

                                    if ((!classStmt.getObject().equals(OWL2.Class))
                                            && (!classStmt.getPredicate().equals(RDFS.label))
                                            && (!classStmt.getPredicate().equals(RDFS.subClassOf))
                                            && (!classStmt.getPredicate().equals(OWL2.annotatedTarget))
                                            && (!classStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                        entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, classStmt.getPredicate(), classStmt.getObject()));

                                    }

                                }

                                if (unionNGModel.contains(null, OWL2.annotatedSource, classSubject)) {

                                    ResIterator axiomsForClassSubject = unionNGModel.listSubjectsWithProperty(OWL2.annotatedSource, classSubject);

                                    while (axiomsForClassSubject.hasNext()) {

                                        Resource axiomClassSubject = axiomsForClassSubject.next();

                                        Selector axiomClassSelector = new SimpleSelector(axiomClassSubject, null, null, "");

                                        StmtIterator axiomClassStmts = unionNGModel.listStatements(axiomClassSelector);

                                        while (axiomClassStmts.hasNext()) {

                                            Statement axiomClassStmt = axiomClassStmts.nextStatement();

                                            if ((!axiomClassStmt.getObject().equals(OWL2.Axiom))
                                                    && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedSource))
                                                    && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                    && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                                entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, axiomClassStmt.getPredicate(), axiomClassStmt.getObject()));

                                            }

                                        }

                                    }

                                }

                            }

                        }

                        entryComponentsModel.add(stmt);

                    }

                    if (unionNGModel.contains(null, OWL2.annotatedSource, entryComponentURI)) {

                        ResIterator axiomsForSubject = unionNGModel.listSubjectsWithProperty(OWL2.annotatedSource, entryComponentURI);

                        while (axiomsForSubject.hasNext()) {

                            Resource axiomSubject = axiomsForSubject.next();

                            Selector axiomSelector = new SimpleSelector(axiomSubject, null, null, "");

                            StmtIterator axiomStmts = unionNGModel.listStatements(axiomSelector);

                            while (axiomStmts.hasNext()) {

                                Statement axiomStmt = axiomStmts.nextStatement();

                                if ((!axiomStmt.getObject().equals(OWL2.Axiom))
                                        && (!axiomStmt.getPredicate().equals(OWL2.annotatedSource))
                                        && (!axiomStmt.getPredicate().equals(OWL2.annotatedTarget))
                                        && (!axiomStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                    entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, axiomStmt.getPredicate(), axiomStmt.getObject()));

                                }

                            }

                        }

                    }

                }

            }

            ResIterator subjectIter = entryComponentsModel.listSubjects();

            JSONObject componentsToHide = new JSONObject();

            while (subjectIter.hasNext()) {

                Resource currSubject = subjectIter.nextResource();

                for (int i = 0; i < activeTargetComponents.length(); i++) {

                    if (currSubject.toString().equals(activeTargetComponents.get(i).toString())) {

                        System.out.println("active component = " + activeTargetComponents.get(i).toString());

                        ResIterator parentOfSubjectIter = entryComponentsModel.listSubjectsWithProperty(SprO.hasEntryComponent, currSubject);

                        while (parentOfSubjectIter.hasNext()) {

                            Resource currSubjectParent = parentOfSubjectIter.nextResource();

                            NodeIterator childrenIter = entryComponentsModel.listObjectsOfProperty(currSubjectParent, SprO.hasEntryComponent);

                            while (childrenIter.hasNext()) {

                                RDFNode currChildNode = childrenIter.nextNode();

                                if (currChildNode.isResource()) {

                                    Resource currChild = currChildNode.asResource();

                                    if (!currChild.toString().equals(currSubject.toString())) {

                                        NodeIterator currSubjectPositionIter = entryComponentsModel.listObjectsOfProperty(currSubject, SprO.hasPositionInEntryComponent);

                                        while (currSubjectPositionIter.hasNext()) {

                                            RDFNode currSubjectPositionNode = currSubjectPositionIter.nextNode();

                                            NodeIterator currChildPositionIter = entryComponentsModel.listObjectsOfProperty(currChild, SprO.hasPositionInEntryComponent);

                                            while (currChildPositionIter.hasNext()) {

                                                RDFNode currChildPositionNode = currChildPositionIter.nextNode();

                                                if (currChildPositionNode.toString().equals(currSubjectPositionNode.toString())) {

                                                    componentsToHide.append(currSubjectParent.toString(), currChild.toString());

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                }

                for (int i = 0; i < activeTabComponents.length(); i++) {

                    if (currSubject.toString().equals(activeTabComponents.get(i).toString())) {

                        entryComponentsModel.add(currSubject, SprO.isActive, ResourceFactory.createPlainLiteral("is-active"));

                    }

                }

            }

            Iterator<String> componentsToHideKeys = componentsToHide.keys();

            while (componentsToHideKeys.hasNext()) {

                String currKey = componentsToHideKeys.next();

                if (componentsToHide.get(currKey) instanceof JSONArray) {

                    JSONArray currChildrenToHide = componentsToHide.getJSONArray(currKey);

                    for (int i = 0; i < currChildrenToHide.length(); i++) {

                        entryComponentsModel.removeAll(ResourceFactory.createResource(currChildrenToHide.get(i).toString()), null, null);

                        entryComponentsModel.removeAll(ResourceFactory.createResource(currKey), null, ResourceFactory.createResource(currChildrenToHide.get(i).toString()));

                    }

                }

            }

            StmtIterator entryComponentsModelIter = entryComponentsModel.listStatements();

            OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

            JSONObject entryComponents = this.parentComponents;

            while (entryComponentsModelIter.hasNext()) {

                Statement resStmt = entryComponentsModelIter.nextStatement();

                String currSubject = resStmt.getSubject().toString();

                entryComponents = outputGenerator
                        .manageProperty(currSubject, resStmt, entryComponents, jsonInputObject, connectionToTDB);

            }

            entryComponents = outputGenerator.reorderEntryComponentsValues(entryComponents);

            Iterator<String> iter = entryComponents.keys();

            outputDataJSON = new JSONArray();

            while (iter.hasNext()) {

                String currKey = iter.next();

                JSONObject wrapperJSON = new JSONObject();

                wrapperJSON.put(currKey, entryComponents.getJSONObject(currKey));

                outputDataJSON.put(wrapperJSON);

            }

            outputDataJSON = outputGenerator.orderOutputJSON(this.parentRoot, outputDataJSON);

        } else {

            if (useComponentFromComposition) {

                if (rootComponentOfComposition.equals(SprO.sproVARIABLEFindRootElement.toString())) {

                    String workspace = calculateWorkspaceDirectory(currExecStep);

                    SelectBuilder selectWhereBuilder = new SelectBuilder();

                    PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                    selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                    if (this.useTab && jsonInputObject.get("value").toString().equals("show_localID")) {
                        // identify named graph with active tab

                        selectWhereBuilder.addWhere("<" + this.mdbEntryID + "#" + jsonInputObject.get("localID").toString() + ">", SCMDBMD.hasDescriptionFormWithRootElement, "?o");

                    } else {

                        selectWhereBuilder.addWhere("<" + this.mdbEntryID + "#" + jsonInputObject.get("localID").toString() + ">", "?p", "?o");

                    }

                    SelectBuilder selectBuilder = new SelectBuilder();

                    selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                    ExprVar exprVar = new ExprVar("o");

                    selectBuilder.addVar(exprVar);

                    selectBuilder.addGraph("<" + this.previousCalculatedNG + ">", selectWhereBuilder);

                    String sparqlQueryString = selectBuilder.buildString();

                    rootComponentOfComposition = connectionToTDB.pullSingleDataFromTDB(workspace, sparqlQueryString, "?o");

                }

                System.out.println("rootComponentOfComposition = " + rootComponentOfComposition);

                Model unionNGModel = ModelFactory.createDefaultModel(), entryComponentsModel = ModelFactory.createDefaultModel();

                for (int j = 0; j < ngs.length(); j++) {

                    unionNGModel = unionNGModel.union(connectionToTDB.pullNamedModelFromTDB(directory, ngs.get(j).toString()));

                }

                ResIterator subIter = unionNGModel.listSubjects();

                while (subIter.hasNext()) {

                    Resource potentialSubject = subIter.next();

                    if (unionNGModel.contains(potentialSubject, RDF.type, OWL2.NamedIndividual) &&
                            !potentialSubject.toString().contains(ApplicationConfigurator.getDomain() + "/resource/")) {

                        Selector tripleSelector = new SimpleSelector(potentialSubject, RDF.type, null, "");

                        StmtIterator tripleStmts = unionNGModel.listStatements(tripleSelector);

                        while (tripleStmts.hasNext()) {

                            Statement stmt = tripleStmts.nextStatement();

                            Resource currSubject = stmt.getSubject();

                            Property currProperty = stmt.getPredicate();

                            Resource currObject;

                            if (stmt.getObject().isURIResource()) {

                                currObject = stmt.getObject().asResource();

                                if (currSubject.equals(potentialSubject)
                                        && currProperty.equals(RDF.type)
                                        && !currObject.equals(OWL2.NamedIndividual)) {

                                    int index;

                                    if (this.numberOfClassInstancesOverlay.has(currObject.toString())) {

                                        index = (this.numberOfClassInstancesOverlay.getInt(currObject.toString()) + 1);

                                        this.numberOfClassInstancesOverlay.put(currObject.toString(), index);

                                    } else {

                                        index = 1;

                                        this.numberOfClassInstancesOverlay.put(currObject.toString(), 1);

                                    }

                                    this.classOverlayMapping = this.classOverlayMapping.put(potentialSubject.toString(), ApplicationConfigurator.getDomain() + "/resource/dummy-overlay#" + currObject.getLocalName() + "_" + index);

                                }

                            }

                        }

                    }

                }

                ModelResourceExchanger modelResourceExchanger = new ModelResourceExchanger();

                unionNGModel = modelResourceExchanger.substituteSubjectIndividualsInModel(unionNGModel, this.classOverlayMapping);

                this.overlayModel.add(unionNGModel);

                if (unionNGModel.isEmpty()) {

                    System.out.println();
                    System.out.println("WARN: The composition for the root " + rootComponentOfComposition + " is empty!");
                    System.out.println("WARN: Maybe update the default composition on the admin page.");
                    System.out.println();

                }

                ResIterator resIter = unionNGModel.listSubjects();

                while (resIter.hasNext()) {

                    Resource entryComponentURI = resIter.next();

                    if (unionNGModel.contains(entryComponentURI, RDF.type, OWL2.NamedIndividual)) {

                        Selector tripleSelector = new SimpleSelector(entryComponentURI, null, null, "");

                        StmtIterator tripleStmts = unionNGModel.listStatements(tripleSelector);

                        while (tripleStmts.hasNext()) {

                            Statement stmt = tripleStmts.nextStatement();

                            Resource currSubject = stmt.getSubject();

                            Property currProperty = stmt.getPredicate();

                            Resource currObject;

                            if (stmt.getObject().isURIResource()) {

                                currObject = stmt.getObject().asResource();

                                if (currSubject.equals(entryComponentURI)
                                        && currProperty.equals(RDF.type)
                                        && !currObject.equals(OWL2.NamedIndividual)) {

                                    Selector classSelector = new SimpleSelector(currObject, null, null, "");

                                    StmtIterator classStmts = unionNGModel.listStatements(classSelector);

                                    Resource classSubject = null;

                                    while (classStmts.hasNext()) {

                                        Statement classStmt = classStmts.nextStatement();

                                        classSubject = classStmt.getSubject();

                                        if ((!classStmt.getObject().equals(OWL2.Class))
                                                && (!classStmt.getPredicate().equals(RDFS.label))
                                                && (!classStmt.getPredicate().equals(RDFS.subClassOf))
                                                && (!classStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                && (!classStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                            entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, classStmt.getPredicate(), classStmt.getObject()));

                                        }

                                        if (showExpanded) {
                                            // add information if a gui component should be expanded

                                            for (int i = 0; i < resourcesToShowExpanded.length(); i++) {

                                                if (ResourceFactory.createResource(resourcesToShowExpanded.get(i).toString()).equals(classSubject)) {

                                                    entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, ResourceFactory.createProperty(propertyForResourcesToShowExpanded), ResourceFactory.createPlainLiteral("true")));

                                                }

                                            }

                                        }

                                    }

                                    if (unionNGModel.contains(null, OWL2.annotatedSource, classSubject)) {

                                        ResIterator axiomsForClassSubject = unionNGModel.listSubjectsWithProperty(OWL2.annotatedSource, classSubject);

                                        while (axiomsForClassSubject.hasNext()) {

                                            Resource axiomClassSubject = axiomsForClassSubject.next();

                                            Selector axiomClassSelector = new SimpleSelector(axiomClassSubject, null, null, "");

                                            StmtIterator axiomClassStmts = unionNGModel.listStatements(axiomClassSelector);

                                            while (axiomClassStmts.hasNext()) {

                                                Statement axiomClassStmt = axiomClassStmts.nextStatement();

                                                if ((!axiomClassStmt.getObject().equals(OWL2.Axiom))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedSource))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                                    entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, axiomClassStmt.getPredicate(), axiomClassStmt.getObject()));

                                                }

                                            }

                                        }

                                    }

                                    if (currSubject.equals(entryComponentURI)
                                            && currProperty.equals(RDF.type)
                                            && currObject.equals(ResourceFactory.createResource(rootComponentOfComposition))) {

                                        rootComponentOfComposition = currSubject.toString();

                                    }

                                }

                            }

                            entryComponentsModel.add(stmt);

                        }

                        if (unionNGModel.contains(null, OWL2.annotatedSource, entryComponentURI)) {

                            ResIterator axiomsForSubject = unionNGModel.listSubjectsWithProperty(OWL2.annotatedSource, entryComponentURI);

                            while (axiomsForSubject.hasNext()) {

                                Resource axiomSubject = axiomsForSubject.next();

                                Selector axiomSelector = new SimpleSelector(axiomSubject, null, null, "");

                                StmtIterator axiomStmts = unionNGModel.listStatements(axiomSelector);

                                while (axiomStmts.hasNext()) {

                                    Statement axiomStmt = axiomStmts.nextStatement();

                                    if ((!axiomStmt.getObject().equals(OWL2.Axiom))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedSource))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedTarget))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                        entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, axiomStmt.getPredicate(), axiomStmt.getObject()));

                                    }

                                }

                            }

                        }

                    }

                }
                ResIterator subjectIter = entryComponentsModel.listSubjects();

                JSONObject componentsToHide = new JSONObject();

                while (subjectIter.hasNext()) {

                    Resource currSubject = subjectIter.nextResource();

                    for (int i = 0; i < activeTargetComponents.length(); i++) {

                        if (currSubject.toString().equals(activeTargetComponents.get(i).toString())) {

                            System.out.println("active component = " + activeTargetComponents.get(i).toString());

                            ResIterator parentOfSubjectIter = entryComponentsModel.listSubjectsWithProperty(SprO.hasEntryComponent, currSubject);

                            while (parentOfSubjectIter.hasNext()) {

                                Resource currSubjectParent = parentOfSubjectIter.nextResource();

                                NodeIterator childrenIter = entryComponentsModel.listObjectsOfProperty(currSubjectParent, SprO.hasEntryComponent);

                                while (childrenIter.hasNext()) {

                                    RDFNode currChildNode = childrenIter.nextNode();

                                    if (currChildNode.isResource()) {

                                        Resource currChild = currChildNode.asResource();

                                        if (!currChild.toString().equals(currSubject.toString())) {

                                            NodeIterator currSubjectPositionIter = entryComponentsModel.listObjectsOfProperty(currSubject, SprO.hasPositionInEntryComponent);

                                            while (currSubjectPositionIter.hasNext()) {

                                                RDFNode currSubjectPositionNode = currSubjectPositionIter.nextNode();

                                                NodeIterator currChildPositionIter = entryComponentsModel.listObjectsOfProperty(currChild, SprO.hasPositionInEntryComponent);

                                                while (currChildPositionIter.hasNext()) {

                                                    RDFNode currChildPositionNode = currChildPositionIter.nextNode();

                                                    if (currChildPositionNode.toString().equals(currSubjectPositionNode.toString())) {

                                                        componentsToHide.append(currSubjectParent.toString(), currChild.toString());

                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                    for (int i = 0; i < activeTabComponents.length(); i++) {

                        if (currSubject.toString().equals(activeTabComponents.get(i).toString())) {

                            entryComponentsModel.add(currSubject, SprO.isActive, ResourceFactory.createPlainLiteral("is-active"));

                        }

                    }

                }

                Iterator<String> componentsToHideKeys = componentsToHide.keys();

                while (componentsToHideKeys.hasNext()) {

                    String currKey = componentsToHideKeys.next();

                    if (componentsToHide.get(currKey) instanceof JSONArray) {

                        JSONArray currChildrenToHide = componentsToHide.getJSONArray(currKey);

                        for (int i = 0; i < currChildrenToHide.length(); i++) {

                            entryComponentsModel.removeAll(ResourceFactory.createResource(currChildrenToHide.get(i).toString()), null, null);

                            entryComponentsModel.removeAll(ResourceFactory.createResource(currKey), null, ResourceFactory.createResource(currChildrenToHide.get(i).toString()));

                        }

                    }

                }

                StmtIterator entryComponentsModelIter = entryComponentsModel.listStatements();

                OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

                JSONObject entryComponents = new JSONObject();

                while (entryComponentsModelIter.hasNext()) {

                    Statement resStmt = entryComponentsModelIter.nextStatement();

                    String currSubject = resStmt.getSubject().toString();

                    entryComponents = outputGenerator
                            .manageProperty(currSubject, resStmt, entryComponents, jsonInputObject, connectionToTDB);

                }

                entryComponents = outputGenerator.reorderEntryComponentsValues(entryComponents);

                Iterator<String> iter = entryComponents.keys();

                outputDataJSON = new JSONArray();

                while (iter.hasNext()) {

                    String currKey = iter.next();

                    JSONObject wrapperJSON = new JSONObject();

                    wrapperJSON.put(currKey, entryComponents.getJSONObject(currKey));

                    outputDataJSON.put(wrapperJSON);

                }

                if (useCompositionFromEntry) {

                    if (compositionFromEntry.equals(SprO.sproVARIABLEThisUserEntryID.toString())) {

                        IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                        String workspace = calculateWorkspaceDirectory(currExecStep);

                        rootComponentOfComposition = individualURI.getThisURIForAnIndividual(rootComponentOfComposition, workspace, connectionToTDB);

                    }

                }

                if (this.classOverlayMapping.has(rootComponentOfComposition)) {

                    rootComponentOfComposition = this.classOverlayMapping.get(rootComponentOfComposition).toString();

                }

                outputDataJSON = outputGenerator.orderOutputJSON(rootComponentOfComposition, outputDataJSON);

            } else if (useUnionOfCompositionsWithParentRoot) {

                System.out.println("rootComponentOfUnionComposition = " + rootComponentOfUnionComposition);

                Model unionNGModel = ModelFactory.createDefaultModel(), entryComponentsModel = ModelFactory.createDefaultModel();

                for (int j = 0; j < ngs.length(); j++) {

                    unionNGModel = unionNGModel.union(connectionToTDB.pullNamedModelFromTDB(directory, ngs.get(j).toString()));

                }

                if (unionNGModel.contains(null, RDF.type, ResourceFactory.createResource(rootComponentOfUnionComposition))) {
                    // case input is an ontology class

                    ResIterator rootComponentIter = unionNGModel.listSubjectsWithProperty(RDF.type, ResourceFactory.createResource(rootComponentOfUnionComposition));

                    while (rootComponentIter.hasNext()) {

                        Resource rootComponentResource = rootComponentIter.nextResource();

                        rootComponentOfUnionComposition = rootComponentResource.toString();

                    }

                    System.out.println("Set rootComponentOfUnionComposition to = " + rootComponentOfUnionComposition);

                } else if (!unionNGModel.contains(ResourceFactory.createResource(rootComponentOfUnionComposition), RDF.type)) {
                    // case unknown root component

                    System.out.println("Error: Can't identify a root in the composition.");

                } else {

                    System.out.println("rootComponentOfUnionComposition = " + rootComponentOfUnionComposition);

                }

                ResIterator subIter = unionNGModel.listSubjects();

                while (subIter.hasNext()) {

                    Resource potentialSubject = subIter.next();

                    if (unionNGModel.contains(potentialSubject, RDF.type, OWL2.NamedIndividual) &&
                            !potentialSubject.toString().contains(ApplicationConfigurator.getDomain() + "/resource/")) {

                        Selector tripleSelector = new SimpleSelector(potentialSubject, RDF.type, null, "");

                        StmtIterator tripleStmts = unionNGModel.listStatements(tripleSelector);

                        while (tripleStmts.hasNext()) {

                            Statement stmt = tripleStmts.nextStatement();

                            Resource currSubject = stmt.getSubject();

                            Property currProperty = stmt.getPredicate();

                            Resource currObject;

                            if (stmt.getObject().isURIResource()) {

                                currObject = stmt.getObject().asResource();

                                if (currSubject.equals(potentialSubject)
                                        && currProperty.equals(RDF.type)
                                        && !currObject.equals(OWL2.NamedIndividual)) {

                                    int index;

                                    if (this.numberOfClassInstancesOverlay.has(currObject.toString())) {

                                        index = (this.numberOfClassInstancesOverlay.getInt(currObject.toString()) + 1);

                                        this.numberOfClassInstancesOverlay.put(currObject.toString(), index);

                                    } else {

                                        index = 1;

                                        this.numberOfClassInstancesOverlay.put(currObject.toString(), 1);

                                    }

                                    this.classOverlayMapping = this.classOverlayMapping.put(potentialSubject.toString(), ApplicationConfigurator.getDomain() + "/resource/dummy-overlay#" + currObject.getLocalName() + "_" + index);

                                }

                            }

                        }

                    }

                }

                ModelResourceExchanger modelResourceExchanger = new ModelResourceExchanger();

                unionNGModel = modelResourceExchanger.substituteSubjectIndividualsInModel(unionNGModel, this.classOverlayMapping);

                this.overlayModel.add(unionNGModel);

                ResIterator resIter = unionNGModel.listSubjects();

                while (resIter.hasNext()) {

                    Resource entryComponentURI = resIter.next();

                    if (unionNGModel.contains(entryComponentURI, RDF.type, OWL2.NamedIndividual)) {

                        Selector tripleSelector = new SimpleSelector(entryComponentURI, null, null, "");

                        StmtIterator tripleStmts = unionNGModel.listStatements(tripleSelector);

                        while (tripleStmts.hasNext()) {

                            Statement stmt = tripleStmts.nextStatement();

                            Resource currSubject = stmt.getSubject();

                            Property currProperty = stmt.getPredicate();

                            Resource currObject;

                            if (stmt.getObject().isURIResource()
                                    && stmt.getPredicate().equals(RDF.type)) {

                                currObject = stmt.getObject().asResource();

                                if (currSubject.equals(entryComponentURI)
                                        && currProperty.equals(RDF.type)
                                        && !currObject.equals(OWL2.NamedIndividual)) {

                                    Selector classSelector = new SimpleSelector(currObject, null, null, "");

                                    StmtIterator classStmts = unionNGModel.listStatements(classSelector);

                                    Resource classSubject = null;

                                    while (classStmts.hasNext()) {

                                        Statement classStmt = classStmts.nextStatement();

                                        classSubject = classStmt.getSubject();

                                        if ((!classStmt.getObject().equals(OWL2.Class))
                                                && (!classStmt.getPredicate().equals(RDFS.label))
                                                && (!classStmt.getPredicate().equals(RDFS.subClassOf))
                                                && (!classStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                && (!classStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                            entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, classStmt.getPredicate(), classStmt.getObject()));

                                        }

                                    }

                                    if (unionNGModel.contains(null, OWL2.annotatedSource, classSubject)) {

                                        ResIterator axiomsForClassSubject = unionNGModel.listSubjectsWithProperty(OWL2.annotatedSource, classSubject);

                                        while (axiomsForClassSubject.hasNext()) {

                                            Resource axiomClassSubject = axiomsForClassSubject.next();

                                            Selector axiomClassSelector = new SimpleSelector(axiomClassSubject, null, null, "");

                                            StmtIterator axiomClassStmts = unionNGModel.listStatements(axiomClassSelector);

                                            while (axiomClassStmts.hasNext()) {

                                                Statement axiomClassStmt = axiomClassStmts.nextStatement();

                                                if ((!axiomClassStmt.getObject().equals(OWL2.Axiom))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedSource))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                                    entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, axiomClassStmt.getPredicate(), axiomClassStmt.getObject()));

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                            entryComponentsModel.add(stmt);

                        }

                        if (unionNGModel.contains(null, OWL2.annotatedSource, entryComponentURI)) {

                            ResIterator axiomsForSubject = unionNGModel.listSubjectsWithProperty(OWL2.annotatedSource, entryComponentURI);

                            while (axiomsForSubject.hasNext()) {

                                Resource axiomSubject = axiomsForSubject.next();

                                Selector axiomSelector = new SimpleSelector(axiomSubject, null, null, "");

                                StmtIterator axiomStmts = unionNGModel.listStatements(axiomSelector);

                                while (axiomStmts.hasNext()) {

                                    Statement axiomStmt = axiomStmts.nextStatement();

                                    if ((!axiomStmt.getObject().equals(OWL2.Axiom))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedSource))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedTarget))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                        entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, axiomStmt.getPredicate(), axiomStmt.getObject()));

                                    }

                                }

                            }

                        }

                    }

                }

                ResIterator subjectIter = entryComponentsModel.listSubjects();

                JSONObject componentsToHide = new JSONObject();

                while (subjectIter.hasNext()) {

                    Resource currSubject = subjectIter.nextResource();

                    for (int i = 0; i < activeTargetComponents.length(); i++) {

                        if (currSubject.toString().equals(activeTargetComponents.get(i).toString())) {

                            System.out.println("active component = " + activeTargetComponents.get(i).toString());

                            ResIterator parentOfSubjectIter = entryComponentsModel.listSubjectsWithProperty(SprO.hasEntryComponent, currSubject);

                            while (parentOfSubjectIter.hasNext()) {

                                Resource currSubjectParent = parentOfSubjectIter.nextResource();

                                NodeIterator childrenIter = entryComponentsModel.listObjectsOfProperty(currSubjectParent, SprO.hasEntryComponent);

                                while (childrenIter.hasNext()) {

                                    RDFNode currChildNode = childrenIter.nextNode();

                                    if (currChildNode.isResource()) {

                                        Resource currChild = currChildNode.asResource();

                                        if (!currChild.toString().equals(currSubject.toString())) {

                                            NodeIterator currSubjectPositionIter = entryComponentsModel.listObjectsOfProperty(currSubject, SprO.hasPositionInEntryComponent);

                                            while (currSubjectPositionIter.hasNext()) {

                                                RDFNode currSubjectPositionNode = currSubjectPositionIter.nextNode();

                                                NodeIterator currChildPositionIter = entryComponentsModel.listObjectsOfProperty(currChild, SprO.hasPositionInEntryComponent);

                                                while (currChildPositionIter.hasNext()) {

                                                    RDFNode currChildPositionNode = currChildPositionIter.nextNode();

                                                    if (currChildPositionNode.toString().equals(currSubjectPositionNode.toString())) {

                                                        componentsToHide.append(currSubjectParent.toString(), currChild.toString());

                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                    for (int i = 0; i < activeTabComponents.length(); i++) {

                        if (currSubject.toString().equals(activeTabComponents.get(i).toString())) {

                            entryComponentsModel.add(currSubject, SprO.isActive, ResourceFactory.createPlainLiteral("is-active"));

                        }

                    }

                }

                Iterator<String> componentsToHideKeys = componentsToHide.keys();

                while (componentsToHideKeys.hasNext()) {

                    String currKey = componentsToHideKeys.next();

                    if (componentsToHide.get(currKey) instanceof JSONArray) {

                        JSONArray currChildrenToHide = componentsToHide.getJSONArray(currKey);

                        for (int i = 0; i < currChildrenToHide.length(); i++) {

                            entryComponentsModel.removeAll(ResourceFactory.createResource(currChildrenToHide.get(i).toString()), null, null);

                            entryComponentsModel.removeAll(ResourceFactory.createResource(currKey), null, ResourceFactory.createResource(currChildrenToHide.get(i).toString()));

                        }

                    }

                }

                StmtIterator entryComponentsModelIter = entryComponentsModel.listStatements();

                OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

                JSONObject entryComponents = new JSONObject();

                while (entryComponentsModelIter.hasNext()) {

                    Statement resStmt = entryComponentsModelIter.nextStatement();

                    String currSubject = resStmt.getSubject().toString();

                    entryComponents = outputGenerator
                            .manageProperty(currSubject, resStmt, entryComponents, jsonInputObject, connectionToTDB);

                }

                this.parentRootExist = true;

                this.parentRootPosition = position;

                this.parentRoot = rootComponentOfUnionComposition;

                this.parentComponents = entryComponents;

            } else if (useComponentFromOntology) {

                System.out.println("componentOfOntology = " + componentOfOntology);

                JSONArray resourcesToCheck = new JSONArray();

                Model entryComponentsModel = ModelFactory.createDefaultModel();

                resourcesToCheck.put(componentOfOntology);

                while (!resourcesToCheck.isNull(0)) {

                    Model individualsModel = findTriple(resourcesToCheck.get(0).toString(), directory, connectionToTDB);

                    if (individualsModel.contains(ResourceFactory.createResource(resourcesToCheck.get(0).toString()), RDF.type, OWL2.NamedIndividual)) {

                        Selector tripleSelector = new SimpleSelector(ResourceFactory.createResource(resourcesToCheck.get(0).toString()), null, null, "");

                        StmtIterator tripleStmts = individualsModel.listStatements(tripleSelector);

                        while (tripleStmts.hasNext()) {

                            Statement currStatement = tripleStmts.nextStatement();

                            Property currProperty = currStatement.getPredicate();

                            Resource currObject;

                            if (currStatement.getObject().isURIResource()) {

                                currObject = currStatement.getObject().asResource();

                                if (currProperty.equals(RDF.type)
                                        && !currObject.equals(OWL2.NamedIndividual)) {

                                    Model classModel = findTriple(currObject.toString(), directory, connectionToTDB);

                                    StmtIterator classStmts = classModel.listStatements();

                                    while (classStmts.hasNext()) {

                                        Statement classStmt = classStmts.nextStatement();

                                        if ((!classStmt.getObject().equals(OWL2.Class))
                                                && (!classStmt.getPredicate().equals(RDFS.label))
                                                && (!classStmt.getPredicate().equals(RDFS.subClassOf))) {

                                            entryComponentsModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(resourcesToCheck.get(0).toString()), classStmt.getPredicate(), classStmt.getObject()));

                                        }

                                    }

                                    Model axiomClassModel = findAxiomTriple(currObject.toString(), directory, connectionToTDB);

                                    StmtIterator axiomClassStmts = axiomClassModel.listStatements();

                                    while (axiomClassStmts.hasNext()) {

                                        Statement axiomClassStmt = axiomClassStmts.nextStatement();

                                        if ((!axiomClassStmt.getObject().equals(OWL2.Axiom))
                                                && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedSource))
                                                && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                            entryComponentsModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(resourcesToCheck.get(0).toString()), axiomClassStmt.getPredicate(), axiomClassStmt.getObject()));

                                        }

                                    }

                                } else if (currProperty.equals(SprO.hasEntryComponent)) {
                                    // add children to loop

                                    resourcesToCheck.put(currObject.toString());

                                }

                            }

                            entryComponentsModel.add(currStatement);

                        }

                    }

                    Model axiomIndividualsModel = findAxiomTriple(resourcesToCheck.get(0).toString(), directory, connectionToTDB);

                    StmtIterator axiomIndividualStmts = axiomIndividualsModel.listStatements();

                    while (axiomIndividualStmts.hasNext()) {

                        Statement axiomIndividualStmt = axiomIndividualStmts.nextStatement();

                        if ((!axiomIndividualStmt.getObject().equals(OWL2.Axiom))
                                && (!axiomIndividualStmt.getPredicate().equals(OWL2.annotatedSource))
                                && (!axiomIndividualStmt.getPredicate().equals(OWL2.annotatedTarget))
                                && (!axiomIndividualStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                            entryComponentsModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(resourcesToCheck.get(0).toString()), axiomIndividualStmt.getPredicate(), axiomIndividualStmt.getObject()));

                        }

                    }

                    resourcesToCheck.remove(0);

                }

                ResIterator subjectIter = entryComponentsModel.listSubjects();

                JSONObject componentsToHide = new JSONObject();

                while (subjectIter.hasNext()) {

                    Resource currSubject = subjectIter.nextResource();

                    for (int i = 0; i < activeTargetComponents.length(); i++) {

                        if (currSubject.toString().equals(activeTargetComponents.get(i).toString())) {

                            System.out.println("active component = " + activeTargetComponents.get(i).toString());

                            ResIterator parentOfSubjectIter = entryComponentsModel.listSubjectsWithProperty(SprO.hasEntryComponent, currSubject);

                            while (parentOfSubjectIter.hasNext()) {

                                Resource currSubjectParent = parentOfSubjectIter.nextResource();

                                NodeIterator childrenIter = entryComponentsModel.listObjectsOfProperty(currSubjectParent, SprO.hasEntryComponent);

                                while (childrenIter.hasNext()) {

                                    RDFNode currChildNode = childrenIter.nextNode();

                                    if (currChildNode.isResource()) {

                                        Resource currChild = currChildNode.asResource();

                                        if (!currChild.toString().equals(currSubject.toString())) {

                                            NodeIterator currSubjectPositionIter = entryComponentsModel.listObjectsOfProperty(currSubject, SprO.hasPositionInEntryComponent);

                                            while (currSubjectPositionIter.hasNext()) {

                                                RDFNode currSubjectPositionNode = currSubjectPositionIter.nextNode();

                                                NodeIterator currChildPositionIter = entryComponentsModel.listObjectsOfProperty(currChild, SprO.hasPositionInEntryComponent);

                                                while (currChildPositionIter.hasNext()) {

                                                    RDFNode currChildPositionNode = currChildPositionIter.nextNode();

                                                    if (currChildPositionNode.toString().equals(currSubjectPositionNode.toString())) {

                                                        componentsToHide.append(currSubjectParent.toString(), currChild.toString());

                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                    for (int i = 0; i < activeTabComponents.length(); i++) {

                        if (currSubject.toString().equals(activeTabComponents.get(i).toString())) {

                            entryComponentsModel.add(currSubject, SprO.isActive, ResourceFactory.createPlainLiteral("is-active"));
                            // is active

                        }

                    }

                }

                Iterator<String> componentsToHideKeys = componentsToHide.keys();

                while (componentsToHideKeys.hasNext()) {

                    String currKey = componentsToHideKeys.next();

                    if (componentsToHide.get(currKey) instanceof JSONArray) {

                        JSONArray currChildrenToHide = componentsToHide.getJSONArray(currKey);

                        for (int i = 0; i < currChildrenToHide.length(); i++) {

                            entryComponentsModel.removeAll(ResourceFactory.createResource(currChildrenToHide.get(i).toString()), null, null);

                            entryComponentsModel.removeAll(ResourceFactory.createResource(currKey), null, ResourceFactory.createResource(currChildrenToHide.get(i).toString()));

                        }

                    }

                }

                StmtIterator entryComponentsModelIter = entryComponentsModel.listStatements();

                OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

                JSONObject entryComponents = new JSONObject();

                while (entryComponentsModelIter.hasNext()) {

                    Statement resStmt = entryComponentsModelIter.nextStatement();

                    entryComponents = outputGenerator
                            .manageProperty(resStmt.getSubject().toString(), resStmt, entryComponents,
                                    jsonInputObject, connectionToTDB);

                }

                entryComponents = outputGenerator.reorderEntryComponentsValues(entryComponents);

                Iterator<String> iter = entryComponents.keys();

                while (iter.hasNext()) {

                    String currKey = iter.next();

                    JSONObject wrapperJSON = new JSONObject();

                    wrapperJSON.put(currKey, entryComponents.getJSONObject(currKey));

                    outputDataJSON.put(wrapperJSON);

                }

                outputDataJSON = outputGenerator.orderOutputJSON(componentOfOntology, outputDataJSON);

            }

        }

        if (!outputDataJSON.isNull(0)) {

            if (currComponentObject.has("compositionForMDBHyperlink")) {// todo change key to "data" at a later point

                JSONArray compositionForMDBHyperlink = currComponentObject.getJSONArray("compositionForMDBHyperlink");

                compositionForMDBHyperlink.put(position - 1, outputDataJSON.getJSONObject(0));

                currComponentObject.put("compositionForMDBHyperlink", compositionForMDBHyperlink);

            } else {

                JSONArray compositionForMDBHyperlink = new JSONArray();

                compositionForMDBHyperlink.put(position - 1, outputDataJSON.getJSONObject(0));

                currComponentObject.put("compositionForMDBHyperlink", compositionForMDBHyperlink);

            }

        }

        return currComponentObject;
    }

    /**
     * This method defines variables for a later use.
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public JSONObject executionStepDefineVariables(JSONObject currComponentObject, JSONObject jsonInputObject,
                                                   JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        boolean useInKnownSubsequentWA = false;

        JSONObject keywordsToTransfer = new JSONObject();

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.useInKnownSubsequentWorkflowActionBOOLEAN.toString())) {

                useInKnownSubsequentWA = true;

            }

        }

        String identifiedKey = "", identifiedValue = "", identifiedResourcesKey = "";

        boolean addResourceToList = false, deleteResourceFromList = false;

        JSONArray resourceList = new JSONArray(), resourcesToAdd = new JSONArray(), resourcesToDelete = new JSONArray();

        for (int i = 0; i < currExecStep.length(); i++) {

            boolean useAsInput = useObjectAsInput(currExecStep.getJSONObject(i).get("property").toString(), connectionToTDB);

            if (useAsInput) {

                // calculate the corresponding KEYWORD resource for the KEYWORD property
                String uriOfIndividual = getKeywordIndividualFromProperty(currExecStep.getJSONObject(i).get("property").toString(), connectionToTDB);

                if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEThisEntryID.toString())) {

                    this.infoInput.put(uriOfIndividual, this.mdbEntryID);
                    this.identifiedResources.put(uriOfIndividual, this.mdbEntryID);

                } else if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEThisCoreID.toString())) {

                    this.infoInput.put(uriOfIndividual, this.mdbCoreID);
                    this.identifiedResources.put(uriOfIndividual, this.mdbCoreID);

                } else if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEEmpty.toString())) {

                    this.infoInput.put(currExecStep.getJSONObject(i).get("object").toString(), currExecStep.getJSONObject(i).get("object").toString());
                    this.identifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), currExecStep.getJSONObject(i).get("object").toString());

                } else if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEResourceDescribedWithThisDescriptionFormComposition.toString())) {

                    if (jsonInputObject.has("partID")) {

                        this.infoInput.put(uriOfIndividual, jsonInputObject.get("mdbentryid").toString() + "#" + jsonInputObject.get("partID").toString());
                        this.identifiedResources.put(uriOfIndividual, jsonInputObject.get("mdbentryid").toString() + "#" + jsonInputObject.get("partID").toString());

                    } else {

                        System.out.println("WARN: There is no partID in Input!");

                    }

                } else {

                    String value = currExecStep.getJSONObject(i).get("object").toString();

                    Iterator<String> keyIterator = this.generatedResources.keys();

                    while (keyIterator.hasNext()) {

                        String currKey = keyIterator.next();

                        // get local name of a key
                        String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                        if (value.contains(localNameOfKey)) {
                            // get ng from generated resources

                            value = this.generatedResources.get(currKey).toString();

                        }

                    }

                    if (jsonInputObject.has("localIDs")) {

                        JSONArray currJSONArray = jsonInputObject.getJSONArray("localIDs");

                        for (int j = 0; j < currJSONArray.length(); j++) {

                            JSONObject currJSONObject = currJSONArray.getJSONObject(j);

                            if (currJSONObject.has("keyword")) {

                                if (ResourceFactory.createResource(value).getLocalName().equals(currJSONObject.get("keyword").toString()) &&
                                        jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                    if (EmailValidator.getInstance().isValid(currJSONObject.get("value").toString())) {

                                        value = "mailto:" + currJSONObject.get("value").toString();

                                    } else {

                                        value = currJSONObject.get("value").toString();

                                    }

                                } else if (ResourceFactory.createResource(value).getLocalName().equals(currJSONObject.get("keyword").toString()) &&
                                        jsonInputObject.has("useKeywordsFromComposition")) {

                                    if (jsonInputObject.get("useKeywordsFromComposition").toString().equals("true")) {

                                        if (currJSONObject.get("value") instanceof JSONObject) {

                                            value = currJSONObject.getJSONObject("value").get("resource").toString();

                                        } else if (currJSONObject.get("value") instanceof String) {

                                            value = currJSONObject.get("value").toString();

                                        }

                                    }

                                }

                            }

                        }

                    }

                    // check if the variable was defined before and update the variable in this case
                    if (this.identifiedResources.has(uriOfIndividual)) {

                        this.identifiedResources.put(uriOfIndividual, value);

                    } else if (this.generatedResources.has(uriOfIndividual)) {

                        this.generatedResources.put(uriOfIndividual, value);

                    } else {

                        this.infoInput.put(uriOfIndividual, value);

                    }

                    System.out.println();
                    System.out.println("uriOfIndividual = " + uriOfIndividual);
                    System.out.println();
                    System.out.println("value = " + value);

                    if (useInKnownSubsequentWA) {

                        keywordsToTransfer.put(uriOfIndividual, value);

                    }

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.identifiedWorkflowAction.toString())) {

                jsonInputObject.put(ResourceFactory.createResource(currExecStep.getJSONObject(i).get("property").toString()).getLocalName(), currExecStep.getJSONObject(i).get("object").toString());

                System.out.println();
                System.out.println("save keyword for later = " + ResourceFactory.createResource(currExecStep.getJSONObject(i).get("property").toString()).getLocalName());
                System.out.println();

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.identifiedIndividualDefinesSPrOVariableResource.toString())) {

                identifiedKey = currExecStep.getJSONObject(i).get("object").toString();

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.identifyThisEntrySSpecificIndividualOf.toString())) {

                if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEThisEntryVersionNumber.toString())) {

                    if (this.hasExecutionStepFocus) {

                        identifiedValue = this.executionStepFocus.substring((this.executionStepFocus.lastIndexOf("-") + 1));

                    } else {

                        identifiedValue = this.currentFocus.substring((this.currentFocus.lastIndexOf("-") + 1));

                    }

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.addResourceToList.toString())) {

                addResourceToList = true;

                identifiedResourcesKey = currExecStep.getJSONObject(i).get("object").toString();

                resourceList = this.identifiedResources.getJSONArray(identifiedResourcesKey);


            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.resourceToBeAddedToAList.toString())) {

                String keywordValue = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                resourcesToAdd.put(keywordValue);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.deleteResourceFromList.toString())) {

                deleteResourceFromList = true;

                identifiedResourcesKey = currExecStep.getJSONObject(i).get("object").toString();

                resourceList = this.identifiedResources.getJSONArray(identifiedResourcesKey);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.resourceToBeDeletedFromAList.toString())) {

                String keywordValue = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                resourcesToDelete.put(keywordValue);

            }

        }

        if (addResourceToList) {

            for (int i = 0; i < resourcesToAdd.length(); i++) {

                resourceList.put(resourcesToAdd.get(i).toString());

            }

            System.out.println("Added the following resources to a keyword list: " + resourcesToAdd);

            this.identifiedResources.put(identifiedResourcesKey, resourceList);

        }

        if (deleteResourceFromList) {

            for (int i = resourceList.length() - 1; i >= 0; i--) {

                for (int j = 0; j < resourcesToDelete.length(); j++) {

                    if (resourceList.get(i).toString().equals(resourcesToDelete.get(j).toString())) {

                        resourceList.remove(i);

                    }

                }

            }

            System.out.println("Removed the following resources from a keyword list: " + resourcesToDelete);

            this.identifiedResources.put(identifiedResourcesKey, resourceList);

        }

        if (identifiedKey.length() > 0) {

            System.out.println("identifiedKey = " + identifiedKey);
            System.out.println("identifiedValue = " + identifiedValue);

            this.identifiedResources.put(identifiedKey, identifiedValue);

        }

        currComponentObject.put("use_in_known_subsequent_WA", keywordsToTransfer);

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }

    /**
     * This method removes statement(s) in a jena tdb
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return additional modified information about the current object
     */
    public JSONObject executionStepDeleteMultipleTripleStatements(JSONObject currComponentObject,
                                                                  JSONObject jsonInputObject, JSONArray currExecStep,
                                                                  JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        boolean listExist = false;

        JSONArray objectsJSONArray = new JSONArray(), subjectsJSONArray = new JSONArray();

        for (int i = currExecStep.length() - 1; i >= 0; i--) {

            boolean removeCurrID = false;

            if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.subjectList.toString())) {

                subjectsJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                listExist = true;

                removeCurrID = true;

            }

            if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.objectList.toString())) {

                objectsJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                listExist = true;

                removeCurrID = true;

            }

            if (removeCurrID) {
                // remove the list information

                currExecStep.remove(i);

            }

        }

        if (listExist) {

            if (subjectsJSONArray.length() > 0) {

                System.out.println("length of currSubjectList = " + subjectsJSONArray.length());

                for (int i = 0; i < subjectsJSONArray.length(); i++) {

                    JSONObject currSubjectJSONObject = new JSONObject();

                    currSubjectJSONObject.put("property", SprO.subjectSOCCOMAS.toString());

                    currSubjectJSONObject.put("object", subjectsJSONArray.get(i).toString());

                    currExecStep.put(currSubjectJSONObject);

                    currComponentObject = executionStepDeleteMultipleTripleStatements(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

                    currExecStep.remove(currExecStep.length() - 1);

                }


            } else if (objectsJSONArray.length() > 0) {

                System.out.println("length of currObjectList = " + objectsJSONArray.length());

                for (int i = 0; i < objectsJSONArray.length(); i++) {

                    JSONObject currObjectJSONObject = new JSONObject();

                    currObjectJSONObject.put("property", SprO.objectSOCCOMAS.toString());

                    currObjectJSONObject.put("object", objectsJSONArray.get(i).toString());

                    currExecStep.put(currObjectJSONObject);

                    currComponentObject = executionStepDeleteMultipleTripleStatements(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

                    currExecStep.remove(currExecStep.length() - 1);

                }

            } else {

                System.out.println("WARN: The object list and the subject list is empty.");

            }

        } else {

            boolean deleteWithProperty = false, deleteTriplesSubject = false, deleteTriplesObject = false,
                    deleteTriplesWithCopiedSubject = false, deleteTriplesWithCopiedObject = false;
            String subject = "", property = "", object = "", ng ="", directory = "";

            for (int i = 0; i < currExecStep.length(); i++) {

                if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())) {

                    JSONObject dataToFindObjectInTDB = new JSONObject();

                    subject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                    deleteTriplesSubject = true;

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {

                    JSONObject dataToFindObjectInTDB = new JSONObject();

                    subject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                    deleteTriplesWithCopiedSubject = true;

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())) {

                    JSONObject dataToFindObjectInTDB = new JSONObject();

                    object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, "r", connectionToTDB);

                    deleteTriplesObject = true;

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectCopiedIndividualOf.toString())) {

                    JSONObject dataToFindObjectInTDB = new JSONObject();

                    object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, "r", connectionToTDB);

                    deleteTriplesWithCopiedObject = true;

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())
                        || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                    ng = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                    directory = calculateWorkspaceDirectory(currExecStep);

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.propertySOCCOMAS.toString())) {

                    property = calculateProperty(currExecStep);

                    deleteWithProperty = true;

                }

            }

            if (deleteTriplesSubject) {

                Model mdbCompositionModel = connectionToTDB.pullNamedModelFromTDB(directory, ng);

                Selector subjectSelector;

                if (deleteWithProperty) {

                    subjectSelector = new SimpleSelector(ResourceFactory.createResource(subject), ResourceFactory.createProperty(property), null, null);

                } else {

                    subjectSelector = new SimpleSelector(ResourceFactory.createResource(subject), null, null, null);

                }

                StmtIterator subjectStmtIter = mdbCompositionModel.listStatements(subjectSelector);

                System.out.println("length before = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").length());

                while (subjectStmtIter.hasNext()) {

                    Statement currStmt = subjectStmtIter.nextStatement();

                    currComponentObject.getJSONObject("input_data").append("subject", subject);

                    if (property.equals("")) {

                        currComponentObject.getJSONObject("input_data").append("property", currStmt.getPredicate().toString());

                    } else {

                        currComponentObject.getJSONObject("input_data").append("property", property);

                    }

                    if (currStmt.getObject().isLiteral()) {

                        currComponentObject.getJSONObject("input_data").append("object_data", currStmt.getObject().asLiteral().toString());

                        currComponentObject.getJSONObject("input_data").append("object_type", "l");

                    } else {

                        currComponentObject.getJSONObject("input_data").append("object_data", currStmt.getObject().asResource().toString());

                        currComponentObject.getJSONObject("input_data").append("object_type", "r");

                    }

                    currComponentObject.getJSONObject("input_data").append("ng", ng);

                    currComponentObject.getJSONObject("input_data").append("directory", directory);

                    currComponentObject.getJSONObject("input_data").append("operation", "d");

                }

                System.out.println("length behind = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").length());

            } else if (deleteTriplesWithCopiedSubject) {

                JSONArray subjectsJSON = currComponentObject.getJSONObject("input_data").getJSONArray("subject");

                if (deleteWithProperty) {

                    JSONArray propertyJSON = currComponentObject.getJSONObject("input_data").getJSONArray("property");

                    for (int j = (subjectsJSON.length() - 1); j >= 0; j--) {

                        if (subjectsJSON.get(j).toString().equals(subject)
                                &&  propertyJSON.get(j).toString().equals(property)) {

                            currComponentObject.getJSONObject("input_data").getJSONArray("subject").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("property").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("object_data").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("object_type").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("ng").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("directory").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("operation").remove(j);

                        }

                    }


                } else {

                    for (int j = (subjectsJSON.length() - 1); j >= 0; j--) {

                        if (subjectsJSON.get(j).toString().equals(subject)) {

                            currComponentObject.getJSONObject("input_data").getJSONArray("subject").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("property").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("object_data").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("object_type").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("ng").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("directory").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("operation").remove(j);

                        }

                    }

                }

            } else if (deleteTriplesObject) {

                Model mdbCompositionModel = connectionToTDB.pullNamedModelFromTDB(directory, ng);

                Selector objectSelector;

                String objectType = calculateObjectTypeForAnnotationProperty(object, "a");

                if (deleteWithProperty) {

                    if (objectType.equals("r")) {

                        objectSelector = new SimpleSelector(null, ResourceFactory.createProperty(property), ResourceFactory.createResource(object));

                    } else {

                        objectSelector = new SimpleSelector(null, ResourceFactory.createProperty(property), object, null);

                    }

                } else {

                    if (objectType.equals("r")) {

                        objectSelector = new SimpleSelector(null, null, ResourceFactory.createResource(object));

                    } else {

                        objectSelector = new SimpleSelector(null, null, object, null);

                    }



                }

                StmtIterator objectStmtIter = mdbCompositionModel.listStatements(objectSelector);

                if (currComponentObject.has("input_data")) {

                    if (currComponentObject.getJSONObject("input_data").has("subject")) {

                        System.out.println("length before = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").length());

                    } else {

                        System.out.println("length before = 0");

                    }

                } else {
                    // no other statement was generated yet

                    System.out.println("length before = 0");

                }

                while (objectStmtIter.hasNext()) {

                    Statement currStmt = objectStmtIter.nextStatement();

                    Property currProp = currStmt.getPredicate();

                    if (currProp.equals(OWL2.annotatedSource)) {
                        // delete axioms of the component

                        StmtIterator axiomIter = currStmt.getSubject().listProperties();

                        while (axiomIter.hasNext()) {

                            Statement currAxiomStmt = axiomIter.nextStatement();

                            currComponentObject.getJSONObject("input_data").append("subject", currAxiomStmt.getSubject().toString());

                            if (property.equals("")) {

                                currComponentObject.getJSONObject("input_data").append("property", currProp.toString());

                            } else {

                                currComponentObject.getJSONObject("input_data").append("property", property);

                            }

                            if (currStmt.getObject().isLiteral()) {

                                currComponentObject.getJSONObject("input_data").append("object_data", currStmt.getObject().asLiteral().toString());

                                currComponentObject.getJSONObject("input_data").append("object_type", "l");

                            } else {

                                currComponentObject.getJSONObject("input_data").append("object_data", currStmt.getObject().asResource().toString());

                                currComponentObject.getJSONObject("input_data").append("object_type", "r");

                            }

                            currComponentObject.getJSONObject("input_data").append("ng", ng);

                            currComponentObject.getJSONObject("input_data").append("directory", directory);

                            currComponentObject.getJSONObject("input_data").append("operation", "d");

                        }

                    }

                    currComponentObject.getJSONObject("input_data").append("subject", currStmt.getSubject().toString());

                    currComponentObject.getJSONObject("input_data").append("property", currStmt.getPredicate().toString());

                    currComponentObject.getJSONObject("input_data").append("object_data", object);

                    currComponentObject.getJSONObject("input_data").append("object_type", "r");
                    // is definitive a resource, because it is the part to delete

                    currComponentObject.getJSONObject("input_data").append("ng", ng);

                    currComponentObject.getJSONObject("input_data").append("directory", directory);

                    currComponentObject.getJSONObject("input_data").append("operation", "d");

                }

                System.out.println("length behind = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").length());

            } else if (deleteTriplesWithCopiedObject) {

                JSONArray objectsJSON = currComponentObject.getJSONObject("input_data").getJSONArray("object_data");

                if (deleteWithProperty) {

                    JSONArray propertyJSON = currComponentObject.getJSONObject("input_data").getJSONArray("property");

                    for (int j = (objectsJSON.length() - 1); j >= 0; j--) {

                        if (objectsJSON.get(j).toString().equals(object)
                                &&  propertyJSON.get(j).toString().equals(property)) {

                            currComponentObject.getJSONObject("input_data").getJSONArray("subject").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("property").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("object_data").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("object_type").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("ng").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("directory").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("operation").remove(j);

                        }

                    }

                } else {

                    for (int j = (objectsJSON.length() - 1); j >= 0; j--) {

                        if (objectsJSON.get(j).toString().equals(object)) {

                            currComponentObject.getJSONObject("input_data").getJSONArray("subject").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("property").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("object_data").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("object_type").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("ng").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("directory").remove(j);
                            currComponentObject.getJSONObject("input_data").getJSONArray("operation").remove(j);

                        }

                    }

                }

            }

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }


    /**
     * This method removes named graphs in a jena tdb
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return additional modified information about the current object
     */
    private JSONObject deleteNamedGraphs(JSONObject currComponentObject, JSONObject jsonInputObject,
                                         JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {
        String directory = "";
        JSONArray ngs = new JSONArray();
        JSONArray deleteNamedGraphs = new JSONArray();

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())
                    || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                String ng = calculateNGWithMultipleInput(currExecStep.getJSONObject(i).get("property").toString(), currExecStep.getJSONObject(i).get("object").toString(), currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                if (!ng.equals(SprO.sproVARIABLEEmpty.toString())) {

                    ngs.put(ng);

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                directory = calculateWorkspaceDirectory(currExecStep);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInAllNamedGraphsThisEntrySSpecificIndividualOfOfThisSPrOVariableList.toString())) {

                JSONArray classListJSON = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                String individualDirectory = calculateWorkspaceDirectory(currExecStep);

                for (int j = 0; j  < classListJSON.length(); j++) {

                    IndividualURI individualURI;

                    if (this.hasExecutionStepFocus) {

                        individualURI = new IndividualURI(this.executionStepFocus);

                    } else {

                        individualURI = new IndividualURI(this.currentFocus);

                    }

                    JSONArray currNGSJSON = individualURI.getIndividualURISForAClass(classListJSON.get(j).toString(), individualDirectory, connectionToTDB);

                    if (currNGSJSON.length() > 0) {

                        for (int k = 0; k < currNGSJSON.length(); k++) {

                            ngs.put(currNGSJSON.get(k).toString());

                        }

                    }

                }

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInMultipleNamedGraphsThisEntrySSpecificIndividualsOf.toString())) {

                String individualDirectory = calculateWorkspaceDirectory(currExecStep);

                IndividualURI individualURI;

                if (this.hasExecutionStepFocus) {

                    individualURI = new IndividualURI(this.executionStepFocus);

                } else {

                    individualURI = new IndividualURI(this.currentFocus);

                }

                JSONArray currNGSJSON = individualURI.getIndividualURISForAClass(currExecStep.getJSONObject(i).get("object").toString(), individualDirectory, connectionToTDB);

                if (currNGSJSON.length() > 0) {

                    for (int j = 0; j < currNGSJSON.length(); j++) {

                        ngs.put(currNGSJSON.get(j).toString());

                    }

                }

            }

        }

        for (int i = 0; i < ngs.length(); i++) {

            JSONObject currNGJSONObject = new JSONObject();

            currNGJSONObject.put("ng", ngs.get(i).toString());

            currNGJSONObject.put("directory", directory);

            deleteNamedGraphs.put(currNGJSONObject);

        }

        System.out.println("deleteNamedGraphs = " + deleteNamedGraphs);

        if (!currComponentObject.has("input_data")) {
            // no other statement was generated yet

            JSONObject currInputDataObject = new JSONObject();

            currInputDataObject.put("deleteNamedGraphs", deleteNamedGraphs);

            currComponentObject.put("input_data", currInputDataObject);

        } else {

            if (!currComponentObject.getJSONObject("input_data").has("deleteNamedGraphs")) {

                currComponentObject.getJSONObject("input_data").put("deleteNamedGraphs", deleteNamedGraphs);

            } else {

                for (int i = 0; i < deleteNamedGraphs.length(); i++) {

                    currComponentObject.getJSONObject("input_data").append("deleteNamedGraphs", deleteNamedGraphs.getJSONObject(i));

                }

            }

        }

        return currComponentObject;

    }

    /**
     * This method removes named graphs in a jena tdb
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return additional modified information about the current object
     */
    public JSONObject executionStepDeleteNamedGraphs(JSONObject currComponentObject, JSONObject jsonInputObject,
                                                     JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        if (this.multipleExecutionStepFocus) {

            if (this.executionStepFocuses.length() > 0) {

                for (int i = 0; i < this.executionStepFocuses.length(); i++) {

                    SOCCOMASIDFinder soccomasIDFinder = new SOCCOMASIDFinder(this.executionStepFocuses.get(i).toString(), connectionToTDB);

                    if (!soccomasIDFinder.hasMDBEntryID()
                            && !soccomasIDFinder.hasMDBCoreID()
                            && !soccomasIDFinder.hasMDBUEID()) {

                        SOCCOMASIDChecker mdbIDChecker = new SOCCOMASIDChecker();

                        boolean valueIsMDBID = mdbIDChecker.isMDBID(this.executionStepFocuses.get(i).toString(), connectionToTDB);

                        if (valueIsMDBID) {

                            this.executionStepFocus = this.executionStepFocuses.get(i).toString();

                        }


                    } else {

                        if (soccomasIDFinder.hasMDBEntryID()) {

                            this.executionStepFocus = soccomasIDFinder.getMDBEntryID();

                        }

                        if (!soccomasIDFinder.hasMDBEntryID()
                                && soccomasIDFinder.hasMDBCoreID()) {

                            this.executionStepFocus = soccomasIDFinder.getMDBCoreID();

                        }

                        if (!soccomasIDFinder.hasMDBEntryID()
                                && !soccomasIDFinder.hasMDBCoreID()
                                && soccomasIDFinder.hasMDBUEID()) {

                            this.executionStepFocus = soccomasIDFinder.getMDBUEID();

                        }

                    }

                    deleteNamedGraphs(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

                }

            }

            this.multipleExecutionStepFocus = false;

        } else {

            deleteNamedGraphs(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }


    /**
     * This method removes statement(s) in a jena tdb
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return additional modified information about the current object
     */
    public JSONObject executionStepDeletePartOfComposition(JSONObject currComponentObject, JSONObject jsonInputObject,
                                                           JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        String ng = "", directory = "";
        boolean deleteMDBEntryComponentWithChildren = false, listExist = false;

        JSONArray mdbEntryComponentsListWithChildren = new JSONArray(), deleteMDBEntryComponentWithChildrenJSONArray = new JSONArray();

        for (int i = currExecStep.length() - 1; i >= 0; i--) {

            boolean removeCurrID = false;

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())
                    || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                ng = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                directory = calculateWorkspaceDirectory(currExecStep);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.deleteEntryComponentWithAllOfItsChildren.toString())) {

                if (checkValueOfKeywordIsJSONArray(currExecStep.getJSONObject(i).get("object").toString())) {

                    mdbEntryComponentsListWithChildren = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                } else {

                    mdbEntryComponentsListWithChildren.put(calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString()));

                }

                deleteMDBEntryComponentWithChildren = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.deleteEntryComponentWithAllOfItsChildrenList.toString())) {

                deleteMDBEntryComponentWithChildrenJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                listExist = true;

                removeCurrID = true;

            }

            if (removeCurrID) {
                // remove the list information

                currExecStep.remove(i);

            }

        }

        if (listExist) {

            if (deleteMDBEntryComponentWithChildrenJSONArray.length() > 0) {

                System.out.println("length of deleteMDBEntryComponentWithChildrenJSONArray = " + deleteMDBEntryComponentWithChildrenJSONArray.length());

                for (int i = 0; i < deleteMDBEntryComponentWithChildrenJSONArray.length(); i++) {

                    JSONObject currSubjectJSONObject = new JSONObject();

                    currSubjectJSONObject.put("property", SprO.deleteEntryComponentWithAllOfItsChildren.toString());

                    currSubjectJSONObject.put("object", deleteMDBEntryComponentWithChildrenJSONArray.get(i).toString());

                    currExecStep.put(currSubjectJSONObject);

                    currComponentObject = executionStepDeletePartOfComposition(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

                    currExecStep.remove(currExecStep.length() - 1);

                }

            } else {

                System.out.println("WARN: The 'MDB component with all its children' list is empty.");

            }

        } else {

            if (deleteMDBEntryComponentWithChildren) {

                Model mdbComposition = connectionToTDB.pullNamedModelFromTDB(directory, ng);

                int positionOfComponentToDelete = -1;

                String hasMDBEntryComponent = SprO.hasEntryComponent.toString();

                String hasMDBPosition = SprO.hasPositionInEntryComponent.toString();

                if (currComponentObject.has("input_data")) {

                    System.out.println("length before = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").length());

                } else {
                    // no other statement was generated yet

                    System.out.println("length before = 0");

                }

                for (int i = 0; i < mdbEntryComponentsListWithChildren.length(); i++) {

                    String mdbEntryComponentWithChildren = mdbEntryComponentsListWithChildren.get(i).toString();

                    NodeIterator posIter = mdbComposition.listObjectsOfProperty(ResourceFactory.createResource(mdbEntryComponentWithChildren), ResourceFactory.createProperty(hasMDBPosition));

                    if (posIter.hasNext()) {

                        positionOfComponentToDelete = posIter.next().asLiteral().getInt();

                    }

                    ResIterator parentIter = mdbComposition.listSubjectsWithProperty(ResourceFactory.createProperty(hasMDBEntryComponent), ResourceFactory.createResource(mdbEntryComponentWithChildren));

                    while (parentIter.hasNext()) {

                        Resource parent = parentIter.nextResource();

                        Selector parentSelector = new SimpleSelector(parent, ResourceFactory.createProperty(hasMDBEntryComponent), null, "");

                        StmtIterator parentComponentsIter = mdbComposition.listStatements(parentSelector);

                        while (parentComponentsIter.hasNext()) {

                            Statement currChildStmt = parentComponentsIter.nextStatement();

                            Resource currChild = currChildStmt.getObject().asResource();

                            if (!(currChild.toString().equals(mdbEntryComponentWithChildren))) {

                                int currPositionOfChild = mdbComposition.listObjectsOfProperty(currChild, ResourceFactory.createProperty(hasMDBPosition)).next().asLiteral().getInt();

                                if (currPositionOfChild > positionOfComponentToDelete) {

                                    if (!currComponentObject.has("input_data")) {
                                        // no other statement was generated yet

                                        // delete old position

                                        JSONObject currInputDataObject = new JSONObject();

                                        currInputDataObject.append("subject", currChild.toString());

                                        currInputDataObject.append("property", hasMDBPosition);

                                        currInputDataObject.append("ng", ng);

                                        currInputDataObject.append("directory", directory);

                                        currInputDataObject.append("object_data", String.valueOf(currPositionOfChild));

                                        currInputDataObject.append("object_type", "l");

                                        currInputDataObject.append("operation", "d");

                                        currComponentObject.put("input_data", currInputDataObject);

                                    } else {

                                        // delete old position

                                        currComponentObject.getJSONObject("input_data").append("subject", currChild.toString());

                                        currComponentObject.getJSONObject("input_data").append("property", hasMDBPosition);

                                        currComponentObject.getJSONObject("input_data").append("object_data", String.valueOf(currPositionOfChild));

                                        currComponentObject.getJSONObject("input_data").append("object_type", "l");

                                        currComponentObject.getJSONObject("input_data").append("ng", ng);

                                        currComponentObject.getJSONObject("input_data").append("directory", directory);

                                        currComponentObject.getJSONObject("input_data").append("operation", "d");

                                    }

                                    // add new position

                                    currComponentObject.getJSONObject("input_data").append("subject", currChild.toString());

                                    currComponentObject.getJSONObject("input_data").append("property", hasMDBPosition);

                                    currComponentObject.getJSONObject("input_data").append("object_data", String.valueOf(currPositionOfChild - 1));

                                    currComponentObject.getJSONObject("input_data").append("object_type", "l");

                                    currComponentObject.getJSONObject("input_data").append("ng", ng);

                                    currComponentObject.getJSONObject("input_data").append("directory", directory);

                                    currComponentObject.getJSONObject("input_data").append("operation", "s");

                                }

                            } else {
                                // delete connection parent and component

                                if (!currComponentObject.has("input_data")) {
                                    // no other statement was generated yet

                                    // delete old position

                                    JSONObject currInputDataObject = new JSONObject();

                                    currInputDataObject.append("subject", parent.toString());

                                    currInputDataObject.append("property", hasMDBEntryComponent);

                                    currInputDataObject.append("object_data", mdbEntryComponentWithChildren);

                                    currInputDataObject.append("object_type", "l");

                                    currInputDataObject.append("ng", ng);

                                    currInputDataObject.append("directory", directory);

                                    currInputDataObject.append("operation", "d");

                                    currComponentObject.put("input_data", currInputDataObject);

                                } else {

                                    currComponentObject.getJSONObject("input_data").append("subject", parent.toString());

                                    currComponentObject.getJSONObject("input_data").append("property", hasMDBEntryComponent);

                                    currComponentObject.getJSONObject("input_data").append("object_data", mdbEntryComponentWithChildren);

                                    currComponentObject.getJSONObject("input_data").append("object_type", "r");

                                    currComponentObject.getJSONObject("input_data").append("ng", ng);

                                    currComponentObject.getJSONObject("input_data").append("directory", directory);

                                    currComponentObject.getJSONObject("input_data").append("operation", "d");

                                }

                            }

                        }

                    }

                    JSONArray componentsToDeleteJSONArray = new JSONArray();

                    componentsToDeleteJSONArray.put(mdbEntryComponentWithChildren);

                    deleteMDBEntryComponent(currComponentObject, componentsToDeleteJSONArray, ng, directory, mdbComposition);

                }

                System.out.println("length behind = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").length());

            }

        }



        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }


    /**
     * This method executes an order from the ontology.
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a modified JSONObject
     */
    public JSONObject executionStepExecuteNow (JSONObject currComponentObject, JSONObject jsonInputObject,
                                               JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        boolean saveToStoreExist = false;

        for (int j = 0; j < currExecStep.length(); j++) {

            if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.updateStoreBOOLEAN.toString())) {

                saveToStoreExist = true;

            } else if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.applicationInfoMessage.toString())) {

                currComponentObject.put(ResourceFactory.createProperty(currExecStep.getJSONObject(j).get("property").toString()).getLocalName(), ResourceFactory.createPlainLiteral(currExecStep.getJSONObject(j).get("object").toString()).asLiteral().getLexicalForm());

            }

        }

        if (saveToStoreExist) {

            //System.out.println("currComponentObject before save store" + currComponentObject);

            saveToStores(currComponentObject, jsonInputObject, connectionToTDB);

        }

        return currComponentObject;

    }


    /**
     * This method creates a new DOI
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void executionStepGetDOI(JSONObject jsonInputObject, JSONArray currExecStep,
                                    JenaIOTDBFactory connectionToTDB) {


        String newFocusURI = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryID.toString())) {

                newFocusURI = setFocusOnIndividual(currExecStep.getJSONObject(i).get("object").toString(), currExecStep, jsonInputObject, newFocusURI, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        // todo rebuilt this when we use real DOIs

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.doiLabelDefinesSPrOVariableResource.toString())) {

                System.out.println("Created the following DOI = " + "doi:10.5072/mdb." + this.currentFocus.substring(this.currentFocus.lastIndexOf("/") + 1));

                this.identifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), "doi:10.5072/mdb." + this.currentFocus.substring(this.currentFocus.lastIndexOf("/") + 1));

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.doiDefinesSPrOVariableResource.toString())) {

                System.out.println("Created the following DOI = " + this.currentFocus);

                this.identifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), this.currentFocus);

            }

        }


        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

    }

    /**
     * This method search for a resource or value in the jena tdb and save the result in an identified keyword
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void executionStepSearchTripleStore(JSONObject currComponentObject, JSONObject jsonInputObject,
                                               JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        String newFocusURI = "";

        boolean copiedIndividualNG = false;

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryID.toString())) {

                newFocusURI = setFocusOnIndividual(currExecStep.getJSONObject(i).get("object").toString(), currExecStep, jsonInputObject, newFocusURI, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())) {

                copiedIndividualNG = true;

            }

        }

        boolean executeThisStep = true;

        if (jsonInputObject.has("mdbcoreid")) {

            if (jsonInputObject.get("mdbcoreid").toString().equals(ApplicationConfigurator.getDomain() + "/resource/dummy-overlay")) {

                executeThisStep = false;

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {


                    } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                        if (jsonInputObject.get("html_form").toString().contains(ResourceFactory.createResource(currExecStep.getJSONObject(i).get("object").toString()).getLocalName())) {

                            this.createOverlayNG = this.mdbCoreID.substring(0, this.mdbCoreID.indexOf("resource/dummy-overlay")) + jsonInputObject.get("html_form").toString();

                            this.hasCreateOverlayInput = true;

                            executeThisStep = true;

                            System.out.println("createOverlayNG = " + this.createOverlayNG);

                        }

                    }

                }

            }

        }

        if (executeThisStep) {

            if (copiedIndividualNG) {

                boolean searchSubject = false, searchObject = false;

                String subject = "", property = "", object = "", directory = "", ngIndividual = "",
                        searchTargetKeyword = "", searchTarget = "";

                JSONObject dataToFindSubjectInTDB = new JSONObject(), dataToFindObjectInTDB = new JSONObject();

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.searchTarget.toString())) {

                        if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLESubject.toString())) {

                            searchSubject = true;

                        } else if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEObject.toString())) {

                            searchObject = true;

                        }

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.searchTargetDefinesSPrOVariable.toString())) {

                        searchTargetKeyword = currExecStep.getJSONObject(i).get("object").toString();

                    }

                }

                if (searchSubject) {

                    for (int i = 0; i < currExecStep.length(); i++) {

                        if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())
                                || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectCopiedIndividualOf.toString())) {

                            object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, "a", connectionToTDB);

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.propertySOCCOMAS.toString())) {

                            property = calculateProperty(currExecStep);

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                            directory = calculateWorkspaceDirectory(currExecStep);

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())
                                || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {

                            ngIndividual = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        }

                    }

                    JSONArray ngsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("ng");

                    // check if object already was generated in execution step 'copy and save triple statement(s)'
                    for (int i = 0; i < ngsInJSONArray.length(); i++) {

                        if (ngsInJSONArray.get(i).toString().equals(ngIndividual)) {

                            if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString().equals(property)
                                    && currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString().equals(object)
                                    && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                searchTarget = currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString();

                            }

                        }

                    }

                } else if (searchObject) {

                    for (int i = 0; i < currExecStep.length(); i++) {

                        if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.propertySOCCOMAS.toString())) {

                            property = calculateProperty(currExecStep);

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())
                                || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {

                            subject = calculateSubject(dataToFindSubjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                            directory = calculateWorkspaceDirectory(currExecStep);

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())
                                || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {

                            ngIndividual = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        }

                    }

                    JSONArray ngsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("ng");

                    // check if object already was generated in execution step 'copy and save triple statement(s)'
                    for (int i = 0; i < ngsInJSONArray.length(); i++) {

                        if (ngsInJSONArray.get(i).toString().equals(ngIndividual)) {

                            if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString().equals(property)
                                    && currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString().equals(subject)
                                    && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                searchTarget = currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString();

                            }

                        }

                    }

                }

                System.out.println("searchTargetKeyword = " + searchTargetKeyword);
                System.out.println("searchTarget = " + searchTarget);

                this.identifiedResources.put(searchTargetKeyword, searchTarget);

            } else {

                boolean multipleHitsSearch = false, subjectIsUnknown = false, propertyIsUnknown = false,
                        objectIsUnknown = false, ngIsUnknown = false, listExist = false;

                String searchTarget = null, searchTargetKeyword = null, searchTargetListKeyword = null,
                        uriWithoutTerminalCounterValue = "";

                JSONArray objectsJSONArray = new JSONArray(), subjectsJSONArray = new JSONArray();

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())) {

                        if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEQuestionMark.toString())) {

                            subjectIsUnknown = true;

                        }

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.propertySOCCOMAS.toString())) {


                        if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEQuestionMark.toString())) {

                            propertyIsUnknown = true;

                        }

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())) {

                        if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEQuestionMark.toString())) {

                            objectIsUnknown = true;

                        }

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {

                        if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEQuestionMark.toString())) {

                            ngIsUnknown = true;

                        }

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.searchTarget.toString())) {

                        if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLESubject.toString())) {

                            searchTarget = "s";

                        } else if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEProperty.toString())) {

                            searchTarget = "p";

                        } else if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEObject.toString())) {

                            searchTarget = "o";

                        } else if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLENamedGraph.toString())) {

                            searchTarget = "ng";

                        }

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.searchTargetDefinesSPrOVariable.toString())) {

                        searchTargetKeyword = currExecStep.getJSONObject(i).get("object").toString();

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.multipleHitsSearchBOOLEAN.toString())) {

                        multipleHitsSearch = true;

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.searchTargetSavedToListOfURIsNamedGraphSPrOVariable.toString())) {

                        searchTargetListKeyword = currExecStep.getJSONObject(i).get("object").toString();

                    } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.subjectList.toString())) {

                        subjectsJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                        listExist = true;

                    } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.objectList.toString())) {

                        objectsJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                        listExist = true;

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.searchRestrictedToEntryURIExceptForTerminalCounterValue.toString())) {

                        uriWithoutTerminalCounterValue = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                    }

                }

                if (multipleHitsSearch) {

                    JSONObject dataToFindObjectInTDB = new JSONObject();

                    switch (searchTarget) {

                        case "o":

                            String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                            System.out.println("currDirectoryPath = " + currDirectoryPath);

                            dataToFindObjectInTDB.put("directory", currDirectoryPath);

                            String currSubject = subjectIsUnknown ? "?s" : calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            System.out.println("currSubject = " + currSubject);

                            String currProperty = propertyIsUnknown ? "?p" : calculateProperty(currExecStep);

                            System.out.println("currProperty = " + currProperty);

                            String currNG = ngIsUnknown ? "?ng" : calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            System.out.println("currNG = " + currNG);

                            String currObjectType = calculateObjectType(currProperty);

                            System.out.println("currObjectType = " + currObjectType);

                            dataToFindObjectInTDB.put("subject", currSubject);
                            dataToFindObjectInTDB.put("property", currProperty);
                            dataToFindObjectInTDB.put("ng", currNG);

                            JSONArray currObjectList = calculateObjectList(dataToFindObjectInTDB, connectionToTDB);

                            if (!uriWithoutTerminalCounterValue.isEmpty()) {

                                uriWithoutTerminalCounterValue = uriWithoutTerminalCounterValue.substring(0, uriWithoutTerminalCounterValue.lastIndexOf("_"));

                                for (int i = currObjectList.length() - 1; i >= 0 ; i--) {

                                    if (!currObjectList.get(i).toString().contains(uriWithoutTerminalCounterValue)) {

                                        System.out.println(currObjectList.get(i).toString() + " was removed!");

                                        currObjectList.remove(i);

                                    }

                                }

                            }

                            System.out.println("currObjectList = " + currObjectList);

                            this.identifiedResources.put(searchTargetListKeyword, currObjectList);

                            break;

                        case "s":

                            currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                            System.out.println("currDirectoryPath = " + currDirectoryPath);

                            dataToFindObjectInTDB.put("directory", currDirectoryPath);

                            currProperty = propertyIsUnknown ? "?p" : calculateProperty(currExecStep);

                            System.out.println("currProperty = " + currProperty);

                            currNG = ngIsUnknown ? "?ng" : calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            System.out.println("currNG = " + currNG);

                            currObjectType = calculateObjectType(currProperty);

                            System.out.println("currObjectType = " + currObjectType);

                            String currObject = objectIsUnknown ? "?o" : calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, currObjectType, connectionToTDB);

                            System.out.println("currObject = " + currObject);

                            dataToFindObjectInTDB.put("property", currProperty);
                            dataToFindObjectInTDB.put("ng", currNG);
                            dataToFindObjectInTDB.put("object", currObject);

                            JSONArray currSubjectList = calculateSubjectList(dataToFindObjectInTDB, connectionToTDB);

                            System.out.println("currSubjectList = " + currSubjectList);

                            this.identifiedResources.put(searchTargetListKeyword, currSubjectList);

                            break;

                    }

                } else {

                    if (listExist) {

                        boolean searchTargetWasAlreadyFound = false;

                        if (subjectsJSONArray.length() > 0) {

                            System.out.println("length of currSubjectList = " + subjectsJSONArray.length());

                            String currProperty = calculateProperty(currExecStep);

                            //System.out.println("currProperty = " + currProperty);

                            String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            //System.out.println("currNG = " + currNG);

                            String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                            //System.out.println("currDirectoryPath = " + currDirectoryPath);

                            String currObjectType = calculateObjectType(currProperty);

                            //System.out.println("currObjectType = " + currObjectType);

                            String currObject = calculateObject(new JSONObject(), currExecStep, currComponentObject, jsonInputObject, currObjectType, connectionToTDB);

                            for (int i = 0; i < subjectsJSONArray.length(); i++) {

                                String currSubject = subjectsJSONArray.get(i).toString();

                                SelectBuilder selectWhereBuilder = new SelectBuilder();

                                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                                selectWhereBuilder.addWhere("<" + currSubject + ">", "<" + currProperty + ">","<" + currObject + ">");

                                AskBuilder askBuilder = new AskBuilder();

                                askBuilder = prefixesBuilder.addPrefixes(askBuilder);

                                askBuilder.addGraph("<" + currNG + ">", selectWhereBuilder);

                                String sparqlQueryString = askBuilder.buildString();

                                boolean stmtExist = connectionToTDB.statementExistInTDB(currDirectoryPath, sparqlQueryString);

                                if (stmtExist) {

                                    if (searchTargetWasAlreadyFound) {

                                        System.out.println("WARN: The former found search target" + searchTarget + " will be overwritten!");

                                    }

                                    searchTarget = currSubject;

                                    searchTargetWasAlreadyFound = true;

                                }

                            }

                        } else if (objectsJSONArray.length() > 0) {

                            System.out.println("length of currObjectList = " + objectsJSONArray.length());

                            String currSubject = calculateSubject(new JSONObject(), currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            //System.out.println("currSubject = " + currSubject);

                            String currProperty = calculateProperty(currExecStep);

                            //System.out.println("currProperty = " + currProperty);

                            String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            //System.out.println("currNG = " + currNG);

                            String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                            //System.out.println("currDirectoryPath = " + currDirectoryPath);

                            String currObjectType = calculateObjectType(currProperty);

                            //System.out.println("currObjectType = " + currObjectType);

                            for (int i = 0; i < objectsJSONArray.length(); i++) {

                                String currObject = objectsJSONArray.get(i).toString();

                                SelectBuilder selectWhereBuilder = new SelectBuilder();

                                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                                selectWhereBuilder.addWhere("<" + currSubject + ">", "<" + currProperty + ">","<" + currObject + ">");

                                AskBuilder askBuilder = new AskBuilder();

                                askBuilder = prefixesBuilder.addPrefixes(askBuilder);

                                askBuilder.addGraph("<" + currNG + ">", selectWhereBuilder);

                                String sparqlQueryString = askBuilder.buildString();

                                boolean stmtExist = connectionToTDB.statementExistInTDB(currDirectoryPath, sparqlQueryString);

                                if (stmtExist) {

                                    if (searchTargetWasAlreadyFound) {

                                        System.out.println("WARN: The former found search target" + searchTarget + " will be overwritten!");

                                    }

                                    searchTarget = currObject;

                                    searchTargetWasAlreadyFound = true;

                                }

                            }

                        } else {

                            System.out.println("WARN: The object list and the subject list is empty.");

                        }

                    } else {

                        JSONObject dataToFindObjectInTDB = new JSONObject();

                        String currSubject = subjectIsUnknown ? "?s" : calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        System.out.println("currSubject = " + currSubject);

                        String currProperty = calculateProperty(currExecStep);

                        System.out.println("currProperty = " + currProperty);

                        String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        System.out.println("currNG = " + currNG);

                        String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                        System.out.println("currDirectoryPath = " + currDirectoryPath);

                        String currObjectType = calculateObjectType(currProperty);

                        System.out.println("currObjectType = " + currObjectType);

                        dataToFindObjectInTDB.put("subject", currSubject);
                        dataToFindObjectInTDB.put("property", currProperty);
                        dataToFindObjectInTDB.put("ng", currNG);
                        dataToFindObjectInTDB.put("directory", currDirectoryPath);

                        String currObject = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, currObjectType, connectionToTDB);

                        System.out.println("currObject = " + currObject);

                        switch (searchTarget) {

                            case "s":

                                if (currSubject.equals("?s")) {

                                    dataToFindObjectInTDB = new JSONObject();

                                    dataToFindObjectInTDB.put("object", currObject);
                                    dataToFindObjectInTDB.put("property", currProperty);
                                    dataToFindObjectInTDB.put("ng", currNG);
                                    dataToFindObjectInTDB.put("directory", currDirectoryPath);

                                    currSubject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                                }

                                searchTarget = currSubject;

                                break;

                            case "p":

                                searchTarget = currProperty;

                                break;

                            case "o":

                                searchTarget = currObject;

                                break;

                            case "ng":

                                searchTarget = currNG;

                                break;

                        }

                    }

                    System.out.println("searchTargetKeyword = " + searchTargetKeyword);
                    System.out.println("searchTarget = " + searchTarget);

                    this.identifiedResources.put(searchTargetKeyword, searchTarget);

                }

            }

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

    }

    /**
     * This method copies and modifies statements from the default composition to a specific composition
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return input information for a jena tdb
     */
    public JSONObject executionStepCopyAndSaveTripleStatements(JSONArray currExecStep, JSONObject currComponentObject,
                                                               JSONObject jsonInputObject,
                                                               JenaIOTDBFactory connectionToTDB) {

        String newFocusURI = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryID.toString())) {

                newFocusURI = setFocusOnIndividual(currExecStep.getJSONObject(i).get("object").toString(), currExecStep, jsonInputObject, newFocusURI, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        for (int i = 0; i < currExecStep.length();i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.copyFromNamedGraphOfClass.toString())) {

                Iterator<String> infoInputKeys = this.infoInput.keys();

                boolean continueLoop = true;

                while (infoInputKeys.hasNext() && continueLoop) {

                    String currKey = infoInputKeys.next();

                    if (currKey.equals(currExecStep.getJSONObject(i).get("object").toString())) {

                        currExecStep.getJSONObject(i).put("object", this.infoInput.get(currKey));

                        continueLoop = false;

                    }

                }

                Model defaultCompositionModel = findRootIndividual(currExecStep.getJSONObject(i).get("object").toString(), currExecStep, connectionToTDB);

                String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                String ng = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                boolean modelExistInTDB = connectionToTDB.modelExistInTDB(currDirectoryPath, ng);

                Model modelFromTDB = ModelFactory.createDefaultModel();

                if (!modelExistInTDB) {

                    Selector selector = new SimpleSelector(null, RDF.type, null, "");

                    StmtIterator typeStmts = defaultCompositionModel.listStatements(selector);

                    while (typeStmts.hasNext()) {

                        Statement currStmt = typeStmts.nextStatement();

                        Resource currObject = currStmt.getObject().asResource();
                        Resource currSubject = currStmt.getSubject();

                        String newResource = "";

                        if (!(currObject.equals(OWL2.Axiom))
                                && !(currObject.equals(OWL2.Class))
                                && !(currObject.equals(OWL2.NamedIndividual))) {

                            if (this.mdbCoreIDNotEmpty
                                    && this.mdbUEIDNotEmpty
                                    && !this.mdbEntryIDNotEmpty) {


                            } else if (this.mdbEntryIDNotEmpty
                                    && this.mdbUEIDNotEmpty) {

                                IndividualURI individualURI = new IndividualURI(this.mdbEntryID);

                                newResource = individualURI.createURIForAnIndividualForANewNamespace(currObject.toString());

                            } else if (mdbUEIDNotEmpty) {

                                IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                                newResource = individualURI.createURIForAnIndividualForANewNamespace(currObject.toString());

                            }

                            if (this.numberOfClassInstances.has(currObject.toString())) {

                                if (defaultCompositionModel.contains(currSubject, SCBasic.isRootEntryComponentOfCompositionContainedInNamedGraph)) {

                                    newResource = this.rootResourcesOfCompositions.get(currSubject.toString()).toString();

                                } else {

                                    int newNumberOfInstancesOfClass = this.numberOfClassInstances.getInt(currObject.toString()) + 1;

                                    this.numberOfClassInstances.put(currObject.toString(), newNumberOfInstancesOfClass);

                                    newResource = newResource.substring(0, newResource.lastIndexOf("_")) + "_" + newNumberOfInstancesOfClass;

                                }

                            } else {

                                if (defaultCompositionModel.contains(currSubject, SCBasic.isRootEntryComponentOfCompositionContainedInNamedGraph)) {

                                    this.rootResourcesOfCompositions.put(currSubject.toString(), newResource);

                                }

                                this.numberOfClassInstances.put(currObject.toString(), 1);

                            }

                            this.entrySpecificAndDefaultResourcesMap.put(currSubject.toString(), newResource);

                        }

                    }

                } else {

                    modelFromTDB = connectionToTDB.pullNamedModelFromTDB(currDirectoryPath, ng);

                }

                StmtIterator stmtIterator = defaultCompositionModel.listStatements();

                while (stmtIterator.hasNext()) {

                    Statement currStmt = stmtIterator.nextStatement();
                    String currSubject = currStmt.getSubject().toString();
                    String currObject;

                    boolean generateIndividuals;

                    if (defaultCompositionModel.contains(currStmt.getSubject(), SCBasic.isRootEntryComponentOfCompositionContainedInNamedGraph)) {

                        if (defaultCompositionModel.contains(currStmt.getSubject(), SCBasic.isRootEntryComponentOfCompositionContainedInNamedGraph, ResourceFactory.createResource(currExecStep.getJSONObject(i).get("object").toString()))) {

                            generateIndividuals = true;

                        } else {

                            generateIndividuals = false;

                        }

                    } else {

                        generateIndividuals = true;

                    }

                    if (generateIndividuals) {

                        if (currStmt.getObject().isURIResource()) {

                            currObject = currStmt.getObject().asResource().toString();

                        } else if(currStmt.getObject().isAnon()) {

                            currObject = currStmt.getObject().toString();

                        } else {

                            currObject = currStmt.getObject().asLiteral().getLexicalForm();

                        }

                        if (!(currStmt.getSubject().isAnon()) &&
                                !(this.classSet.classExist(this.classModel, currStmt.getSubject().toString()))) {

                            if (modelExistInTDB) {

                                if (this.mdbCoreIDNotEmpty
                                        && this.mdbUEIDNotEmpty
                                        && !this.mdbEntryIDNotEmpty) {


                                } else if (this.mdbEntryIDNotEmpty
                                        && this.mdbUEIDNotEmpty) {

                                    if (this.entrySpecificAndDefaultResourcesMap.has(currSubject)) {

                                        currSubject = this.entrySpecificAndDefaultResourcesMap.get(currSubject).toString();

                                    } else {

                                        IndividualURI individualURI = new IndividualURI(this.mdbEntryID);

                                        if (defaultCompositionModel.contains(currStmt.getSubject(), RDF.type)) {

                                            Selector potentialNewIndividualsSelector = new SimpleSelector(currStmt.getSubject(), RDF.type, null, "");

                                            StmtIterator potentialNewIndividualIter = defaultCompositionModel.listStatements(potentialNewIndividualsSelector);

                                            while (potentialNewIndividualIter.hasNext()) {

                                                Statement potentialNewIndividualStmt = potentialNewIndividualIter.next();

                                                if (!potentialNewIndividualStmt.getObject().equals(OWL2.NamedIndividual)) {

                                                    if (modelFromTDB.contains(null, RDF.type, potentialNewIndividualStmt.getObject())) {

                                                        currSubject = individualURI.createURIForAnIndividual(potentialNewIndividualStmt.getObject().toString(), ng, currDirectoryPath, connectionToTDB);

                                                        this.entrySpecificAndDefaultResourcesMap.put(potentialNewIndividualStmt.getSubject().toString(), currSubject);

                                                    } else {

                                                        currSubject = individualURI.createURIForAnIndividualForANewNamespace(potentialNewIndividualStmt.getObject().toString());

                                                        this.entrySpecificAndDefaultResourcesMap.put(potentialNewIndividualStmt.getSubject().toString(), currSubject);

                                                    }

                                                }

                                            }

                                        } else {

                                            currSubject = individualURI.createURIForAnIndividual(currSubject, ng, currDirectoryPath, connectionToTDB);

                                        }

                                    }

                                } else if (mdbUEIDNotEmpty) {

                                    IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                                    currSubject = individualURI.createURIForAnIndividual(currSubject, ng, currDirectoryPath, connectionToTDB);

                                }

                            } else {

                                currSubject = this.entrySpecificAndDefaultResourcesMap.get(currSubject).toString();

                            }

                        }

                        if (    (!(currStmt.getSubject().isAnon()) &&
                                currStmt.getObject().isURIResource() &&
                                !currStmt.getObject().equals(OWL2.Axiom) &&
                                !currStmt.getObject().equals(OWL2.Class) &&
                                !currStmt.getObject().equals(OWL2.NamedIndividual) &&
                                !(this.classSet.classExist(this.classModel, currStmt.getObject().toString())))
                                || (currStmt.getSubject().isAnon() &&
                                ((currStmt.getPredicate().equals(OWL2.annotatedSource))
                                        || (currStmt.getPredicate().equals(OWL2.annotatedTarget))))) {


                            if (modelExistInTDB) {

                                if (this.mdbCoreIDNotEmpty
                                        && this.mdbUEIDNotEmpty
                                        && !this.mdbEntryIDNotEmpty) {


                                } else if (this.mdbEntryIDNotEmpty
                                        && this.mdbUEIDNotEmpty) {

                                    if ((currStmt.getSubject().isAnon() &&
                                            ((currStmt.getPredicate().toString().equals("http://www.w3.org/2002/07/owl#annotatedSource"))
                                                    || (currStmt.getPredicate().toString().equals("http://www.w3.org/2002/07/owl#annotatedTarget"))))) {

                                        if (currStmt.getObject().isResource()) {

                                            if (!this.classSet.classExist(this.classModel, currStmt.getObject().toString())) {

                                                IndividualURI individualURI = new IndividualURI(this.mdbEntryID);

                                                currObject = individualURI.createURIForAnIndividual(currObject, ng, currDirectoryPath, connectionToTDB);

                                            }

                                        }

                                    } else if(currStmt.getObject().isAnon()) {


                                    } else {

                                        if (this.entrySpecificAndDefaultResourcesMap.has(currObject)) {

                                            currObject = this.entrySpecificAndDefaultResourcesMap.get(currObject).toString();

                                        } else {

                                            IndividualURI individualURI = new IndividualURI(this.mdbEntryID);

                                            if (!this.classSet.classExist(this.classModel, currStmt.getObject().toString())) {

                                                if (defaultCompositionModel.contains(currStmt.getObject().asResource(), RDF.type)) {

                                                    Selector potentialNewIndividualsSelector = new SimpleSelector(currStmt.getObject().asResource(), RDF.type, null, "");

                                                    StmtIterator potentialNewIndividualIter = defaultCompositionModel.listStatements(potentialNewIndividualsSelector);

                                                    while (potentialNewIndividualIter.hasNext()) {

                                                        Statement potentialNewIndividualStmt = potentialNewIndividualIter.next();

                                                        if (!potentialNewIndividualStmt.getObject().equals(OWL2.NamedIndividual)) {

                                                            if (modelFromTDB.contains(null, RDF.type, potentialNewIndividualStmt.getObject())) {

                                                                currObject = individualURI.createURIForAnIndividual(potentialNewIndividualStmt.getObject().toString(), ng, currDirectoryPath, connectionToTDB);

                                                                this.entrySpecificAndDefaultResourcesMap.put(potentialNewIndividualStmt.getSubject().toString(), currObject);

                                                            } else {

                                                                currObject = individualURI.createURIForAnIndividualForANewNamespace(potentialNewIndividualStmt.getObject().toString());

                                                                this.entrySpecificAndDefaultResourcesMap.put(potentialNewIndividualStmt.getSubject().toString(), currObject);

                                                            }

                                                        }

                                                    }

                                                } else if (!currStmt.getPredicate().equals(SprO.hasGUIRepresentation)) {

                                                    currObject = individualURI.createURIForAnIndividual(currObject, ng, currDirectoryPath, connectionToTDB);

                                                }

                                            }

                                        }

                                    }

                                } else if (mdbUEIDNotEmpty) {

                                    if (currStmt.getSubject().isAnon() &&
                                            ((currStmt.getPredicate().equals(OWL2.annotatedSource))
                                                    || (currStmt.getPredicate().equals(OWL2.annotatedTarget)))) {

                                        if (currStmt.getObject().isResource()) {

                                            if (!this.classSet.classExist(this.classModel, currStmt.getObject().toString())) {

                                                IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                                                currObject = individualURI.createURIForAnIndividual(currObject, ng, currDirectoryPath, connectionToTDB);

                                            }

                                        }

                                    } else {

                                        IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                                        currObject = individualURI.createURIForAnIndividual(currObject, ng, currDirectoryPath, connectionToTDB);

                                    }

                                }

                            } else {

                                if ((currStmt.getSubject().isAnon() &&
                                        ((currStmt.getPredicate().equals(OWL2.annotatedSource))
                                                || (currStmt.getPredicate().equals(OWL2.annotatedTarget))))) {

                                    if (currStmt.getObject().isResource()) {

                                        if (!this.classSet.classExist(this.classModel, currStmt.getObject().toString())
                                                && this.entrySpecificAndDefaultResourcesMap.has(currObject)) {

                                            currObject = this.entrySpecificAndDefaultResourcesMap.get(currObject).toString();

                                        }

                                    }

                                } else if(this.entrySpecificAndDefaultResourcesMap.has(currObject)) {

                                    currObject = this.entrySpecificAndDefaultResourcesMap.get(currObject).toString();

                                }

                            }

                        }

                        if (!currComponentObject.has("input_data")) {
                            // no other statement was generated yet

                            JSONObject currInputDataObject = new JSONObject();

                            currInputDataObject.append("subject", currSubject);

                            String currProperty = currStmt.getPredicate().toString();

                            currInputDataObject.append("property", currProperty);

                            String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            currInputDataObject.append("ng", currNG);

                            currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                            currInputDataObject.append("directory", currDirectoryPath);

                            String currObjectType = calculateObjectType(currProperty);

                            currInputDataObject.append("object_data", currObject);

                            currObjectType = calculateObjectTypeForAnnotationProperty(currObject, currObjectType);

                            currInputDataObject.append("object_type", currObjectType);

                            String currOperation = calculateOperation(currExecStep);

                            currInputDataObject.append("operation", currOperation);

                            currComponentObject.put("input_data", currInputDataObject);

                        } else {

                            currComponentObject.getJSONObject("input_data").append("subject", currSubject);

                            String currProperty = currStmt.getPredicate().toString();

                            currComponentObject.getJSONObject("input_data").append("property", currProperty);

                            String currObjectType = calculateObjectType(currProperty);

                            currComponentObject.getJSONObject("input_data").append("object_data", currObject);

                            currObjectType = calculateObjectTypeForAnnotationProperty(currObject, currObjectType);

                            currComponentObject.getJSONObject("input_data").append("object_type", currObjectType);

                            String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            currComponentObject.getJSONObject("input_data").append("ng", currNG);

                            currComponentObject.getJSONObject("input_data").append("directory", currDirectoryPath);

                            String currOperation = "s";

                            currComponentObject.getJSONObject("input_data").append("operation", currOperation);

                        }

                    }

                }

            }

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;
    }


    /**
     * This method deletes a named graph from a jena tdb.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void executionStepDeleteAllTriplesOfNamedGraph(JSONArray currExecStep, JSONObject currComponentObject, JSONObject jsonInputObject,
                                                          JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }


        String workspace = calculateWorkspaceDirectory(currExecStep);

        String namedGraph = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

        if (connectionToTDB.modelExistInTDB(workspace, namedGraph)) {

            connectionToTDB.removeNamedModelFromTDB(workspace, namedGraph);

        } else {

            System.out.println("There is no ng in the jena tdb.");

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

    }

    /**
     * This method extracts a MDB Entry Composition in a new generated named graph
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param jsonInputObject contains the information for the calculation
     * @param currComponentObject contains the current component information for the output json
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void executionStepExtractAndSaveEntryComposition(JSONArray currExecStep, JSONObject currComponentObject,
                                                            JSONObject jsonInputObject,
                                                            JenaIOTDBFactory connectionToTDB){

        JSONArray classToCheck = new JSONArray();

        String directoryPath = calculateWorkspaceDirectory(currExecStep);

        String ng = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.compositionHasRootElement.toString())) {

                classToCheck.put(currExecStep.getJSONObject(i).get("object").toString());

            }

        }

        Model defaultCompositionModel = calculateDefaultEntryComposition(classToCheck, connectionToTDB);

        // save named graph in jena tdb
        System.out.println("ng message = " + connectionToTDB.addModelDataInTDB(directoryPath, ng, defaultCompositionModel));

    }

    /**
     * This method generates resources and provide this data in for a transition. Furthermore some input information for
     * the jena tdb will be generated.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return input information for a jena tdb
     */
    public JSONObject executionStepGenerateResources(JSONArray currExecStep, JSONObject currComponentObject,
                                                     JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        boolean executeThisStep = true;

        // special case overlay
        if (jsonInputObject.has("mdbcoreid")) {

            if (jsonInputObject.get("mdbcoreid").toString().equals(ApplicationConfigurator.getDomain() + "/resource/dummy-overlay")) {

                executeThisStep = false;

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {


                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())) {


                    } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                        if (jsonInputObject.get("html_form").toString().contains(ResourceFactory.createResource(currExecStep.getJSONObject(i).get("object").toString()).getLocalName())) {

                            this.createOverlayNG = this.mdbCoreID.substring(0, this.mdbCoreID.indexOf("resource/dummy-overlay")) + jsonInputObject.get("html_form").toString();

                            this.hasCreateOverlayInput = true;

                            executeThisStep = true;

                            System.out.println("createOverlayNG = " + this.createOverlayNG);

                        }

                    }

                }

            }

        }

        if (executeThisStep) {

            String generateResourceFor = "";

            for (int i = 0; i < currExecStep.length(); i++) {

                Iterator<String> infoInputKeys = this.infoInput.keys();

                boolean continueLoop = true;

                while (infoInputKeys.hasNext() && continueLoop) {

                    String currKey = infoInputKeys.next();

                    if (currKey.equals(currExecStep.getJSONObject(i).get("object").toString())) {

                        currExecStep.getJSONObject(i).put("object", this.infoInput.get(currKey));

                        continueLoop = false;

                    }

                }

                if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryID.toString())) {

                    generateResourceFor = setFocusOnIndividual(currExecStep.getJSONObject(i).get("object").toString(), currExecStep, jsonInputObject, generateResourceFor, connectionToTDB);

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDIndividualOf.toString())){

                    setFocusOnClass(jsonInputObject, connectionToTDB, currExecStep.getJSONObject(i).get("object").toString());

                }

            }

            JSONObject currInputDataObject;

            // check if some input data already exists
            if (!currComponentObject.has("input_data")) {

                currInputDataObject = new JSONObject();

            } else {

                currInputDataObject = currComponentObject.getJSONObject("input_data");

            }

            // use the class as key and the number of individuals as value
            JSONObject numberIndividualsOfClass = new JSONObject();


            if (this.mdbCoreIDNotEmpty
                    && this.mdbUEIDNotEmpty
                    && !this.mdbEntryIDNotEmpty) {

                // sort resources with numbers in string
                OrderDataset orderDataset = new OrderDataset(currExecStep, ApplicationConfigurator.getPathToApplicationOntologyStore(), connectionToTDB);

                // get the sorted object resources
                ArrayList<String> sortedResources = orderDataset.getSortedObjects();

                //System.out.println("sortedResources: " + sortedResources);

                ArrayList<String> sortedPropertyResources = orderDataset.getSortedProperties();

                String currSubject, currProperty, currObject;

                int index = 0;

                String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                for (String currResource : sortedResources) {

                    if (!currResource.equals(generateResourceFor) &&
                            !currResource.equals(SCBasic.userEntryID.toString())) {
                        // don't calculate for already known individual

                        IndividualURI individualURI = new IndividualURI(this.mdbCoreID);

                        currSubject = individualURI.createURIForAnIndividual(currResource, currDirectoryPath, connectionToTDB);

                    } else if (currResource.equals(SCBasic.userEntryID.toString())) {

                        currSubject = this.mdbUEID;

                    } else {

                        currSubject = this.mdbCoreID;

                    }

                    currProperty = String.valueOf(RDF.type);

                    currObject = currResource;

                    String currObjectType = calculateObjectType(currProperty);

                    String currOperation = "s";

                    if (numberIndividualsOfClass.has(currResource)) {

                        currSubject = currSubject.substring(0, currSubject.lastIndexOf("_") + 1) + (numberIndividualsOfClass.getInt(currResource) + 1);

                        numberIndividualsOfClass.put(currResource, (numberIndividualsOfClass.getInt(currResource) + 1));

                    } else {

                        numberIndividualsOfClass.put(currResource, 1);

                    }

                    currInputDataObject.append("subject", currSubject);
                    currInputDataObject.append("property", currProperty);
                    currInputDataObject.append("object_data", currObject);
                    currInputDataObject.append("object_type", currObjectType);
                    currInputDataObject.append("operation", currOperation);
                    currInputDataObject.append("directory", currDirectoryPath);

                    this.generatedResources.put(sortedPropertyResources.get(index), currSubject);

                    index++;

                }

                // the named graph must calculate afterwards
                String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                for (String currResource : sortedResources) {

                    currInputDataObject.append("ng", currNG);

                }

                currComponentObject.put("input_data", currInputDataObject);

                if (this.hasCreateOverlayInput) {

                    this.hasCreateOverlayInput = false;

                }

                return currComponentObject;


            } else if (this.mdbEntryIDNotEmpty
                    && this.mdbUEIDNotEmpty) {

                System.out.println("case entry ID");

                // sort resources with numbers in string
                OrderDataset orderDataset = new OrderDataset(currExecStep, ApplicationConfigurator.getPathToApplicationOntologyStore(), connectionToTDB);

                // get the sorted object resources
                ArrayList<String> sortedObjectResources = orderDataset.getSortedObjects();

                //System.out.println("sortedObjectResources: " + sortedObjectResources);

                ArrayList<String> sortedPropertyResources = orderDataset.getSortedProperties();

                String currSubject, currProperty, currObject;

                int index = 0;

                String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                for (String currResource : sortedObjectResources) {

                    //System.out.println("numberIndividualsOfClass = " + numberIndividualsOfClass);

                    if (this.infoInput.has(currResource)) {
                        // get class from info input

                        currResource = this.infoInput.get(currResource).toString();

                    } else if (this.identifiedResources.has(currResource)) {
                        // get class from identified resources

                        currResource = this.identifiedResources.get(currResource).toString();

                    } else {
                        // get class from input

                        JSONArray arrayToCheck = jsonInputObject.getJSONArray("localIDs");

                        for (int k = 0; k < arrayToCheck.length(); k++) {

                            if (arrayToCheck.getJSONObject(k).has("keyword")) {

                                // check if current value Resource
                                if (arrayToCheck.getJSONObject(k).get("keyword").toString().equals(ResourceFactory.createResource(currResource).getLocalName()) &&
                                        jsonInputObject.get("localID").toString().equals(arrayToCheck.getJSONObject(k).get("localID").toString())) {
                                    // add the current value for the if operation

                                    if (arrayToCheck.getJSONObject(k).get("value") instanceof JSONObject) {

                                        currResource = arrayToCheck.getJSONObject(k).getJSONObject("value").get("resource").toString();

                                    } else if (arrayToCheck.getJSONObject(k).get("value") instanceof String) {

                                        currResource = arrayToCheck.getJSONObject(k).get("value").toString();

                                    }

                                } else if (((arrayToCheck.getJSONObject(k).get("keyword").toString().equals(ResourceFactory.createResource(currResource).getLocalName()) &&
                                        jsonInputObject.has("useKeywordsFromComposition")))) {

                                    if (jsonInputObject.get("useKeywordsFromComposition").toString().equals("true")) {

                                        if (arrayToCheck.getJSONObject(k).get("value") instanceof JSONObject) {

                                            currResource = arrayToCheck.getJSONObject(k).getJSONObject("value").get("resource").toString();

                                        } else if (arrayToCheck.getJSONObject(k).get("value") instanceof String) {

                                            currResource = arrayToCheck.getJSONObject(k).get("value").toString();

                                        }

                                    }

                                }

                            }

                        }

                    }

                    if (!currResource.equals(generateResourceFor)&&
                            !currResource.equals(SCBasic.userEntryID.toString()) &&
                            !currResource.equals(SCBasic.coreID.toString()) &&
                            !currResource.equals(SprO.sproVARIABLEEmpty.toString())) {

                        // don't calculate for already known individual

                        IndividualURI individualURI = new IndividualURI(this.currentFocus);

                        if (this.focusHasNewNS) {

                            currSubject = individualURI.createURIForAnIndividualForANewNamespace(currResource);

                        } else {

                            currSubject = individualURI.createURIForAnIndividual(currResource, currDirectoryPath, connectionToTDB);

                        }

                        if (numberIndividualsOfClass.has(currResource)) {

                            currSubject = currSubject.substring(0, currSubject.lastIndexOf("_") + 1) + (numberIndividualsOfClass.getInt(currResource) + 1);

                            numberIndividualsOfClass.put(currResource, (numberIndividualsOfClass.getInt(currResource) + 1));

                        } else {

                            numberIndividualsOfClass.put(currResource, 1);

                        }

                    } else if (currResource.equals(SCBasic.coreID.toString())) {

                        currSubject = this.mdbCoreID;

                    } else if (currResource.equals(SCBasic.userEntryID.toString())) {

                        currSubject = this.mdbUEID;

                    } else if (currResource.equals(SprO.sproVARIABLEEmpty.toString())) {

                        currSubject = currResource;

                    } else {

                        currSubject = this.mdbEntryID;

                    }

                    currProperty = RDF.type.toString();

                    currObject = currResource;

                    String currObjectType = calculateObjectType(currProperty);

                    String currOperation = "s";

                    currInputDataObject.append("subject", currSubject);
                    currInputDataObject.append("property", currProperty);
                    currInputDataObject.append("object_data", currObject);
                    currInputDataObject.append("object_type", currObjectType);
                    currInputDataObject.append("operation", currOperation);
                    currInputDataObject.append("directory", currDirectoryPath);

                    this.generatedResources.put(sortedPropertyResources.get(index), currSubject);

                    index++;

                }

                System.out.println("generatedResources = " + this.generatedResources);

                // the named graph must calculate afterwards
                String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                for (String currResource : sortedObjectResources) {

                    currInputDataObject.append("ng", currNG);

                }

                currComponentObject.put("input_data", currInputDataObject);

                if (this.hasCreateOverlayInput) {

                    this.hasCreateOverlayInput = false;

                }

                return currComponentObject;


            } else if (mdbUEIDNotEmpty) {

                // sort resources with numbers in string
                OrderDataset orderDataset = new OrderDataset(currExecStep, ApplicationConfigurator.getPathToApplicationOntologyStore(), connectionToTDB);

                // get the sorted object resources
                ArrayList<String> sortedResources = orderDataset.getSortedObjects();

                ArrayList<String> sortedPropertyResources = orderDataset.getSortedProperties();

                String currSubject, currProperty, currObject;

                int index = 0;

                String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                for (String currResource : sortedResources) {

                    if (!currResource.equals(generateResourceFor) &&
                            !currResource.equals(SprO.sproVARIABLEEmpty.toString())) {

                        // don't calculate for already known individual

                        IndividualURI individualURI = new IndividualURI(this.mdbUEID);

                        currSubject = individualURI.createURIForAnIndividual(currResource, currDirectoryPath, connectionToTDB);

                    } else if (currResource.equals(SprO.sproVARIABLEEmpty.toString())) {

                        currSubject = currResource;

                    } else {

                        currSubject = this.mdbUEID;

                    }

                    currProperty = RDF.type.toString();

                    currObject = currResource;

                    String currObjectType = calculateObjectType(currProperty);

                    String currOperation = "s";

                    if (numberIndividualsOfClass.has(currResource)) {

                        currSubject = currSubject.substring(0, currSubject.lastIndexOf("_") + 1) + (numberIndividualsOfClass.getInt(currResource) + 1);

                        numberIndividualsOfClass.put(currResource, (numberIndividualsOfClass.getInt(currResource) + 1));

                    } else {

                        numberIndividualsOfClass.put(currResource, 1);

                    }

                    currInputDataObject.append("subject", currSubject);
                    currInputDataObject.append("property", currProperty);
                    currInputDataObject.append("object_data", currObject);
                    currInputDataObject.append("object_type", currObjectType);
                    currInputDataObject.append("operation", currOperation);
                    currInputDataObject.append("directory", currDirectoryPath);

                    this.generatedResources.put(sortedPropertyResources.get(index), currSubject);

                    index++;

                }

                //System.out.println("generatedResources = " + generatedResources);

                // the named graph must calculate afterwards
                String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                for (String currResource : sortedResources) {

                    currInputDataObject.append("ng", currNG);

                }

                currComponentObject.put("input_data", currInputDataObject);

                if (this.hasCreateOverlayInput) {

                    this.hasCreateOverlayInput = false;

                }

                return currComponentObject;

            }

        }

        if (this.hasCreateOverlayInput) {

            this.hasCreateOverlayInput = false;

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }


    /**
     * This method choose the if operation and the values for this operation from the execution step and calculate a
     * result for this operation.
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @return the next execution step or a final resource
     */
    public String executionStepIfThenElseStatement(JSONObject jsonInputObject, JSONArray currExecStep,
                                                   JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        String ifOperation = "";

        for (int j = 0; j < currExecStep.length(); j++) {

            if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.hasIFOperation.toString())) {

                ifOperation = currExecStep.getJSONObject(j).get("object").toString();

            }

        }

        ArrayList<String> inputValues = new ArrayList<>();

        if (!ifOperation.isEmpty()) {

            System.out.println("ifOperation: " + ifOperation);

            for (int j = 0; j < currExecStep.length(); j++) {

                if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.hasIFInputValue.toString())) {

                    // todo differ uri and string

                    SOCCOMASURLEncoder mdbLEncoderSomeValue = new SOCCOMASURLEncoder();

                    UrlValidator urlValidatorSomeValue = new UrlValidator();

                    if (urlValidatorSomeValue.isValid(mdbLEncoderSomeValue.encodeUrl(currExecStep.getJSONObject(j).get("object").toString(), "UTF-8"))
                            || (EmailValidator.getInstance().isValid(currExecStep.getJSONObject(j).get("object").toString()))) {

                        Resource inputValueRes = ResourceFactory.createResource(currExecStep.getJSONObject(j).get("object").toString());

                        System.out.println("inputValueRes = " + inputValueRes);

                        if (inputValueRes.toString().equals(SprO.sproVARIABLENumberOfActiveSOCCOMASSessionsOfThisUser.toString())) {

                            if (this.mdbUEIDNotEmpty) {

                                // get the number of interval start date from store

                                String timeIntervalURIWithoutNumber = this.mdbUEID + "#TimeInterval";

                                SelectBuilder selectWhereBuilder = new SelectBuilder();

                                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                                selectWhereBuilder.addWhere("?s", "<http://www.ontologydesignpatterns.org/cp/owl/timeinterval.owl#hasIntervalStartDate>","?o");

                                FilterBuilder filterBuilder = new FilterBuilder();

                                SPARQLFilter sparqlFilter = new SPARQLFilter();

                                // create an array list to collect the filter parts
                                ArrayList<String> filterCollection= new ArrayList<>();

                                // add a part to the collection
                                filterCollection.add(timeIntervalURIWithoutNumber);

                                // generate a filter string
                                ArrayList<String> filter = sparqlFilter.getRegexSTRFilter("?s", filterCollection);

                                selectWhereBuilder = filterBuilder.addFilter(selectWhereBuilder, filter);

                                SelectBuilder selectBuilder = new SelectBuilder();

                                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                                ExprVar exprVar = new ExprVar("s");

                                Aggregator aggregator = AggregatorFactory.createCountExpr(true, exprVar.getExpr());

                                ExprAggregator exprAggregator = new ExprAggregator(exprVar.asVar(), aggregator);

                                selectBuilder.addVar(exprAggregator.getExpr(), "?count");

                                selectBuilder.addGraph("?g", selectWhereBuilder);

                                String sparqlQueryString = selectBuilder.buildString();

                                TDBPath tdbPath = new TDBPath();

                                String numberOfStartDate = connectionToTDB
                                        .pullSingleDataFromTDB(
                                                tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), sparqlQueryString, "?count");

                                // get the number of interval end date from store

                                selectWhereBuilder = new SelectBuilder();

                                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                                selectWhereBuilder.addWhere("?s", "<http://www.ontologydesignpatterns.org/cp/owl/timeinterval.owl#hasIntervalEndDate>","?o");

                                filterBuilder = new FilterBuilder();

                                sparqlFilter = new SPARQLFilter();

                                // create an array list to collect the filter parts
                                filterCollection= new ArrayList<>();

                                // add a part to the collection
                                filterCollection.add(timeIntervalURIWithoutNumber);

                                // generate a filter string
                                filter = sparqlFilter.getRegexSTRFilter("?s", filterCollection);

                                selectWhereBuilder = filterBuilder.addFilter(selectWhereBuilder, filter);

                                selectBuilder = new SelectBuilder();

                                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                                selectBuilder.addVar(exprAggregator.getExpr(), "?count");

                                selectBuilder.addGraph("?g", selectWhereBuilder);

                                sparqlQueryString = selectBuilder.buildString();

                                String numberOfEndDate = connectionToTDB
                                        .pullSingleDataFromTDB(
                                                tdbPath
                                                        .getPathToTDB(
                                                                SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()),
                                                sparqlQueryString,
                                                "?count");

                                System.out.println("inputValues: " + (Integer.parseInt(numberOfStartDate) - Integer.parseInt(numberOfEndDate)));

                                // calculate input value(input = #start - #end)
                                inputValues.add(String.valueOf(Integer.parseInt(numberOfStartDate) - Integer.parseInt(numberOfEndDate)));

                            }


                        } else if (inputValueRes.toString().equals(SprO.sproVARIABLEUserInput.toString())) {

                            inputValues.add(jsonInputObject.get("value").toString());
                            System.out.println("inputValues: " + jsonInputObject.get("value").toString());

                        } else if (inputValueRes.toString().equals(SprO.sproVARIABLEUseActiveTab.toString())) {

                            calculateTabToUse(inputValueRes.toString(), jsonInputObject);

                            inputValues.add(this.tabToUseURI);
                            System.out.println("inputValues: " + this.tabToUseURI);

                        } else if (this.infoInput.has(inputValueRes.toString())) {
                            // use info input as value

                            if ((this.infoInput.get(inputValueRes.toString()).toString().equals(SprO.sproVARIABLEEmpty.toString()))) {

                                return getNextStepFromJSONArray(currExecStep, SprO.elseSOCCOMAS.toString());

                            } else {

                                inputValues.add(this.infoInput.get(inputValueRes.toString()).toString());
                                System.out.println("inputValues: " + this.infoInput.get(inputValueRes.toString()).toString());

                            }

                        } else if (this.identifiedResources.has(inputValueRes.toString())) {
                            // associated keyword from tdb

                            if (this.identifiedResources.get(inputValueRes.toString()) instanceof JSONArray) {

                                JSONArray keywordJSONArray = this.identifiedResources.getJSONArray(inputValueRes.toString());

                                for (int i = 0; i < keywordJSONArray.length(); i++) {

                                    inputValues.add(keywordJSONArray.get(i).toString());
                                    System.out.println("inputValues: " + keywordJSONArray.get(i).toString());

                                }

                            } else {

                                inputValues.add(this.identifiedResources.get(inputValueRes.toString()).toString());
                                System.out.println("inputValues: " + this.identifiedResources.get(inputValueRes.toString()).toString());

                            }

                        } else {
                            // use user input as value

                            JSONArray arrayToCheck = jsonInputObject.getJSONArray("localIDs");

                            //System.out.println("arrayToCheck = " + arrayToCheck);

                            boolean valueWasFound = false;

                            for (int k = 0; k < arrayToCheck.length(); k++) {

                                if (arrayToCheck.getJSONObject(k).has("keyword")) {

                                    // check if current value Resource
                                    if (arrayToCheck.getJSONObject(k).get("keyword").toString().equals(inputValueRes.getLocalName()) &&
                                            jsonInputObject.get("localID").toString().equals(arrayToCheck.getJSONObject(k).get("localID").toString())) {
                                        // add the current value for the if operation

                                        if (arrayToCheck.getJSONObject(k).get("value") instanceof JSONObject) {

                                            inputValues.add(arrayToCheck.getJSONObject(k).getJSONObject("value").get("resource").toString());

                                            valueWasFound = true;

                                            System.out.println("inputValues: " + arrayToCheck.getJSONObject(k).getJSONObject("value").get("resource").toString());

                                        } else if (arrayToCheck.getJSONObject(k).get("value") instanceof String) {

                                            inputValues.add(arrayToCheck.getJSONObject(k).get("value").toString());

                                            valueWasFound = true;

                                            System.out.println("inputValues: " + arrayToCheck.getJSONObject(k).get("value").toString());

                                        }

                                    } else if (((arrayToCheck.getJSONObject(k).get("keyword").toString().equals(inputValueRes.getLocalName()) &&
                                            jsonInputObject.has("useKeywordsFromComposition")))) {

                                        if (jsonInputObject.get("useKeywordsFromComposition").toString().equals("true")) {

                                            if (arrayToCheck.getJSONObject(k).get("value") instanceof JSONObject) {

                                                inputValues.add(arrayToCheck.getJSONObject(k).getJSONObject("value").get("resource").toString());

                                                valueWasFound = true;

                                                System.out.println("inputValues: " + arrayToCheck.getJSONObject(k).getJSONObject("value").get("resource").toString());

                                            } else if (arrayToCheck.getJSONObject(k).get("value") instanceof String) {

                                                inputValues.add(arrayToCheck.getJSONObject(k).get("value").toString());

                                                valueWasFound = true;

                                                System.out.println("inputValues: " + arrayToCheck.getJSONObject(k).get("value").toString());

                                            }

                                        }

                                    }

                                }

                            }

                            if (!valueWasFound) {
                                // use resource as input value

                                inputValues.add(inputValueRes.toString());

                                System.out.println("inputValues: " + inputValueRes.toString());

                            }

                        }

                    } else {

                        inputValues.add(currExecStep.getJSONObject(j).get("object").toString());

                        System.out.println("inputValues: " + currExecStep.getJSONObject(j).get("object").toString());

                    }

                }

            }
        }

        ArrayList<String> targetValues = new ArrayList<>();

        for (int j = 0; j < currExecStep.length(); j++) {

            if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.hasIFTargetValue.toString())) {

                String potentialTarget = currExecStep.getJSONObject(j).get("object").toString();

                if (potentialTarget.equals(SprO.sproVARIABLECheckKnownSPrOVariables.toString())) {

                    Iterator<String> infoInputIter = this.infoInput.keys();

                    while (infoInputIter.hasNext()) {

                        String currKey = infoInputIter.next();

                        targetValues.add(currKey);

                        System.out.println("targetValues: " + currKey);

                    }

                    Iterator<String> generatedResourcesIter = this.generatedResources.keys();

                    while (generatedResourcesIter.hasNext()) {

                        String currKey = generatedResourcesIter.next();

                        targetValues.add(currKey);

                        System.out.println("targetValues: " + currKey);

                    }

                    Iterator<String> identifiedResourcesIter = this.identifiedResources.keys();

                    while (identifiedResourcesIter.hasNext()) {

                        String currKey = identifiedResourcesIter.next();

                        targetValues.add(currKey);

                        System.out.println("targetValues: " + currKey);

                    }

                } else if (potentialTarget.contains("__SPRO_")) {

                    String localNamePropertyInObject = potentialTarget.substring(potentialTarget.indexOf("__") + 2);

                    Iterator<String> genResIterator = this.generatedResources.keys();

                    while (genResIterator.hasNext()) {

                        String currKey = genResIterator.next();

                        // get local name of a key
                        String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                        if (localNameOfKey.equals(localNamePropertyInObject)) {
                            // get already generated resource from cache

                            potentialTarget = this.generatedResources.get(currKey).toString();

                        }

                    }

                    if (jsonInputObject.has("localIDs")) {

                        JSONArray currJSONArray = jsonInputObject.getJSONArray("localIDs");

                        for (int k = 0; k < currJSONArray.length(); k++) {

                            JSONObject currJSONObject = currJSONArray.getJSONObject(k);

                            if (currJSONObject.has("keyword")) {

                                if (ResourceFactory.createResource(potentialTarget).getLocalName().equals(currJSONObject.get("keyword").toString()) &&
                                        jsonInputObject.get("localID").toString().equals(currJSONObject.get("localID").toString())) {

                                    potentialTarget = currJSONObject.get("value").toString();

                                }

                            }

                        }

                    }

                    if (this.identifiedResources.has(potentialTarget)) {

                        potentialTarget = this.identifiedResources.get(potentialTarget).toString();

                    }

                    targetValues.add(potentialTarget);

                    System.out.println("targetValues: " + potentialTarget);

                } else {

                    targetValues.add(potentialTarget);

                    System.out.println("targetValues: " + potentialTarget);

                }

            }

        }

        SOCCOMASIfThenElse SOCCOMASIfThenElse = new SOCCOMASIfThenElse();

        boolean ifDecision = SOCCOMASIfThenElse.checkCondition(ifOperation, inputValues, targetValues, connectionToTDB);

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        if (ifDecision) {

            return getNextStepFromJSONArray(currExecStep, SprO.thenSOCCOMAS.toString());

        } else {

            return getNextStepFromJSONArray(currExecStep, SprO.elseSOCCOMAS.toString());

        }

    }


    /**
     * This method add the information for a new hyperlink of a current object
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return additional modified information about the current object
     */
    public JSONObject executionStepHyperlink(JSONObject currComponentObject, JSONObject jsonInputObject,
                                             JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        System.out.println("Hyperlink case");

        String ng = "", directory = "", selectedPart = "", switchToPageURI = "", switchToOverlayURI = "";
        Boolean switchToPageExist = false, switchToOverlayExist = false, hasSelectedPartExist = false,
                isGeneralApplicationPage = false, updateComposition = false;

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                ng = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                directory = calculateWorkspaceDirectory(currExecStep);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.hasSelectedPartThisEntrySSpecificIndividualOfClass.toString())) {

                hasSelectedPartExist = true;
                selectedPart = currExecStep.getJSONObject(i).get("object").toString();

            } else if((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.switchToPage.toString())) {

                switchToPageExist = true;
                switchToPageURI = currExecStep.getJSONObject(i).get("object").toString();

            } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.switchToOverlay.toString())) {

                switchToOverlayExist = true;
                switchToOverlayURI = currExecStep.getJSONObject(i).get("object").toString();

            } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.applicationInfoMessage.toString())) {

                currComponentObject.put(ResourceFactory.createProperty(currExecStep.getJSONObject(i).get("property").toString()).getLocalName(), ResourceFactory.createPlainLiteral(currExecStep.getJSONObject(i).get("object").toString()).asLiteral().getLexicalForm());

            } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.isGeneralApplicationPageBOOLEAN.toString())) {

                isGeneralApplicationPage = true;

            } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.updateComposition.toString())) {

                updateComposition = true;

            }

        }

        if (currComponentObject.has("compositionForMDBHyperlink")) { // todo change key to "data" at a later point

            if (switchToPageExist) {
                // switch to page

                currComponentObject.put("data", currComponentObject.getJSONArray("compositionForMDBHyperlink"));

                currComponentObject.remove("compositionForMDBHyperlink");

                String potentialURL;

                if (ResourceFactory.createResource(switchToPageURI).isResource()
                        && !(this.currentFocus.equals(""))) {

                    String localName = ResourceFactory.createResource(switchToPageURI).getLocalName();

                    potentialURL = this.currentFocus + "#" + localName;

                } else {

                    potentialURL = switchToPageURI;

                }

                currComponentObject.put("load_page", potentialURL);

                try {

                    URL url = new URL(potentialURL);

                    String loadPageLocalID = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

                    currComponentObject.put("load_page_localID", loadPageLocalID);

                } catch (MalformedURLException e) {

                    System.out.println("INFO: the variable 'potentialURL' contains no valid URL.");

                }

                if (hasSelectedPartExist) {

                    String resultVarLabel = "?s";

                    PrefixesBuilder prefixesBuilderLabel = new PrefixesBuilder();

                    SelectBuilder selectBuilderLabel = new SelectBuilder();

                    selectBuilderLabel = prefixesBuilderLabel.addPrefixes(selectBuilderLabel);

                    SelectBuilder tripleSPOConstructLabel = new SelectBuilder();

                    tripleSPOConstructLabel.addWhere( "?s", RDF.type, "<" + selectedPart + ">");

                    selectBuilderLabel.addVar(selectBuilderLabel.makeVar(resultVarLabel));

                    selectBuilderLabel.addGraph("<" + ng + ">", tripleSPOConstructLabel);

                    String sparqlQueryStringLabel = selectBuilderLabel.buildString();

                    String partID = connectionToTDB.pullSingleDataFromTDB(directory, sparqlQueryStringLabel, resultVarLabel);

                    currComponentObject.put("partID", ResourceFactory.createResource(partID).getLocalName());

                }

                OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

                outputGenerator.getOutputJSONObject(currComponentObject.get("load_page_localID").toString(), jsonInputObject, currComponentObject.getJSONArray("data"));

            } else if (switchToOverlayExist) {
                // switch to overlay

                currComponentObject.put("data", currComponentObject.getJSONArray("compositionForMDBHyperlink"));

                currComponentObject.remove("compositionForMDBHyperlink");

                currComponentObject.put("widget", switchToOverlayURI);

                TDBPath tdbPath = new TDBPath();

                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                // count value in jena tdb
                SelectBuilder selectWhereBuilder = new SelectBuilder();

                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                selectWhereBuilder.addWhere("<" + ApplicationConfigurator.getDomain() + "/resource/dummy-overlay#" + ResourceFactory.createResource(switchToOverlayURI).getLocalName() + ">", RDF.value, "?o");

                SelectBuilder selectBuilder = new SelectBuilder();

                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                selectBuilder.addVar(selectBuilder.makeVar("?o"));

                selectBuilder.addGraph("<" + ApplicationConfigurator.getDomain() + "/resource/dummy-overlay#" + ResourceFactory.createResource(switchToOverlayURI).getLocalName() + ">", selectWhereBuilder);

                String sparqlQueryString = selectBuilder.buildString();

                String result = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString()), sparqlQueryString, "?o");

                String overlayNG;

                if (result.isEmpty()) {

                    overlayNG = ApplicationConfigurator.getDomain() + "/resource/dummy-overlay-" + ResourceFactory.createResource(switchToOverlayURI).getLocalName() + "_1#MDB_CORE_0000000412_1";

                } else {

                    int newResourceIndex = Integer.parseInt(result) + 1;

                    overlayNG = ApplicationConfigurator.getDomain() + "/resource/dummy-overlay-" + ResourceFactory.createResource(switchToOverlayURI).getLocalName() + "_" + newResourceIndex + "#MDB_CORE_0000000412_1";

                }

                currComponentObject.put("load_overlay", overlayNG);

                String loadOverlayLocalID = "";

                try {

                    URL url = new URL(overlayNG);

                    loadOverlayLocalID = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

                } catch (MalformedURLException e) {

                    System.out.println("INFO: the variable 'potentialURL' contains no valid URL.");

                }

                currComponentObject.put("load_overlay_localID", loadOverlayLocalID);

                OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

                outputGenerator.getOutputJSONObject(currComponentObject.get("load_overlay_localID").toString(), jsonInputObject, currComponentObject.getJSONArray("data"));

            } else if (updateComposition) {

                currComponentObject.put("data", currComponentObject.getJSONArray("compositionForMDBHyperlink"));

                System.out.println("length = " + currComponentObject.getJSONArray("compositionForMDBHyperlink").length());

                currComponentObject.remove("compositionForMDBHyperlink");

                if (hasSelectedPartExist) {

                    if (selectedPart.equals(SprO.sproVARIABLENewSelectedPart.toString())) {

                        currComponentObject.put("partID", jsonInputObject.get("localID").toString());

                    } else {

                        String resultVarLabel = "?s";

                        PrefixesBuilder prefixesBuilderLabel = new PrefixesBuilder();

                        SelectBuilder selectBuilderLabel = new SelectBuilder();

                        selectBuilderLabel = prefixesBuilderLabel.addPrefixes(selectBuilderLabel);

                        SelectBuilder tripleSPOConstructLabel = new SelectBuilder();

                        tripleSPOConstructLabel.addWhere( "?s", RDF.type, "<" + selectedPart + ">");

                        selectBuilderLabel.addVar(selectBuilderLabel.makeVar(resultVarLabel));

                        selectBuilderLabel.addGraph("<" + ng + ">", tripleSPOConstructLabel);

                        String sparqlQueryStringLabel = selectBuilderLabel.buildString();

                        String partID = connectionToTDB.pullSingleDataFromTDB(directory, sparqlQueryStringLabel, resultVarLabel);

                        currComponentObject.put("partID", ResourceFactory.createResource(partID).getLocalName());

                    }

                }

                OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

                int nullCounter = 0;

                JSONArray newIndizesMap = new JSONArray();

                for (int i = 0; i < currComponentObject.getJSONArray("data").length(); i++) {

                    if (currComponentObject.getJSONArray("data").isNull(i)) {

                        newIndizesMap.put(-1);

                        nullCounter = nullCounter + 1;

                    } else {

                        newIndizesMap.put(i - nullCounter);

                    }

                }

                outputGenerator.getOutputJSONObject(jsonInputObject.get("html_form").toString(), jsonInputObject, currComponentObject.getJSONArray("data"));

                JSONObject updatePartInnerJSONObject = new JSONObject();

                JSONArray updatePartInnerJSONArray = new JSONArray();

                for (int i = 0; i < currComponentObject.getJSONArray("data").length(); i++) {

                    for (int j = i; j < newIndizesMap.length(); j++) {

                        if (newIndizesMap.getInt(j) == i) {

                            String currIndex = String.valueOf(j + 1);

                            updatePartInnerJSONArray.put(currIndex);

                            updatePartInnerJSONObject.put(currIndex, currComponentObject.getJSONArray("data").get(i));

                        }

                    }

                }

                if (updatePartInnerJSONArray.length() > 0) {

                    // this means the input is correct
                    updatePartInnerJSONObject.put("valid", "true");

                    updatePartInnerJSONObject.put("update_position", updatePartInnerJSONArray);


                } else {

                    // this means the input is correct
                    updatePartInnerJSONObject.put("valid", "false");

                }

                JSONObject updatePartJSONObject = new JSONObject();

                updatePartJSONObject.put(jsonInputObject.get("localID").toString(), updatePartInnerJSONObject);

                JSONArray updatePartJSONArray = new JSONArray();

                updatePartJSONArray.put(updatePartJSONObject);

                currComponentObject.put("data", updatePartJSONArray);

            }

        } else {

            OperationManager operationManager;

            if (this.mdbCoreIDNotEmpty && this.mdbEntryIDNotEmpty && this.mdbUEIDNotEmpty) {

                operationManager = new OperationManager(this.mdbCoreID, this.mdbEntryID, this.mdbUEID, this.mongoDBConnection);

            } else if(this.mdbUEIDNotEmpty) {

                operationManager = new OperationManager(this.mdbUEID, this.mongoDBConnection);

            } else {

                operationManager = new OperationManager(this.mongoDBConnection);

            }

            boolean saveToStoreExist = false;
            boolean switchToEntryExist = false;
            switchToOverlayExist = false;
            String classID = "";
            String localClassID = "";
            String entryID = "";
            String entryIDRaw = "";

            for (int j = 0; j < currExecStep.length(); j++) {

                if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.switchToPage.toString())) {

                    classID = currExecStep.getJSONObject(j).get("object").toString();

                    // add output message to currComponentObject
                    localClassID = ResourceFactory.createResource(classID).getLocalName();

                    for (int k = 0; k < currExecStep.length(); k++) {

                        if ((currExecStep.getJSONObject(k).get("property").toString()).equals(SprO.switchToEntry.toString())) {

                            entryID = currExecStep.getJSONObject(k).get("object").toString();
                            entryIDRaw = entryID;

                            entryID = entryID.substring(entryID.indexOf("__") + 2);

                            Iterator<String> keyIterator = this.generatedResources.keys();

                            while (keyIterator.hasNext()) {

                                String currKey = keyIterator.next();

                                // get local name of a key
                                String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                                if (localNameOfKey.equals(entryID)) {
                                    // get ng from generated resources

                                    entryID = this.generatedResources.get(currKey).toString();

                                }

                            }

                            switchToEntryExist = true;

                        }

                    }



                } else if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.switchToOverlay.toString())) {

                    if ((!currExecStep.getJSONObject(j).get("object").toString().contains(mdbCoreID)) &&
                            (!currExecStep.getJSONObject(j).get("object").toString().contains(mdbEntryID)) &&
                            (!currExecStep.getJSONObject(j).get("object").toString().contains(mdbUEID))) {

                        currComponentObject.put("widget", SCBasic.guiCOMPONENTBASICWIDGETSpecifyRequiredInformation.getLocalName());

                    }

                    classID = currExecStep.getJSONObject(j).get("object").toString();

                    for (int k = 0; k < currExecStep.length(); k++) {

                        if ((currExecStep.getJSONObject(k).get("property").toString()).equals(SprO.switchToEntry.toString())) {

                            entryID = currExecStep.getJSONObject(k).get("object").toString();

                            entryIDRaw = entryID;

                            entryID = entryID.substring(entryID.indexOf("__") + 2);

                            Iterator<String> keyIterator = this.generatedResources.keys();

                            while (keyIterator.hasNext()) {

                                String currKey = keyIterator.next();

                                // get local name of a key
                                String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                                if (localNameOfKey.equals(entryID)) {
                                    // get ng from generated resources

                                    entryID = this.generatedResources.get(currKey).toString();

                                }

                            }

                            switchToEntryExist = true;

                            switchToOverlayExist = true;

                        }

                    }

                } else if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.closeModuleBOOLEAN.toString())) {

                    currComponentObject.put("close_old_page", ResourceFactory.createPlainLiteral(currExecStep.getJSONObject(j).get("object").toString()).asLiteral().getLexicalForm());

                } else if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.updateStoreBOOLEAN.toString())) {

                    saveToStoreExist = true;

                } else if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.applicationInfoMessage.toString())) {

                    currComponentObject.put(ResourceFactory.createProperty(currExecStep.getJSONObject(j).get("property").toString()).getLocalName(), ResourceFactory.createPlainLiteral(currExecStep.getJSONObject(j).get("object").toString()).asLiteral().getLexicalForm());

                } else if ((currExecStep.getJSONObject(j).get("property").toString()).equals(SprO.sproVariableValueTransferredToHyperlink.toString())) {

                    String keywordValueToTransfer = currExecStep.getJSONObject(j).get("object").toString();

                    if (keywordValueToTransfer.contains("__SPRO_")) {

                        keywordValueToTransfer = keywordValueToTransfer.substring(keywordValueToTransfer.indexOf("__") + 2);

                        Iterator<String> keyIterator = this.generatedResources.keys();

                        while (keyIterator.hasNext()) {

                            String currKey = keyIterator.next();

                            // get local name of a key
                            String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                            if (localNameOfKey.equals(keywordValueToTransfer)) {
                                // get ng from generated resources

                                currComponentObject.append("ngInCache", this.generatedResources.get(currKey).toString());

                            }

                        }

                    } else {

                        currComponentObject.append("ngInCache", currExecStep.getJSONObject(j).get("object").toString());

                    }

                }

            }

            if (saveToStoreExist) {

                //System.out.println("currComponentObject before save store" + currComponentObject);

                saveToStores(currComponentObject, jsonInputObject, connectionToTDB);

            }

            if (switchToEntryExist) {

                System.out.println("switchToEntryExist = " + true);

                if (jsonInputObject.has("mdbueid") && (!this.mdbUEIDNotEmpty)) {
                    // put MDBUEID from jena tdb to cache if not already exist

                    FilterBuilder filterBuilder = new FilterBuilder();

                    SelectBuilder selectBuilder = new SelectBuilder();

                    PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                    selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                    SelectBuilder innerSelect = new SelectBuilder();

                    innerSelect.addWhere("?s", RDF.type, SCBasic.user);

                    selectBuilder.addVar(selectBuilder.makeVar("?s"));

                    selectBuilder.addGraph("?g", innerSelect);

                    ArrayList<String> oneDimensionalFilterItems = new ArrayList<>();

                    oneDimensionalFilterItems.add(jsonInputObject.get("mdbueid").toString());

                    SPARQLFilter sparqlFilter = new SPARQLFilter();

                    ArrayList<String> filter = sparqlFilter.getRegexSTRFilter("?s", oneDimensionalFilterItems);

                    selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

                    String sparqlQueryString = selectBuilder.buildString();

                    TDBPath tdbPath = new TDBPath();

                    this.mdbUEID = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), sparqlQueryString, "?s");

                    this.mdbUEIDNotEmpty = true;

                }

                if ((entryID.contains(this.mdbEntryID) && this.mdbEntryIDNotEmpty) ||
                        (entryIDRaw.equals(SprO.sproVARIABLEThisEntryID.toString()) && this.mdbEntryIDNotEmpty)) {

                    if (switchToOverlayExist) {

                        FilterBuilder filterBuilder = new FilterBuilder();

                        SelectBuilder selectBuilder = new SelectBuilder();

                        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                        SelectBuilder tripleSPO = new SelectBuilder();

                        tripleSPO.addWhere("?s", RDF.type, "<" + classID + ">");

                        selectBuilder.addVar(selectBuilder.makeVar("?s"));

                        selectBuilder.addGraph("?g", tripleSPO);

                        SPARQLFilter sparqlFilter = new SPARQLFilter();

                        ArrayList<String> filterItems = new ArrayList<>();

                        filterItems.add(this.mdbEntryID);

                        ArrayList<String> filter = sparqlFilter.getRegexSTRFilter("?s", filterItems);

                        selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

                        String sparqlQueryString = selectBuilder.buildString();

                        System.out.println(sparqlQueryString);

                        TDBPath tdbPath = new TDBPath();

                        System.out.println();

                        String rootURI = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString()), sparqlQueryString, "?s");

                        System.out.println("rootURI = " + rootURI);

                        // set path to draft workspace
                        operationManager.setPathToOntologies(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString()));

                        currComponentObject = operationManager.getOutput(jsonInputObject, currComponentObject, rootURI, connectionToTDB);

                        currComponentObject.put("load_overlay", rootURI);

                        currComponentObject.put("load_overlay_localID", ResourceFactory.createResource(rootURI).getLocalName());

                    } else {

                        TDBPath tdbPath = new TDBPath();

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

                        filterItems = filterBuilder.addItems(filterItems, "?p", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
                        filterItems = filterBuilder.addItems(filterItems, "?o", "<" + classID + ">");

                        ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

                        selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

                        filterItems.clear();

                        // create an array list to collect the filter parts
                        ArrayList<String> filterCollection = new ArrayList<>();

                        filterCollection.add(this.mdbEntryID);

                        // generate a filter string
                        filter = sparqlFilter.getRegexSTRFilter("?s", filterCollection);

                        selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

                        String sparqlQueryString = selectBuilder.buildString();

                        System.out.println(sparqlQueryString);

                        String rootURI = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString()), sparqlQueryString, "?s");

                        String pathForOperationManager = tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString());

                        if (rootURI.isEmpty()) {

                            rootURI = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYCoreWorkspaceDirectory.toString()), sparqlQueryString, "?s");

                            pathForOperationManager = tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYCoreWorkspaceDirectory.toString());

                        }

                        System.out.println("rootURI = " + rootURI);

                        // set path to admin workspace
                        operationManager.setPathToOntologies(pathForOperationManager);

                        currComponentObject = operationManager.getOutput(jsonInputObject, currComponentObject, rootURI, connectionToTDB);

                        currComponentObject.put("load_page", rootURI);

                        if (rootURI.contains(ApplicationConfigurator.getDomain() + "/resource/")) {

                            try {

                                URL url = new URL(rootURI);

                                String loadPageLocalID = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

                                currComponentObject.put("load_page_localID", loadPageLocalID);

                            } catch (MalformedURLException e) {

                                System.out.println("INFO: the variable 'potentialURL' contains no valid URL.");

                            }

                        } else {

                            currComponentObject.put("load_page_localID", ResourceFactory.createResource(rootURI).getLocalName());

                        }

                    }


                } else if (entryID.contains(this.mdbCoreID) && this.mdbCoreIDNotEmpty) {

                    //System.out.println("mdbCoreID case");

                } else if ((entryID.contains(this.mdbUEID) && this.mdbUEIDNotEmpty) ||
                        entryIDRaw.equals(SprO.sproVARIABLEThisUserEntryID.toString())) {

                /*System.out.println();
                System.out.println("switchToPage = " + classID);
                System.out.println("switchToEntry = " + entryID);
                System.out.println();*/

                    TDBPath tdbPath = new TDBPath();

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

                    filterItems = filterBuilder.addItems(filterItems, "?p", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
                    filterItems = filterBuilder.addItems(filterItems, "?o", "<" + SCBasic.userEntryPanelDocument.toString() + ">");

                    ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

                    selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

                    filterItems.clear();

                    // create an array list to collect the filter parts
                    ArrayList<String> filterCollection= new ArrayList<>();

                    filterCollection.add(this.mdbUEID);

                    // generate a filter string
                    filter = sparqlFilter.getRegexSTRFilter("?s", filterCollection);

                    selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

                    String sparqlQueryString = selectBuilder.buildString();

                    System.out.println(sparqlQueryString);

                    String rootURI = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), sparqlQueryString, "?s");

                    System.out.println("rootURI = " + rootURI);

                    operationManager.setPathToOntologies(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()));

                    currComponentObject = operationManager.getOutput(jsonInputObject, currComponentObject, rootURI, connectionToTDB);

                    currComponentObject.put("load_page", rootURI);

                    if (rootURI.contains(ApplicationConfigurator.getDomain() + "/resource/")) {

                        try {

                            URL url = new URL(rootURI);

                            String loadPageLocalID = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

                            currComponentObject.put("load_page_localID", loadPageLocalID);

                        } catch (MalformedURLException e) {

                            System.out.println("INFO: the variable 'potentialURL' contains no valid URL.");

                        }

                    } else {

                        currComponentObject.put("load_page_localID", ResourceFactory.createResource(rootURI).getLocalName());

                    }

                }

            } else {

                String originalLocalID = jsonInputObject.get("localID").toString();

                jsonInputObject.put("localID", localClassID);

                System.out.println("jsonToCalculate = " + jsonInputObject);

                operationManager.setPathToOntologies(ApplicationConfigurator.getPathToApplicationOntologyStore());

                if (!classID.equals(ApplicationConfigurator.getDomain())) {

                    currComponentObject = operationManager.getOutput(jsonInputObject, currComponentObject, connectionToTDB);

                }

                jsonInputObject.put("localID", originalLocalID);

                currComponentObject.put("load_page", classID);

                if (classID.contains(ApplicationConfigurator.getDomain() + "/resource/")
                        || isGeneralApplicationPage) {

                    try {

                        URL url = new URL(classID);

                        String loadPageLocalID = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

                        currComponentObject.put("load_page_localID", loadPageLocalID);

                    } catch (MalformedURLException e) {

                        System.out.println("INFO: the variable 'potentialURL' contains no valid URL.");

                    }

                } else {

                    currComponentObject.put("load_page_localID", ResourceFactory.createResource(classID).getLocalName());

                }

            }

        }

        if (this.useTab) {

            currComponentObject.put("active_tab", this.tabToUse);

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }


    /**
     * This method executes a mdb operation, e.g. "save individual in cookie"
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @return the modified currComponentObject
     */
    public JSONObject executionStepApplicationOperation(JSONObject currComponentObject, JSONObject jsonInputObject,
                                                        JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        boolean saveToStoreExist = false, subsequentlyRedirected = false;

        String key = "", redirectToHyperlink = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.updateStoreBOOLEAN.toString())) {

                saveToStoreExist = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.applicationOperationSaveInCookieAsKey.toString())) {

                key = currExecStep.getJSONObject(i).get("object").toString();

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subsequentlyRedirectedBOOLEAN.toString())) {

                subsequentlyRedirected = true;


            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.applicationOperationRedirectToHyperlink.toString())) {

                redirectToHyperlink = calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString());

            }

        }

        if (subsequentlyRedirected) {

            currComponentObject.put("subsequently_redirected", "true");

            System.out.println("redirectToHyperlink = " + redirectToHyperlink);

            currComponentObject.put("redirect_to_hyperlink", redirectToHyperlink);

        } else {

            for (int i = 0; i < currExecStep.length(); i++) {

                if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.applicationOperationSaveIndividualInCookie.toString())) {

                    String localNamePropertyInObject = currExecStep.getJSONObject(i).get("object").toString();

                    if (localNamePropertyInObject.contains("__SPRO_")) {

                        localNamePropertyInObject = localNamePropertyInObject.substring(localNamePropertyInObject.indexOf("__") + 2);

                        Iterator<String> keyIterator = this.generatedResources.keys();

                        while (keyIterator.hasNext()) {

                            String currKey = keyIterator.next();

                            // get local name of a key
                            String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                            if (localNameOfKey.equals(localNamePropertyInObject)) {
                                // get already generated resource from cache

                                currComponentObject.put(key, this.generatedResources.get(currKey).toString());

                            }

                        }

                    }

                }

            }

        }

        if (saveToStoreExist) {

            //System.out.println("currComponentObject before save store" + currComponentObject);

            saveToStores(currComponentObject, jsonInputObject, connectionToTDB);

        }


        return currComponentObject;

    }

    /**
     * This method generate new triples. This triples must be save in a jena tdb
     * @param currComponentObject contains information about the current object
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return input information for a jena tdb
     */
    private JSONObject saveDeleteTripleStatements (JSONObject currComponentObject, JSONObject jsonInputObject,
                                                   JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        boolean executeThisStep = true;

        if (jsonInputObject.has("mdbcoreid")) {

            if (jsonInputObject.get("mdbcoreid").toString().equals(ApplicationConfigurator.getDomain() + "/resource/dummy-overlay")) {

                executeThisStep = false;

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {


                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())) {


                    } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                        if (jsonInputObject.get("html_form").toString().contains(ResourceFactory.createResource(currExecStep.getJSONObject(i).get("object").toString()).getLocalName())) {

                            this.createOverlayNG = this.mdbCoreID.substring(0, this.mdbCoreID.indexOf("resource/dummy-overlay")) + jsonInputObject.get("html_form").toString();

                            this.hasCreateOverlayInput = true;

                            executeThisStep = true;

                            System.out.println("createOverlayNG = " + this.createOverlayNG);

                        }

                    }

                }

            }

        }

        if (executeThisStep) {

            boolean saveToStoreExist = false, listExist = false, copiedIndividualNG = false, deleteTriple = false;
            JSONArray objectsJSONArray = new JSONArray(), subjectsJSONArray = new JSONArray();

            String ngIndividual = "";

            for (int i = currExecStep.length() - 1; i >= 0; i--) {

                boolean removeCurrID = false;

                if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryID.toString())) {

                    String newFocusURI = "";

                    newFocusURI = setFocusOnIndividual(currExecStep.getJSONObject(i).get("object").toString(), currExecStep, jsonInputObject, newFocusURI, connectionToTDB);

                }

                if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDIndividualOf.toString())) {

                    setFocusOnClass(jsonInputObject, connectionToTDB, currExecStep.getJSONObject(i).get("object").toString());

                }

                if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.updateStoreBOOLEAN.toString())) {

                    saveToStoreExist = true;

                }

                if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())) {

                    copiedIndividualNG = true;

                }

                if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.deleteTripleStatementBOOLEAN.toString())) {

                    String operation = calculateOperation(currExecStep);

                    if (operation.equals("d")) {

                        deleteTriple = true;

                    }

                }

                if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.subjectList.toString())) {

                    subjectsJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                    listExist = true;

                    removeCurrID = true;

                }

                if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.objectList.toString())) {

                    objectsJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                    listExist = true;

                    removeCurrID = true;

                }

                // check if generated resource is empty
                if (currExecStep.getJSONObject(i).get("object").toString().contains("__SPRO_")) {

                    String localNamePropertyInObject = currExecStep.getJSONObject(i).get("object").toString().substring(currExecStep.getJSONObject(i).get("object").toString().indexOf("__") + 2);

                    Iterator<String> genResIterator = this.generatedResources.keys();

                    while (genResIterator.hasNext()) {

                        String currKey = genResIterator.next();

                        // get local name of a key
                        String localNameOfKey = ResourceFactory.createResource(currKey).getLocalName();

                        if (localNameOfKey.equals(localNamePropertyInObject)) {
                            // get already generated resource from cache

                            if (this.generatedResources.get(currKey).toString().equals(SprO.sproVARIABLEEmpty.toString())) {

                                System.out.println("generated resource " + currKey + " is empty!");

                                if (this.hasCreateOverlayInput) {

                                    this.hasCreateOverlayInput = false;

                                }

                                // if empty skip
                                return currComponentObject;

                            }

                        }

                    }

                }

                if (removeCurrID) {
                    // remove the list information

                    currExecStep.remove(i);

                }

            }

            if (listExist) {

                if (subjectsJSONArray.length() > 0) {

                    System.out.println("length of currSubjectList = " + subjectsJSONArray.length());

                    for (int i = 0; i < subjectsJSONArray.length(); i++) {

                        JSONObject currSubjectJSONObject = new JSONObject();

                        currSubjectJSONObject.put("property", SprO.subjectSOCCOMAS.toString());

                        currSubjectJSONObject.put("object", subjectsJSONArray.get(i).toString());

                        currExecStep.put(currSubjectJSONObject);

                        currComponentObject = saveDeleteTripleStatements(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

                        currExecStep.remove(currExecStep.length() - 1);

                    }


                } else if (objectsJSONArray.length() > 0) {

                    System.out.println("length of currObjectList = " + objectsJSONArray.length());

                    for (int i = 0; i < objectsJSONArray.length(); i++) {

                        JSONObject currObjectJSONObject = new JSONObject();

                        currObjectJSONObject.put("property", SprO.objectSOCCOMAS.toString());

                        currObjectJSONObject.put("object", objectsJSONArray.get(i).toString());

                        currExecStep.put(currObjectJSONObject);

                        currComponentObject = saveDeleteTripleStatements(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

                        currExecStep.remove(currExecStep.length() - 1);

                    }

                } else {

                    System.out.println("WARN: The object list and the subject list is empty.");

                }

            } else {

                if (copiedIndividualNG
                        && deleteTriple) {

                    String subject = "", property = "", object = "", directory = "";

                    JSONObject dataToFindSubjectInTDB = new JSONObject(), dataToFindObjectInTDB = new JSONObject();

                    boolean subjectNotUnknown = false, propertyNotUnknown = false, objectNotUnknown = false;

                    for (int i = 0; i < currExecStep.length(); i++) {

                        if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())
                                || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectCopiedIndividualOf.toString())) {

                            if (!(currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEQuestionMark.toString()))) {

                                objectNotUnknown = true;
                                object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, "a", connectionToTDB);

                            }

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.propertySOCCOMAS.toString())) {

                            if (!(currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEQuestionMark.toString()))) {

                                propertyNotUnknown = true;
                                property = calculateProperty(currExecStep);

                            }

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())
                                || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {

                            if (!(currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEQuestionMark.toString()))) {

                                subjectNotUnknown = true;
                                subject = calculateSubject(dataToFindSubjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                            }

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                            directory = calculateWorkspaceDirectory(currExecStep);

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())
                                || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {

                            ngIndividual = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        }

                    }

                    JSONArray ngsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("ng");

                    // check if object already was generated in execution step 'copy and save triple statement(s)'
                    for (int i = 0; i < ngsInJSONArray.length(); i++) {

                        if (ngsInJSONArray.get(i).toString().equals(ngIndividual)) {

                            boolean wasChanged = false;

                            if (subjectNotUnknown
                                    && propertyNotUnknown
                                    && objectNotUnknown) {

                                if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString().equals(property)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString().equals(subject)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString().equals(object)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put(i, "d");

                                    wasChanged = true;

                                }

                            } else if (subjectNotUnknown
                                    && propertyNotUnknown
                                    && (!objectNotUnknown)) {

                                if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString().equals(property)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString().equals(subject)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put(i, "d");

                                    wasChanged = true;

                                }

                            } else if (subjectNotUnknown
                                    && (!propertyNotUnknown)
                                    && objectNotUnknown) {

                                if (currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString().equals(subject)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString().equals(object)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put(i, "d");

                                    wasChanged = true;

                                }

                            } else if (!subjectNotUnknown
                                    && propertyNotUnknown
                                    && objectNotUnknown) {

                                if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString().equals(property)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString().equals(object)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put(i, "d");

                                    wasChanged = true;

                                }

                            } else if (subjectNotUnknown
                                    && (!propertyNotUnknown)
                                    && (!objectNotUnknown)) {

                                if (currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString().equals(subject)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put(i, "d");

                                    wasChanged = true;

                                }

                            } else if ((!subjectNotUnknown)
                                    && propertyNotUnknown
                                    && (!objectNotUnknown)) {

                                if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString().equals(property)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put(i, "d");

                                    wasChanged = true;

                                }

                            } else if ((!subjectNotUnknown)
                                    && (!propertyNotUnknown)
                                    && objectNotUnknown) {

                                if (currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString().equals(object)
                                        && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put(i, "d");

                                    wasChanged = true;

                                }

                            }

                            if (wasChanged) {

                                System.out.println("currSubject = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString());
                                System.out.println("currProperty = " + currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString());
                                System.out.println("currNG = " + currComponentObject.getJSONObject("input_data").getJSONArray("ng").get(i).toString());
                                System.out.println("currDirectoryPath = " + currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString());
                                System.out.println("currObjectType = " + currComponentObject.getJSONObject("input_data").getJSONArray("object_type").get(i).toString());
                                System.out.println("currObject = " + currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString());
                                System.out.println("currOperation = " + currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(i).toString());

                            }

                        }

                    }

                } else {

                    JSONObject dataToFindObjectInTDB = new JSONObject();

                    if (!currComponentObject.has("input_data")) {
                        // no other statement was generated yet

                        JSONObject currInputDataObject = new JSONObject();

                        String currSubject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        currInputDataObject.append("subject", currSubject);

                        String currProperty = calculateProperty(currExecStep);

                        currInputDataObject.append("property", currProperty);

                        String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        currInputDataObject.append("ng", currNG);

                        String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                        currInputDataObject.append("directory", currDirectoryPath);

                        String currObjectType = calculateObjectType(currProperty);

                        dataToFindObjectInTDB.put("subject", currSubject);
                        dataToFindObjectInTDB.put("property", currProperty);
                        dataToFindObjectInTDB.put("ng", currNG);
                        dataToFindObjectInTDB.put("directory", currDirectoryPath);

                        String currObject = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, currObjectType, connectionToTDB);

                        currInputDataObject.append("object_data", currObject);

                        currObjectType = calculateObjectTypeForAnnotationProperty(currObject, currObjectType);

                        currInputDataObject.append("object_type", currObjectType);

                        String currOperation = calculateOperation(currExecStep);

                        currInputDataObject.append("operation", currOperation);

                        currComponentObject.put("input_data", currInputDataObject);

                    } else {

                        String currSubject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        System.out.println("currSubject = " + currSubject);

                        currComponentObject.getJSONObject("input_data").append("subject", currSubject);

                        String currProperty = calculateProperty(currExecStep);

                        System.out.println("currProperty = " + currProperty);

                        currComponentObject.getJSONObject("input_data").append("property", currProperty);

                        String currNG = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        currComponentObject.getJSONObject("input_data").append("ng", currNG);

                        System.out.println("currNG = " + currNG);

                        String currDirectoryPath = calculateWorkspaceDirectory(currExecStep);

                        currComponentObject.getJSONObject("input_data").append("directory", currDirectoryPath);

                        System.out.println("currDirectoryPath = " + currDirectoryPath);

                        String currObjectType = calculateObjectType(currProperty);

                        System.out.println("currObjectType = " + currObjectType);

                        dataToFindObjectInTDB.put("subject", currSubject);
                        dataToFindObjectInTDB.put("property", currProperty);
                        dataToFindObjectInTDB.put("ng", currNG);
                        dataToFindObjectInTDB.put("directory", currDirectoryPath);

                        String currObject = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, currObjectType, connectionToTDB);

                        System.out.println("currObject = " + currObject);

                        currComponentObject.getJSONObject("input_data").append("object_data", currObject);

                        currObjectType = calculateObjectTypeForAnnotationProperty(currObject, currObjectType);

                        System.out.println("currObjectType = " + currObjectType);

                        currComponentObject.getJSONObject("input_data").append("object_type", currObjectType);

                        String currOperation = calculateOperation(currExecStep);

                        System.out.println("currOperation = " + currOperation);

                        currComponentObject.getJSONObject("input_data").append("operation", currOperation);

                    }

                    if (saveToStoreExist) {

                        saveToStores(currComponentObject, jsonInputObject, connectionToTDB);

                    }

                }

            }

        }

        if (this.hasCreateOverlayInput) {

            this.hasCreateOverlayInput = false;

        }

        return currComponentObject;

    }

    /**
     * This method coordinates the generation of new triples. This triples must be save in a jena tdb
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return input information for a jena tdb
     */
    public JSONObject executionStepSaveDeleteTripleStatements(JSONArray currExecStep, JSONObject currComponentObject,
                                                              JSONObject jsonInputObject,
                                                              JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        if (this.multipleExecutionStepFocus) {

            if (this.executionStepFocuses.length() > 0) {

                for (int i = 0; i < this.executionStepFocuses.length(); i++) {

                    SOCCOMASIDFinder soccomasIDFinder = new SOCCOMASIDFinder(this.executionStepFocuses.get(i).toString(), connectionToTDB);

                    if (!soccomasIDFinder.hasMDBEntryID()
                            && !soccomasIDFinder.hasMDBCoreID()
                            && !soccomasIDFinder.hasMDBUEID()) {

                        SOCCOMASIDChecker mdbIDChecker = new SOCCOMASIDChecker();

                        boolean valueIsMDBID = mdbIDChecker.isMDBID(this.executionStepFocuses.get(i).toString(), connectionToTDB);

                        if (valueIsMDBID) {

                            this.executionStepFocus = this.executionStepFocuses.get(i).toString();

                        }


                    } else {

                        if (soccomasIDFinder.hasMDBEntryID()) {

                            this.executionStepFocus = soccomasIDFinder.getMDBEntryID();

                        }

                        if (!soccomasIDFinder.hasMDBEntryID()
                                && soccomasIDFinder.hasMDBCoreID()) {

                            this.executionStepFocus = soccomasIDFinder.getMDBCoreID();

                        }

                        if (!soccomasIDFinder.hasMDBEntryID()
                                && !soccomasIDFinder.hasMDBCoreID()
                                && soccomasIDFinder.hasMDBUEID()) {

                            this.executionStepFocus = soccomasIDFinder.getMDBUEID();

                        }

                    }

                    currComponentObject = saveDeleteTripleStatements(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

                }

            }

            this.multipleExecutionStepFocus = false;

        } else {

            currComponentObject = saveDeleteTripleStatements(currComponentObject, jsonInputObject, currExecStep, connectionToTDB);

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }


    /**
     * This method adds statements to the currComponentObject, which must be deleted at a later execution step in the
     * transition/workflow.
     * @param currComponentObject contains information about the current object
     * @param componentsToDeleteJSONArray contains a list of URI which must be deleted from the composition
     * @param ng contains the URI of a named model
     * @param directory contains the path to the jena tdb
     * @param mdbCompositionModel contains a composition model from the jena tdb
     * @return additional modified information about the current object
     */
    private JSONObject deleteMDBEntryComponent (JSONObject currComponentObject,JSONArray componentsToDeleteJSONArray,
                                                String ng, String directory, Model mdbCompositionModel) {

        for (int i = 0; i < componentsToDeleteJSONArray.length(); i++) {

            String currPartToDelete = componentsToDeleteJSONArray.get(i).toString();

            Selector subjectSelector = new SimpleSelector(ResourceFactory.createResource(currPartToDelete), null, null, null);

            Selector objectSelector = new SimpleSelector(null, null, currPartToDelete, null);

            StmtIterator subjectStmtIter = mdbCompositionModel.listStatements(subjectSelector);

            StmtIterator objectStmtIter = mdbCompositionModel.listStatements(objectSelector);

            while (subjectStmtIter.hasNext()) {

                Statement currStmt = subjectStmtIter.nextStatement();

                Property property = currStmt.getPredicate();

                if (property.equals(SprO.hasEntryComponent)) {

                    componentsToDeleteJSONArray.put(currStmt.getObject().asResource().toString());

                }

                currComponentObject.getJSONObject("input_data").append("subject", currPartToDelete);

                currComponentObject.getJSONObject("input_data").append("property", property.toString());

                if (currStmt.getObject().isLiteral()) {

                    currComponentObject.getJSONObject("input_data").append("object_data", currStmt.getObject().asLiteral().toString());

                    currComponentObject.getJSONObject("input_data").append("object_type", "l");

                } else {

                    currComponentObject.getJSONObject("input_data").append("object_data", currStmt.getObject().asResource().toString());

                    currComponentObject.getJSONObject("input_data").append("object_type", "r");

                }

                currComponentObject.getJSONObject("input_data").append("ng", ng);

                currComponentObject.getJSONObject("input_data").append("directory", directory);

                currComponentObject.getJSONObject("input_data").append("operation", "d");

            }

            while (objectStmtIter.hasNext()) {

                Statement currStmt = objectStmtIter.nextStatement();

                Property property = currStmt.getPredicate();

                if (property.equals(OWL2.annotatedSource)) {
                    // delete axioms of the component

                    StmtIterator axiomIter = currStmt.getSubject().listProperties();

                    while (axiomIter.hasNext()) {

                        Statement currAxiomStmt = axiomIter.nextStatement();

                        currComponentObject.getJSONObject("input_data").append("subject", currAxiomStmt.getSubject().toString());

                        currComponentObject.getJSONObject("input_data").append("property", property.toString());

                        if (currStmt.getObject().isLiteral()) {

                            currComponentObject.getJSONObject("input_data").append("object_data", currStmt.getObject().asLiteral().toString());

                            currComponentObject.getJSONObject("input_data").append("object_type", "l");

                        } else {

                            currComponentObject.getJSONObject("input_data").append("object_data", currStmt.getObject().asResource().toString());

                            currComponentObject.getJSONObject("input_data").append("object_type", "r");

                        }

                        currComponentObject.getJSONObject("input_data").append("ng", ng);

                        currComponentObject.getJSONObject("input_data").append("directory", directory);

                        currComponentObject.getJSONObject("input_data").append("operation", "d");

                    }

                }

                currComponentObject.getJSONObject("input_data").append("subject", currStmt.getSubject().toString());

                currComponentObject.getJSONObject("input_data").append("property", currStmt.getPredicate().toString());

                currComponentObject.getJSONObject("input_data").append("object_data", currPartToDelete);

                currComponentObject.getJSONObject("input_data").append("object_type", "r");
                // is definitive a resource, because it is the part to delete

                currComponentObject.getJSONObject("input_data").append("ng", ng);

                currComponentObject.getJSONObject("input_data").append("directory", directory);

                currComponentObject.getJSONObject("input_data").append("operation", "d");

            }

        }

        return  currComponentObject;

    }



    /**
     * This method provides a query to find all axioms for a resource in a jena tdb.
     * @param resource contains an URI
     * @param directory contains the directory
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a model with all statements for a resource
     */
    private Model findAxiomTriple(String resource, String directory, JenaIOTDBFactory connectionToTDB) {

        PrefixesBuilder prefixesAxiomBuilder = new PrefixesBuilder();

        ConstructBuilder constructAxiomBuilder = new ConstructBuilder();

        constructAxiomBuilder = prefixesAxiomBuilder.addPrefixes(constructAxiomBuilder);

        constructAxiomBuilder.addConstruct("?s", "?p", "?o");

        SelectBuilder tripleAxiomSPOConstruct = new SelectBuilder();

        tripleAxiomSPOConstruct.addWhere("?s", "?p", "?o");
        tripleAxiomSPOConstruct.addWhere("?s", OWL2.annotatedSource, "<" + resource + ">");

        constructAxiomBuilder.addGraph("?g", tripleAxiomSPOConstruct);

        String sparqlQueryAxiomString = constructAxiomBuilder.buildString();

        return connectionToTDB.pullDataFromTDB(directory, sparqlQueryAxiomString);

    }


    /**
     * This method provides a query to find all statements for a subject resource in a jena tdb.
     * @param resource contains an URI
     * @param directory contains the directory
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a model with all statements for a subject resource
     */
    private Model findTriple(String resource, String directory, JenaIOTDBFactory connectionToTDB) {

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        ConstructBuilder constructBuilder = new ConstructBuilder();

        constructBuilder = prefixesBuilder.addPrefixes(constructBuilder);

        constructBuilder.addConstruct("<" + resource + ">", "?p", "?o");

        SelectBuilder tripleSPOConstruct = new SelectBuilder();

        tripleSPOConstruct.addWhere("<" + resource + ">", "?p", "?o");

        constructBuilder.addGraph("?g", tripleSPOConstruct);

        String sparqlQueryString = constructBuilder.buildString();

        return connectionToTDB.pullDataFromTDB(directory, sparqlQueryString);

    }


    /**
     * This method calculates and formats the output for a mdb composition.
     * @param root contains the URI of a root element
     * @param ngs contains the URI of a named graph which contains the root element
     * @param directory contains the path to the directory which contains the root element
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a formatted JSONArray
     */
    public JSONArray getCompositionFromStoreForOutput(String root, JSONArray ngs, String directory, JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        Model unionNGModel = ModelFactory.createDefaultModel(), entryComponentsModel = ModelFactory.createDefaultModel();

        for (int j = 0; j < ngs.length(); j++) {

            unionNGModel = unionNGModel.union(connectionToTDB.pullNamedModelFromTDB(directory, ngs.get(j).toString()));

        }

        ResIterator resIter = unionNGModel.listSubjects();

        while (resIter.hasNext()) {

            Resource entryComponentURI = resIter.next();

            if (unionNGModel.contains(entryComponentURI, RDF.type, OWL2.NamedIndividual)) {

                Selector tripleSelector = new SimpleSelector(entryComponentURI, null, null, "");

                StmtIterator tripleStmts = unionNGModel.listStatements(tripleSelector);

                while (tripleStmts.hasNext()) {

                    Statement stmt = tripleStmts.nextStatement();

                    Resource currSubject = stmt.getSubject();

                    Property currProperty = stmt.getPredicate();

                    Resource currLabelObject;

                    if (stmt.getObject().isURIResource()) {

                        currLabelObject = stmt.getObject().asResource();

                        if (currSubject.equals(entryComponentURI)
                                && currProperty.equals(RDF.type)
                                && !currLabelObject.equals(OWL2.NamedIndividual)) {

                            Selector classSelector = new SimpleSelector(currLabelObject, null, null, "");

                            StmtIterator classStmts = unionNGModel.listStatements(classSelector);

                            Resource classSubject = null;

                            while (classStmts.hasNext()) {

                                Statement classStmt = classStmts.nextStatement();

                                classSubject = classStmt.getSubject();

                                if ((!classStmt.getObject().equals(OWL2.Class))
                                        && (!classStmt.getPredicate().equals(RDFS.label))
                                        && (!classStmt.getPredicate().equals(RDFS.subClassOf))
                                        && (!classStmt.getPredicate().equals(OWL2.annotatedTarget))
                                        && (!classStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                    entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, classStmt.getPredicate(), classStmt.getObject()));

                                }

                            }

                            if (unionNGModel.contains(null, OWL2.annotatedSource, classSubject)) {

                                ResIterator axiomsForClassSubject = unionNGModel.listSubjectsWithProperty(OWL2.annotatedSource, classSubject);

                                while (axiomsForClassSubject.hasNext()) {

                                    Resource axiomClassSubject = axiomsForClassSubject.next();

                                    Selector axiomClassSelector = new SimpleSelector(axiomClassSubject, null, null, "");

                                    StmtIterator axiomClassStmts = unionNGModel.listStatements(axiomClassSelector);

                                    while (axiomClassStmts.hasNext()) {

                                        Statement axiomClassStmt = axiomClassStmts.nextStatement();

                                        if ((!axiomClassStmt.getObject().equals(OWL2.Axiom))
                                                && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedSource))
                                                && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                            entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, axiomClassStmt.getPredicate(), axiomClassStmt.getObject()));

                                        }

                                    }

                                }

                            }

                        }

                    }

                    entryComponentsModel.add(stmt);

                }

                if (unionNGModel.contains(null, OWL2.annotatedSource, entryComponentURI)) {

                    ResIterator axiomsForSubject = unionNGModel.listSubjectsWithProperty(OWL2.annotatedSource, entryComponentURI);

                    while (axiomsForSubject.hasNext()) {

                        Resource axiomSubject = axiomsForSubject.next();

                        Selector axiomSelector = new SimpleSelector(axiomSubject, null, null, "");

                        StmtIterator axiomStmts = unionNGModel.listStatements(axiomSelector);

                        while (axiomStmts.hasNext()) {

                            Statement axiomStmt = axiomStmts.nextStatement();

                            if ((!axiomStmt.getObject().equals(OWL2.Axiom))
                                    && (!axiomStmt.getPredicate().equals(OWL2.annotatedSource))
                                    && (!axiomStmt.getPredicate().equals(OWL2.annotatedTarget))
                                    && (!axiomStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                entryComponentsModel.add(ResourceFactory.createStatement(entryComponentURI, axiomStmt.getPredicate(), axiomStmt.getObject()));

                            }

                        }

                    }

                }

            }

        }

        StmtIterator entryComponentsModelIter = entryComponentsModel.listStatements();

        OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

        JSONObject entryComponents = new JSONObject();

        while (entryComponentsModelIter.hasNext()) {

            Statement resStmt = entryComponentsModelIter.nextStatement();

            String currSubject = resStmt.getSubject().toString();

            entryComponents = outputGenerator
                    .manageProperty(currSubject, resStmt, entryComponents, jsonInputObject, connectionToTDB);

        }

        entryComponents = outputGenerator.reorderEntryComponentsValues(entryComponents);

        Iterator<String> iter = entryComponents.keys();

        JSONArray outputDataJSON = new JSONArray();

        while (iter.hasNext()) {

            String currKey = iter.next();

            JSONObject wrapperJSON = new JSONObject();

            wrapperJSON.put(currKey, entryComponents.getJSONObject(currKey));

            outputDataJSON.put(wrapperJSON);

        }

        outputDataJSON = outputGenerator.orderOutputJSON(root, outputDataJSON);

        return outputDataJSON;

    }


    /**
     * This method is a getter for the overlay named graph.
     * @return a jena model for a MDB overlay
     */
    public Model getOverlayModel() {

        return this.overlayModel;

    }


    /**
     * This method calculates the URIs for tracking procedures of a parent transition and save them in an JSONArray
     * @param parentTransition contains an URI which has other transitions as an object
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an JSONArray with URI(s) of tracking procedures
     */
    private JSONArray getTrackingProcedures(String parentTransition, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        selectBuilder.addVar(selectBuilder.makeVar("?o"));

        SelectBuilder tripleSPO = new SelectBuilder();

        tripleSPO = prefixesBuilder.addPrefixes(tripleSPO);

        UrlValidator urlValidator = new UrlValidator();

        if (urlValidator.isValid(parentTransition)) {

            tripleSPO.addWhere("<" + parentTransition + ">", SprO.involvesTrackingProcedure, "?o");

        } else {

            tripleSPO.addWhere("<" + parentTransition + ">", SprO.involvesTrackingProcedure, "?o");

        }

        selectBuilder.addGraph("?g", tripleSPO);

        String sparqlQueryString = selectBuilder.buildString();

        return connectionToTDB.pullMultipleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?o");

    }


    /**
     * This method orders information for the calculation of the transition
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return input information for a jena tdb
     */
    public JSONObject executionStepTriggerWorkflowAction(JSONObject currComponentObject, JSONObject jsonInputObject,
                                                         JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        // calculate the start date
        long executionStart = System.currentTimeMillis();

        boolean startTransition = false;

        String nextTransition = "";

        boolean subsequentlyTriggeredWA = false;

        JSONObject localIdentifiedResources = new JSONObject();

        String subsequentlyRoot = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.sproVariableValueTransferredToTriggeredAction.toString())) {

                String localNameKeyword = ResourceFactory.createResource(currExecStep.getJSONObject(i).get("object").toString()).getLocalName();

                if (jsonInputObject.has("localIDs")) {

                    JSONArray inputLocalIDs = jsonInputObject.getJSONArray("localIDs");

                    if (inputLocalIDs != null) {

                        for (int j = 0; j < inputLocalIDs.length(); j++) {

                            if (inputLocalIDs.getJSONObject(j).has("keyword")) {

                                if ((inputLocalIDs.getJSONObject(j).get("keyword").toString()).equals(localNameKeyword)) {

                                    if (inputLocalIDs.getJSONObject(j).get("value") instanceof JSONObject) {

                                        localIdentifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), inputLocalIDs.getJSONObject(j).getJSONObject("value").get("resource").toString());

                                    } else if (inputLocalIDs.getJSONObject(j).get("value") instanceof String) {

                                        localIdentifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), inputLocalIDs.getJSONObject(j).get("value").toString());

                                    }

                                }

                            }

                            if (inputLocalIDs.getJSONObject(j).has("keywordLabel")) {

                                if ((inputLocalIDs.getJSONObject(j).get("keywordLabel").toString()).equals(localNameKeyword)) {

                                    localIdentifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), inputLocalIDs.getJSONObject(j).get("valueLabel").toString());

                                }

                            }

                            if (inputLocalIDs.getJSONObject(j).has("keywordDefinition")) {

                                if ((inputLocalIDs.getJSONObject(j).get("keywordDefinition").toString()).equals(localNameKeyword)) {

                                    if (inputLocalIDs.getJSONObject(j).has("valueDefinition")) {
                                        // todo remove the if condition if keyword for valueDefinition is implemented!

                                        localIdentifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), inputLocalIDs.getJSONObject(j).get("valueDefinition").toString());

                                    }

                                }

                            }

                        }

                    }

                }

                if (jsonInputObject.has("precedingKeywords")) {

                    JSONObject inputPrecedingKeywords = jsonInputObject.getJSONObject("precedingKeywords");

                    if (inputPrecedingKeywords.has(currExecStep.getJSONObject(i).get("object").toString())) {

                        localIdentifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), inputPrecedingKeywords.get(currExecStep.getJSONObject(i).get("object").toString()).toString());

                    }

                }

                if (this.infoInput.keys().hasNext()) {

                    Iterator<String> infoInputKeys = infoInput.keys();

                    while (infoInputKeys.hasNext()) {

                        String currKey = infoInputKeys.next();

                        String localCurrKey = currKey;

                        UrlValidator keyURLValidator = new UrlValidator();

                        // get a MDB url Encoder to encode the uri with utf-8
                        SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

                        if (keyURLValidator.isValid(soccomasURLEncoder.encodeUrl(localCurrKey, "UTF-8"))) {

                            localCurrKey = ResourceFactory.createResource(localCurrKey).getLocalName();

                        }

                        if (localCurrKey.equals(localNameKeyword)) {

                            localIdentifiedResources.put(currExecStep.getJSONObject(i).get("object").toString(), this.infoInput.get(currKey).toString());

                        }

                    }

                }

            } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.triggersWorkflowAction.toString())) {

                nextTransition = currExecStep.getJSONObject(i).get("object").toString();

                startTransition = true;

            } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.updateStoreBOOLEAN.toString())) {

                //System.out.println("currComponentObject before save store" + currComponentObject);

                saveToStores(currComponentObject, jsonInputObject, connectionToTDB);

            } else if((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.subsequentlyTriggeredWorkflowActionBOOLEAN.toString())) {

                subsequentlyTriggeredWA = true;

            } else if((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.triggerActionOfButtonOfClass.toString())) {

                subsequentlyRoot = currExecStep.getJSONObject(i).get("object").toString();

            } else {

                boolean useAsInput = useObjectAsInput(currExecStep.getJSONObject(i).get("property").toString(), connectionToTDB);

                if (useAsInput) {

                    String uriOfIndividual;

                    if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEEmpty.toString())) {

                        uriOfIndividual = currExecStep.getJSONObject(i).get("object").toString();

                    } else {

                        uriOfIndividual = getKeywordIndividualFromProperty(currExecStep.getJSONObject(i).get("property").toString(), connectionToTDB);

                    }

                    this.infoInput.put(uriOfIndividual, currExecStep.getJSONObject(i).get("object").toString());

                    System.out.println();
                    System.out.println("uriOfIndividual = " + uriOfIndividual);
                    System.out.println();

                } else {

                    System.out.println("-------------");
                    System.out.println("Else - Branch");
                    System.out.println("-------------");
                    System.out.println();
                    System.out.println("property: " + currExecStep.getJSONObject(i).get("property").toString());
                    System.out.println("object: " + currExecStep.getJSONObject(i).get("object").toString());
                    System.out.println();

                }

            }

        }

        if (subsequentlyTriggeredWA) {

            currComponentObject.put("subsequently_workflow_action", "true");

            currComponentObject.put("subsequently_root", subsequentlyRoot);

            if (localIdentifiedResources.keys().hasNext()) {

                JSONObject transferKeywordToSubsequentlyActionDummy = new JSONObject();

                Iterator<String> localIdentifiedResourcesKeys = localIdentifiedResources.keys();

                while (localIdentifiedResourcesKeys.hasNext()) {

                    String currIdentifiedResource = localIdentifiedResourcesKeys.next();

                    if (this.infoInput.has(currIdentifiedResource)) {

                        transferKeywordToSubsequentlyActionDummy.put(currIdentifiedResource, this.infoInput.get(currIdentifiedResource).toString());

                    }

                }

                currComponentObject.put("keywords_to_transfer" ,transferKeywordToSubsequentlyActionDummy);

            }

        } else {

            Iterator<String> localIdentifiedResourcesKeys = localIdentifiedResources.keys();

            while (localIdentifiedResourcesKeys.hasNext()) {

                String currIdentifiedResource = localIdentifiedResourcesKeys.next();

                this.identifiedResources.put(currIdentifiedResource, localIdentifiedResources.get(currIdentifiedResource).toString());

            }

            if (startTransition) {

                KBOrder kbOrder = new KBOrder(connectionToTDB, this.pathToOntologies, nextTransition);

                if (kbOrder.getSortedKBIndicesJSONArray().length() > 0) {

                    // get the sorted input knowledge base
                    JSONArray sortedKBJSONArray = kbOrder.getSortedKBJSONArray();

                    // get the sorted indices of the knowledge base
                    JSONArray sortedKBIndicesJSONArray = kbOrder.getSortedKBIndicesJSONArray();

                    SOCCOMASExecutionStepHandler soccomasExecutionStepHandler;

                    if (this.mdbCoreIDNotEmpty && this.mdbEntryIDNotEmpty && this.mdbUEIDNotEmpty) {

                        if (this.infoInput.length() != 0) {

                            soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.mdbCoreID, this.mdbEntryID, this.mdbUEID, this.identifiedResources, this.infoInput, this.overlayModel, this.mongoDBConnection);

                        } else {

                            soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.mdbCoreID, this.mdbEntryID, this.mdbUEID, this.identifiedResources, this.overlayModel, this.mongoDBConnection);

                        }

                    } else if(this.mdbUEIDNotEmpty) {

                        if (this.infoInput.length() != 0) {

                            soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.mdbUEID, this.identifiedResources, this.infoInput, this.overlayModel, this.mongoDBConnection);

                        } else {

                            soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.mdbUEID, this.identifiedResources, this.overlayModel, this.mongoDBConnection);

                        }

                    } else {

                        if (this.infoInput.length() != 0) {

                            soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.identifiedResources, this.infoInput, this.overlayModel, this.mongoDBConnection);

                        } else {

                            soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.identifiedResources, this.overlayModel, this.mongoDBConnection);

                        }

                    }

                    soccomasExecutionStepHandler.convertKBToJSONObject(sortedKBJSONArray, sortedKBIndicesJSONArray, currComponentObject, jsonInputObject, connectionToTDB);

                    this.overlayModel = soccomasExecutionStepHandler.getOverlayModel();

                }

                boolean updateStoreAfterTrackingProcedure = updateStoreAfterTrackingProcedureExist(nextTransition, connectionToTDB);

                if (updateStoreAfterTrackingProcedure) {

                    System.out.println("updateStoreAfterTrackingProcedure = " + true);

                    JSONArray trackingProcedures = getTrackingProcedures(nextTransition, connectionToTDB);

                    System.out.println();

                    for (int i = 0; i < trackingProcedures.length(); i++) {

                        System.out.println("tracking procedure " + i + " = " + trackingProcedures.get(i).toString());

                        KBOrder trackingKBOrder = new KBOrder(connectionToTDB, this.pathToOntologies, trackingProcedures.get(i).toString());

                        // get the sorted input knowledge base
                        JSONArray sortedKBJSONArray = trackingKBOrder.getSortedKBJSONArray();

                        // get the sorted indices of the knowledge base
                        JSONArray sortedKBIndicesJSONArray = trackingKBOrder.getSortedKBIndicesJSONArray();

                        convertKBToJSONObject(sortedKBJSONArray, sortedKBIndicesJSONArray, currComponentObject, jsonInputObject, connectionToTDB);

                    }

                    saveToStores(currComponentObject, jsonInputObject, connectionToTDB);

                }

            }

        }

        return currComponentObject;

    }


    /**
     * This method generate new triples. The triples must be saved or deleted from a jena tdb
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return input information for a jena tdb
     */
    public JSONObject executionStepUpdateTripleStatements (JSONArray currExecStep, JSONObject currComponentObject,
                                                           JSONObject jsonInputObject,
                                                           JenaIOTDBFactory connectionToTDB) {

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryIDForThisExecutionStep.toString())) {

                calculateFocusForExecutionStep(currExecStep.getJSONObject(i).get("object").toString(), connectionToTDB);

            }

        }

        boolean executeThisStep = true;

        if (jsonInputObject.has("mdbcoreid")) {

            if (jsonInputObject.get("mdbcoreid").toString().equals(ApplicationConfigurator.getDomain() + "/resource/dummy-overlay")) {

                executeThisStep = false;

                for (int i = 0; i < currExecStep.length(); i++) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {


                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())) {


                    } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.loadFromSaveToUpdateInNamedGraphThisEntrySSpecificIndividualOf.toString())) {

                        if (jsonInputObject.get("html_form").toString().contains(ResourceFactory.createResource(currExecStep.getJSONObject(i).get("object").toString()).getLocalName())) {

                            this.createOverlayNG = this.mdbCoreID.substring(0, this.mdbCoreID.indexOf("resource/dummy-overlay")) + jsonInputObject.get("html_form").toString();

                            this.hasCreateOverlayInput = true;

                            executeThisStep = true;

                            System.out.println("createOverlayNG = " + this.createOverlayNG);

                        }

                    }

                }

            }

        }

        if (executeThisStep) {

            boolean saveToStoreExist = false;


            for (int i = 0; i < currExecStep.length(); i++) {

                if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.setNewFocusOnEntryID.toString())) {

                    String newFocusURI = "";

                    newFocusURI = setFocusOnIndividual(currExecStep.getJSONObject(i).get("object").toString(), currExecStep, jsonInputObject, newFocusURI, connectionToTDB);

                } else if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.updateStoreBOOLEAN.toString())) {

                    saveToStoreExist = true;

                }

            }

            if (this.multipleExecutionStepFocus) {

                if (this.executionStepFocuses.length() > 0) {

                    for (int i = 0; i < this.executionStepFocuses.length(); i++) {

                        SOCCOMASIDFinder soccomasIDFinder = new SOCCOMASIDFinder(this.executionStepFocuses.get(i).toString(), connectionToTDB);

                        if (!soccomasIDFinder.hasMDBEntryID()
                                && !soccomasIDFinder.hasMDBCoreID()
                                && !soccomasIDFinder.hasMDBUEID()) {

                            SOCCOMASIDChecker mdbIDChecker = new SOCCOMASIDChecker();

                            boolean valueIsMDBID = mdbIDChecker.isMDBID(this.executionStepFocuses.get(i).toString(), connectionToTDB);

                            if (valueIsMDBID) {

                                this.executionStepFocus = this.executionStepFocuses.get(i).toString();

                            }


                        } else {

                            if (soccomasIDFinder.hasMDBEntryID()) {

                                this.executionStepFocus = soccomasIDFinder.getMDBEntryID();

                            }

                            if (!soccomasIDFinder.hasMDBEntryID()
                                    && soccomasIDFinder.hasMDBCoreID()) {

                                this.executionStepFocus = soccomasIDFinder.getMDBCoreID();

                            }

                            if (!soccomasIDFinder.hasMDBEntryID()
                                    && !soccomasIDFinder.hasMDBCoreID()
                                    && soccomasIDFinder.hasMDBUEID()) {

                                this.executionStepFocus = soccomasIDFinder.getMDBUEID();

                            }

                        }

                        JSONArray loopSpecificExecStep = new JSONArray(currExecStep.toString());

                        currComponentObject = getStatementToUpdate(loopSpecificExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                    }

                }

                this.multipleExecutionStepFocus = false;

            } else {

                currComponentObject = getStatementToUpdate(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

            }

            if (saveToStoreExist) {

                saveToStores(currComponentObject, jsonInputObject, connectionToTDB);

            }

        }

        if (this.hasCreateOverlayInput) {

            this.hasCreateOverlayInput = false;

        }

        if (this.hasExecutionStepFocus) {

            this.hasExecutionStepFocus = false;

        }

        return currComponentObject;

    }


    /**
     * This method provide a default composition of an entry
     * @param defaultCompositionNGURI contains the uri of the named graph.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a default model from the directory workspace
     */
    private Model findRootIndividual(String defaultCompositionNGURI, JSONArray currExecStep, JenaIOTDBFactory connectionToTDB) {

        String currDirectoryPath = copyFromWorkspace(currExecStep);

        Model defaultCompositionModel;

        if (connectionToTDB.modelExistInTDB(currDirectoryPath, defaultCompositionNGURI)) {

            defaultCompositionModel = connectionToTDB.pullNamedModelFromTDB(currDirectoryPath, defaultCompositionNGURI);

        } else {

            System.out.println(defaultCompositionNGURI + " is empty!");

            JSONArray classToCheck = new JSONArray();

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

            filterItems = filterBuilder.addItems(filterItems, "?p", "<" + SCBasic.isRootEntryComponentOfCompositionContainedInNamedGraph.toString() + ">");

            filterItems = filterBuilder.addItems(filterItems, "?o", "<" + defaultCompositionNGURI + ">");

            ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

            selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

            String sparqlQueryString = selectBuilder.buildString();

            String subject = connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?s");

            classToCheck.put(subject);

            defaultCompositionModel = calculateDefaultEntryComposition(classToCheck, connectionToTDB);

            // save named graph in jena tdb
            System.out.println(connectionToTDB.addModelDataInTDB(currDirectoryPath, defaultCompositionNGURI, defaultCompositionModel));

        }

        return defaultCompositionModel;

    }


    /**
     * This method finds a literal from a workspace.
     * @param subjectFilter contains a part of the subject to specify the correct individual in the store
     * @param property contains a property to find the corresponding range
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a literal from a workspace
     */
    private String getLiteralFromStore (String subjectFilter, String property, String pathToWorkspace, JenaIOTDBFactory connectionToTDB) {

        FilterBuilder filterBuilder = new FilterBuilder();

        SelectBuilder selectFNBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectFNBuilder = prefixesBuilder.addPrefixes(selectFNBuilder);

        SelectBuilder innerFNSelect = new SelectBuilder();

        innerFNSelect.addWhere("?s", "<" + property + ">", "?o");

        SPARQLFilter sparqlFNFilter = new SPARQLFilter();

        ArrayList<String> filterFNItems = new ArrayList<>();

        filterFNItems.add(subjectFilter);

        ArrayList<String> filterFN = sparqlFNFilter.getRegexSTRFilter("?s", filterFNItems);

        innerFNSelect = filterBuilder.addFilter(innerFNSelect, filterFN);

        selectFNBuilder.addVar(selectFNBuilder.makeVar("?o"));

        selectFNBuilder.addGraph("?g", innerFNSelect);

        String sparqlQueryString = selectFNBuilder.buildString();

        return connectionToTDB.pullSingleDataFromTDB(pathToWorkspace, sparqlQueryString, "?o");

    }


    /**
     * This method get an individual keyword uri related to the property from the jena tdb
     * @param infoInputProperty contains the uri of a property
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the uri of an individual keyword
     */
    private String getKeywordIndividualFromProperty (String infoInputProperty, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        SelectBuilder innerSelect = new SelectBuilder();

        innerSelect.addWhere("?s", SprO.valueDefinedThroughProperty, "<" + infoInputProperty + ">");

        selectBuilder.addVar(selectBuilder.makeVar("?s"));

        selectBuilder.addGraph("?g", innerSelect);

        String sparqlQueryString = selectBuilder.buildString();

        return connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?s");

    }


    /**
     * This method calculates the next step.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param annotationProperty contains an annotation property with a potential next step
     * @return the next step
     */
    private String getNextStepFromJSONArray(JSONArray currExecStep, String annotationProperty) {

        String nextStep = "";

        for (int j = 0; j < currExecStep.length(); j++) {

            if (currExecStep.getJSONObject(j).get("property").toString().equals(annotationProperty)) {

                nextStep = currExecStep.getJSONObject(j).get("object").toString();

                System.out.println("next step: " + nextStep);

            }


        }

        return nextStep;

    }


    /**
     * This method find the statement(s) to updated in the jena tdb and delete the old statement(s) and calculate the new
     * statement(s).
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSON Object with statement for the update
     */
    private JSONObject getStatementToUpdate(JSONArray currExecStep, JSONObject currComponentObject, JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {


        boolean listExist = false;
        JSONArray objectsJSONArray = new JSONArray(), subjectsJSONArray = new JSONArray();

        for (int i = 0; i  < currExecStep.length(); i++) {

            boolean removeCurrID = false;

            if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.subjectList.toString())) {

                subjectsJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                listExist = true;

                removeCurrID = true;

            }

            if ((currExecStep.getJSONObject(i).get("property").toString()).equals(SprO.objectList.toString())) {

                objectsJSONArray = calculateValueListForKeyword(currExecStep.getJSONObject(i).get("object").toString());

                listExist = true;

                removeCurrID = true;

            }

            if (removeCurrID) {
                // remove the list information

                currExecStep.remove(i);

            }

        }

        if (listExist) {

            if (subjectsJSONArray.length() > 0) {

                System.out.println("length of currSubjectList = " + subjectsJSONArray.length());

                for (int i = 0; i < subjectsJSONArray.length(); i++) {

                    JSONObject currSubjectJSONObject = new JSONObject();

                    currSubjectJSONObject.put("property", SprO.subjectSOCCOMAS.toString());

                    currSubjectJSONObject.put("object", subjectsJSONArray.get(i).toString());

                    currExecStep.put(currSubjectJSONObject);

                    currComponentObject = getStatementToUpdate(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                    currExecStep.remove(currExecStep.length() - 1);

                }

            } else if (objectsJSONArray.length() > 0) {

                System.out.println("length of currObjectList = " + objectsJSONArray.length());

                for (int i = 0; i < objectsJSONArray.length(); i++) {

                    JSONObject currObjectJSONObject = new JSONObject();

                    currObjectJSONObject.put("property", SprO.objectSOCCOMAS.toString());

                    currObjectJSONObject.put("object", objectsJSONArray.get(i).toString());

                    currExecStep.put(currObjectJSONObject);

                    currComponentObject = getStatementToUpdate(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                    currExecStep.remove(currExecStep.length() - 1);

                }

            } else {

                System.out.println("WARN: The object list and the subject list is empty.");

            }

            return currComponentObject;

        }

        JSONObject updateStatement = new JSONObject();
        JSONObject updateAxiomStatement = new JSONObject();

        boolean copiedIndividualNG = false, copiedSubject = false, copiedObject = false, highThenExist = false;
        String ngIndividual = "";
        int higherThenValue = -1;

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())) {

                copiedIndividualNG = true;
                ngIndividual = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectCopiedIndividualOf.toString())) {

                copiedObject = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {
                // subject (copied individual of)

                copiedSubject = true;

            } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.updateOnlyForValuesHigherThan.toString())) {
                // update only for values higher than

                highThenExist = true;

                higherThenValue = Integer.parseInt(calculateValueForKeyword(currExecStep.getJSONObject(i).get("object").toString()));

            }

        }

        if (copiedIndividualNG
                || copiedSubject
                || copiedObject) {

            boolean updateObjectInput = false;
            boolean updateSubjectInput = false;

            String  updateWithResourceOrValue = "", subject = "", property = "", object = "", directory = "";

            JSONObject dataToFindSubjectInTDB = new JSONObject(), dataToFindObjectInTDB = new JSONObject();

            for (int i = 0; i < currExecStep.length(); i++) {

                if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEToBeUpdated.toString())) {

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())) {

                        updateObjectInput = true;

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())) {

                        updateSubjectInput = true;

                    }

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.updateWithResourceValue.toString())) {

                    updateWithResourceOrValue = currExecStep.getJSONObject(i).get("object").toString();

                    if (updateWithResourceOrValue.equals(SprO.iNPUTCONTROLDateTimeStamp.toString())) {

                        updateWithResourceOrValue = this.soccomasDat.getDate();

                    } else if (updateWithResourceOrValue.equals(SprO.sproVARIABLEThisUserID.toString())) {

                        // the user ID is the combination of the ueid and the local identifier SCBasic.user
                        updateWithResourceOrValue =  this.mdbUEID + "#" + SCBasic.user.getLocalName() + "_1";

                    } else if (updateWithResourceOrValue.equals(SprO.sproVARIABLEThisEntryVersionNumber.toString())) {

                        updateWithResourceOrValue =  this.currentFocus.substring((this.currentFocus.lastIndexOf("-") + 1));

                    }  else {

                        updateWithResourceOrValue = calculateValueForKeyword(updateWithResourceOrValue);

                    }

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())
                        || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectCopiedIndividualOf.toString())) {

                    object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, "a", connectionToTDB);

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.propertySOCCOMAS.toString())) {

                    property = calculateProperty(currExecStep);

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())
                        || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectCopiedIndividualOf.toString())) {

                    subject = calculateSubject(dataToFindSubjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                    directory = calculateWorkspaceDirectory(currExecStep);

                } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraphCopiedIndividualOf.toString())
                        || currExecStep.getJSONObject(i).get("property").toString().equals(SprO.loadFromSaveToUpdateInNamedGraph.toString())) {

                    ngIndividual = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                }

            }

            if (updateObjectInput) {

                JSONArray ngsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("ng");

                boolean copiedDidNotExist = true;

                // check if object already was generated in execution step 'copy and save triple statement(s)'
                for (int i = 0; i < ngsInJSONArray.length(); i++) {

                    if (ngsInJSONArray.get(i).toString().equals(ngIndividual)) {

                        if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString().equals(property)
                               && currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString().equals(subject)
                               && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                            currComponentObject.getJSONObject("input_data").getJSONArray("object_data").put(i, updateWithResourceOrValue);

                            System.out.println("currSubject = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString());
                            System.out.println("currProperty = " + currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString());
                            System.out.println("currNG = " + currComponentObject.getJSONObject("input_data").getJSONArray("ng").get(i).toString());
                            System.out.println("currDirectoryPath = " + currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString());
                            System.out.println("currObjectType = " + currComponentObject.getJSONObject("input_data").getJSONArray("object_type").get(i).toString());
                            System.out.println("currObject = " + currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString());
                            System.out.println("currOperation = " + currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(i).toString());

                            copiedDidNotExist = false;

                        }

                   }

                }

                if (copiedDidNotExist) {

                    currComponentObject.getJSONObject("input_data").getJSONArray("subject").put(subject);
                    currComponentObject.getJSONObject("input_data").getJSONArray("property").put(property);
                    currComponentObject.getJSONObject("input_data").getJSONArray("ng").put(ngIndividual);
                    currComponentObject.getJSONObject("input_data").getJSONArray("directory").put(directory);

                    String objectType = calculateObjectType(property);

                    objectType = calculateObjectTypeForAnnotationProperty(updateWithResourceOrValue, objectType);

                    currComponentObject.getJSONObject("input_data").getJSONArray("object_type").put(objectType);
                    currComponentObject.getJSONObject("input_data").getJSONArray("object_data").put(updateWithResourceOrValue);
                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put("s");

                    System.out.println("currSubject = " + subject);
                    System.out.println("currProperty = " + property);
                    System.out.println("currNG = " + ngIndividual);
                    System.out.println("currDirectoryPath = " + directory);
                    System.out.println("currObjectType = " + objectType);
                    System.out.println("currObject = " + updateWithResourceOrValue);
                    System.out.println("currOperation = " + "s");

                }

            } else if (updateSubjectInput) {

                JSONArray ngsInJSONArray = currComponentObject.getJSONObject("input_data").getJSONArray("ng");

                boolean copiedDidNotExist = true;

                // check if object already was generated in execution step 'copy and save triple statement(s)'
                for (int i = 0; i < ngsInJSONArray.length(); i++) {

                    if (ngsInJSONArray.get(i).toString().equals(ngIndividual)) {

                        if (currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString().equals(property)
                                && currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString().equals(object)
                                && currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString().equals(directory)) {

                            currComponentObject.getJSONObject("input_data").getJSONArray("subject").put(i, updateWithResourceOrValue);

                            System.out.println("currSubject = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(i).toString());
                            System.out.println("currProperty = " + currComponentObject.getJSONObject("input_data").getJSONArray("property").get(i).toString());
                            System.out.println("currNG = " + currComponentObject.getJSONObject("input_data").getJSONArray("ng").get(i).toString());
                            System.out.println("currDirectoryPath = " + currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(i).toString());
                            System.out.println("currObjectType = " + currComponentObject.getJSONObject("input_data").getJSONArray("object_type").get(i).toString());
                            System.out.println("currObject = " + currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(i).toString());
                            System.out.println("currOperation = " + currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(i).toString());

                            copiedDidNotExist = false;

                        }

                    }

                }

                if (copiedDidNotExist) {

                    currComponentObject.getJSONObject("input_data").getJSONArray("subject").put(subject);
                    currComponentObject.getJSONObject("input_data").getJSONArray("property").put(property);
                    currComponentObject.getJSONObject("input_data").getJSONArray("ng").put(ngIndividual);
                    currComponentObject.getJSONObject("input_data").getJSONArray("directory").put(directory);

                    String objectType = calculateObjectType(property);

                    currComponentObject.getJSONObject("input_data").getJSONArray("object_type").put(objectType);
                    currComponentObject.getJSONObject("input_data").getJSONArray("object_data").put(updateWithResourceOrValue);
                    currComponentObject.getJSONObject("input_data").getJSONArray("operation").put("s");

                    System.out.println("currSubject = " + subject);
                    System.out.println("currProperty = " + property);
                    System.out.println("currNG = " + ngIndividual);
                    System.out.println("currDirectoryPath = " + directory);
                    System.out.println("currObjectType = " + objectType);
                    System.out.println("currObject = " + updateWithResourceOrValue);
                    System.out.println("currOperation = " + "s");

                }

            }

        } else {

            boolean axiomStatement = false;
            boolean calculateNewResourceForInput = false;
            boolean calculateNewObjectInput = false;
            boolean calculateNewSubjectInput = false;

            String objectFromStore = "";

            for (int i = 0; i < currExecStep.length(); i++) {

                if (currExecStep.getJSONObject(i).get("object").toString().equals(SprO.sproVARIABLEToBeUpdated.toString())) {

                    String resultVar;

                    if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())) {

                        JSONObject dataToFindObjectInTDB = new JSONObject();

                        String subject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        String property = calculateProperty(currExecStep);

                        String currObjectType = calculateObjectType(property);

                        String ng = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        System.out.println("ng = " + ng);

                        String directory = calculateWorkspaceDirectory(currExecStep);

                        resultVar = "?o";

                        SelectBuilder selectBuilder = new SelectBuilder();

                        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                        SelectBuilder tripleSPO = new SelectBuilder();

                        tripleSPO.addWhere("<" + subject + ">", "<" + property + ">", "?o");

                        selectBuilder.addVar(selectBuilder.makeVar(resultVar));

                        selectBuilder.addGraph("<" + ng + ">", tripleSPO);

                        String sparqlQueryString = selectBuilder.buildString();

                        String object = connectionToTDB.pullSingleDataFromTDB(directory, sparqlQueryString, resultVar);

                        if (!object.equals("")) {

                            updateStatement.put("subject", subject);

                            updateStatement.put("property", property);

                            updateStatement.put("ng", ng);

                            updateStatement.put("directory", directory);

                            currObjectType = calculateObjectTypeForAnnotationProperty(object, currObjectType);

                            updateStatement.put("object_type", currObjectType);

                            if (currObjectType.equals("l")) {
                                // delete initial object from the jena tdb e.g. "true"^^http://www.w3.org/2001/XMLSchema#boolean

                                String literalDatatypeResultVar = "?o";

                                SelectBuilder literalDatatypeSelectBuilder = new SelectBuilder();

                                PrefixesBuilder literalDatatypePrefixesBuilder = new PrefixesBuilder();

                                literalDatatypeSelectBuilder = literalDatatypePrefixesBuilder.addPrefixes(literalDatatypeSelectBuilder);

                                SelectBuilder literalDatatypeTripleSPO = new SelectBuilder();

                                literalDatatypeTripleSPO.addWhere("<" + subject + ">", "<" + property + ">", "?o");

                                literalDatatypeSelectBuilder.addVar(literalDatatypeSelectBuilder.makeVar(literalDatatypeResultVar));

                                literalDatatypeSelectBuilder.addGraph("<" + ng + ">", literalDatatypeTripleSPO);

                                String literalDatatypeSparqlQueryString = literalDatatypeSelectBuilder.buildString();

                                objectFromStore = connectionToTDB.pullSingleLiteralWithDatatypeFromTDB(directory, literalDatatypeSparqlQueryString, literalDatatypeResultVar);

                                updateStatement.put("object_data", objectFromStore);

                            } else {

                                updateStatement.put("object_data", object);

                            }

                            updateStatement.put("operation", "d");

                            calculateNewResourceForInput = true;

                            calculateNewObjectInput = true;

                        } else {

                            PrefixesBuilder prefixesAxiomBuilder = new PrefixesBuilder();

                            ConstructBuilder constructAxiomBuilder = new ConstructBuilder();

                            constructAxiomBuilder = prefixesAxiomBuilder.addPrefixes(constructAxiomBuilder);

                            constructAxiomBuilder.addConstruct("?s", "?p", "?o");

                            SelectBuilder tripleAxiomSPOConstruct = new SelectBuilder();

                            tripleAxiomSPOConstruct.addWhere("?s", "?p", "?o");
                            tripleAxiomSPOConstruct.addWhere("?s", OWL2.annotatedSource, "<" + subject + ">");

                            constructAxiomBuilder.addGraph("<" + ng + ">", tripleAxiomSPOConstruct);

                            sparqlQueryString = constructAxiomBuilder.buildString();

                            Model individualAxiomModel = connectionToTDB.pullDataFromTDB(directory, sparqlQueryString);

                            StmtIterator stmtIterator = individualAxiomModel.listStatements();

                            while (stmtIterator.hasNext()) {

                                Statement currStatement = stmtIterator.next();

                                if (currStatement.getSubject().isAnon()) {

                                    if (currStatement.getObject().isResource()) {

                                        object = currStatement.getObject().asResource().toString();

                                        currObjectType = "r";

                                    } else if (currStatement.getObject().isLiteral()) {

                                        object = currStatement.getObject().asLiteral().getLexicalForm();

                                        currObjectType = "l";

                                    }

                                    // old statements
                                    updateAxiomStatement.append("subject", currStatement.getSubject().toString());
                                    updateAxiomStatement.append("property", currStatement.getPredicate().toString());
                                    updateAxiomStatement.append("ng", ng);
                                    updateAxiomStatement.append("directory", directory);

                                    if (currObjectType.equals("l")) {
                                        // delete initial object from the jena tdb e.g. "true"^^http://www.w3.org/2001/XMLSchema#boolean

                                        String literalDatatypeResultVar = "?o";

                                        SelectBuilder literalDatatypeSelectBuilder = new SelectBuilder();

                                        PrefixesBuilder literalDatatypePrefixesBuilder = new PrefixesBuilder();

                                        literalDatatypeSelectBuilder = literalDatatypePrefixesBuilder.addPrefixes(literalDatatypeSelectBuilder);

                                        SelectBuilder literalDatatypeTripleSPO = new SelectBuilder();

                                        literalDatatypeTripleSPO.addWhere("<" + subject + ">", "<" + property + ">", "?o");

                                        literalDatatypeSelectBuilder.addVar(literalDatatypeSelectBuilder.makeVar(literalDatatypeResultVar));

                                        literalDatatypeSelectBuilder.addGraph("<" + ng + ">", literalDatatypeTripleSPO);

                                        String literalDatatypeSparqlQueryString = literalDatatypeSelectBuilder.buildString();

                                        updateAxiomStatement.append("object_data", connectionToTDB.pullSingleLiteralWithDatatypeFromTDB(directory, literalDatatypeSparqlQueryString, literalDatatypeResultVar));

                                    } else {

                                        updateAxiomStatement.append("object_data", object);

                                    }

                                    updateAxiomStatement.append("object_type", currObjectType);
                                    updateAxiomStatement.append("operation", "d");

                                    for (int j = 0; j < currExecStep.length(); j++) {

                                        if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.propertySOCCOMAS.toString())) {

                                            if (currStatement.getPredicate().toString().equals(currExecStep.getJSONObject(j).get("object").toString())) {

                                                for (int k = 0; k < currExecStep.length(); k++) {

                                                    if (currExecStep.getJSONObject(k).get("property").toString().equals(SprO.updateWithResourceValue.toString())) {

                                                        currObjectType = calculateObjectTypeForAnnotationProperty(currExecStep.getJSONObject(k).get("object").toString(), currObjectType);

                                                        currExecStep.getJSONObject(i).put("object", currExecStep.getJSONObject(k).get("object").toString());

                                                    }

                                                }

                                                dataToFindObjectInTDB = new JSONObject();
                                                dataToFindObjectInTDB.put("subject", currStatement.getSubject().toString());
                                                dataToFindObjectInTDB.put("property", currStatement.getPredicate().toString());
                                                dataToFindObjectInTDB.put("ng", ng);
                                                dataToFindObjectInTDB.put("directory", directory);

                                                object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, currObjectType, connectionToTDB);

                                                currObjectType = calculateObjectTypeForAnnotationProperty(object, currObjectType);

                                            }

                                        }

                                    }

                                    String newSubjectName;

                                    // create new blank node and allocate the corresponding old bNode with the new bNode
                                    if (this.bNodeIdentifier.has(currStatement.getSubject().toString())) {

                                        newSubjectName = this.bNodeIdentifier.get(currStatement.getSubject().toString()).toString();

                                    } else {

                                        newSubjectName = ResourceFactory.createResource().toString();

                                        this.bNodeIdentifier.put(currStatement.getSubject().toString(), newSubjectName);

                                    }

                                    // new statements
                                    updateAxiomStatement.append("subject", newSubjectName);
                                    updateAxiomStatement.append("property", currStatement.getPredicate().toString());
                                    updateAxiomStatement.append("ng", ng);
                                    updateAxiomStatement.append("directory", directory);
                                    updateAxiomStatement.append("object_data", object);
                                    updateAxiomStatement.append("object_type", currObjectType);
                                    updateAxiomStatement.append("operation", "s");

                                    axiomStatement = true;

                                }

                                System.out.println();
                                System.out.println("updateStatement a = " + updateStatement);
                                System.out.println();

                            }

                            if (!axiomStatement) {

                                for (int j = 0; j < currExecStep.length(); j++) {

                                    if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.updateWithResourceValue.toString())) {

                                        // count value in jena tdb
                                        SelectBuilder selectWhereBuilder = new SelectBuilder();

                                        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                                        selectWhereBuilder.addWhere("<" + subject + ">", "<" + property + ">", "?o");

                                        SelectBuilder countSelectBuilder = new SelectBuilder();

                                        countSelectBuilder = prefixesBuilder.addPrefixes(countSelectBuilder);

                                        ExprVar exprVar = new ExprVar("o");

                                        Aggregator aggregator = AggregatorFactory.createCountExpr(true, exprVar.getExpr());

                                        ExprAggregator exprAggregator = new ExprAggregator(exprVar.asVar(), aggregator);

                                        countSelectBuilder.addVar(exprAggregator.getExpr(), "?count");

                                        countSelectBuilder.addGraph("<" + ng + ">", selectWhereBuilder);

                                        sparqlQueryString = countSelectBuilder.buildString();

                                        int count = Integer.parseInt(connectionToTDB.pullSingleDataFromTDB(directory, sparqlQueryString, "?count"));

                                        if (count <= 0 || !object.equals("")) {
                                            // no data exist in store

                                            currExecStep.getJSONObject(i).put("object", currExecStep.getJSONObject(j).get("object").toString());

                                            dataToFindObjectInTDB = new JSONObject();
                                            dataToFindObjectInTDB.put("subject", subject);
                                            dataToFindObjectInTDB.put("property", property);
                                            dataToFindObjectInTDB.put("ng", ng);
                                            dataToFindObjectInTDB.put("directory", directory);

                                            object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, currObjectType, connectionToTDB);

                                            currObjectType = calculateObjectTypeForAnnotationProperty(object, currObjectType);

                                        }

                                        updateStatement.put("subject", subject);

                                        updateStatement.put("property", property);

                                        updateStatement.put("ng", ng);

                                        updateStatement.put("directory", directory);

                                        updateStatement.put("object_type", currObjectType);

                                        if (currObjectType.equals("l")) {
                                            // delete initial object from the jena tdb e.g. "true"^^http://www.w3.org/2001/XMLSchema#boolean

                                            String literalDatatypeResultVar = "?o";

                                            SelectBuilder literalDatatypeSelectBuilder = new SelectBuilder();

                                            PrefixesBuilder literalDatatypePrefixesBuilder = new PrefixesBuilder();

                                            literalDatatypeSelectBuilder = literalDatatypePrefixesBuilder.addPrefixes(literalDatatypeSelectBuilder);

                                            SelectBuilder literalDatatypeTripleSPO = new SelectBuilder();

                                            literalDatatypeTripleSPO.addWhere("<" + subject + ">", "<" + property + ">", "?o");

                                            literalDatatypeSelectBuilder.addVar(literalDatatypeSelectBuilder.makeVar(literalDatatypeResultVar));

                                            literalDatatypeSelectBuilder.addGraph("<" + ng + ">", literalDatatypeTripleSPO);

                                            String literalDatatypeSparqlQueryString = literalDatatypeSelectBuilder.buildString();

                                            updateStatement.put("object_data", connectionToTDB.pullSingleLiteralWithDatatypeFromTDB(directory, literalDatatypeSparqlQueryString, literalDatatypeResultVar));

                                        } else {

                                            updateStatement.put("object_data", object);

                                        }

                                        updateStatement.put("operation", "d");

                                        calculateNewResourceForInput = true;

                                        calculateNewObjectInput = true;

                                    }

                                }

                            }

                        }

                        System.out.println();
                        System.out.println("updateStatement b = " + updateStatement);
                        System.out.println();

                    } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())) {

                        String property = calculateProperty(currExecStep);

                        String ng = calculateNG(currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                        String directory = calculateWorkspaceDirectory(currExecStep);

                        String currObjectType = calculateObjectType(property);

                        JSONObject dataToFindObjectInTDB = new JSONObject();
                        dataToFindObjectInTDB.put("subject", "?s");
                        dataToFindObjectInTDB.put("property", property);
                        dataToFindObjectInTDB.put("ng", ng);
                        dataToFindObjectInTDB.put("directory", directory);

                        String object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, currObjectType, connectionToTDB);

                        resultVar = "?s";

                        SelectBuilder selectBuilder = new SelectBuilder();

                        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                        SelectBuilder tripleSPO = new SelectBuilder();

                        tripleSPO.addWhere("?s", "<" + property + ">", "<" + object + ">");

                        selectBuilder.addVar(selectBuilder.makeVar(resultVar));

                        selectBuilder.addGraph("<" + ng + ">", tripleSPO);

                        String sparqlQueryString = selectBuilder.buildString();

                        String subject = connectionToTDB.pullSingleDataFromTDB(directory, sparqlQueryString, resultVar);

                        if (!subject.equals("")) {

                            updateStatement.put("subject", subject);

                            updateStatement.put("property", property);

                            updateStatement.put("ng", ng);

                            updateStatement.put("directory", directory);

                            currObjectType = calculateObjectTypeForAnnotationProperty(object, currObjectType);

                            updateStatement.put("object_type", currObjectType);

                            if (currObjectType.equals("l")) {
                                // delete initial object from the jena tdb e.g. "true"^^http://www.w3.org/2001/XMLSchema#boolean

                                String literalDatatypeResultVar = "?o";

                                SelectBuilder literalDatatypeSelectBuilder = new SelectBuilder();

                                PrefixesBuilder literalDatatypePrefixesBuilder = new PrefixesBuilder();

                                literalDatatypeSelectBuilder = literalDatatypePrefixesBuilder.addPrefixes(literalDatatypeSelectBuilder);

                                SelectBuilder literalDatatypeTripleSPO = new SelectBuilder();

                                literalDatatypeTripleSPO.addWhere("<" + subject + ">", "<" + property + ">", "?o");

                                literalDatatypeSelectBuilder.addVar(literalDatatypeSelectBuilder.makeVar(literalDatatypeResultVar));

                                literalDatatypeSelectBuilder.addGraph("<" + ng + ">", literalDatatypeTripleSPO);

                                String literalDatatypeSparqlQueryString = literalDatatypeSelectBuilder.buildString();

                                updateStatement.put("object_data", connectionToTDB.pullSingleLiteralWithDatatypeFromTDB(directory, literalDatatypeSparqlQueryString, literalDatatypeResultVar));

                            } else {

                                updateStatement.put("object_data", object);

                            }

                            updateStatement.put("operation", "d");

                            calculateNewResourceForInput = true;

                            calculateNewSubjectInput = true;

                        } else {

                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.updateWithResourceValue.toString())) {

                                    currExecStep.getJSONObject(i).put("object", currExecStep.getJSONObject(j).get("object").toString());

                                    dataToFindObjectInTDB = new JSONObject();
                                    dataToFindObjectInTDB.put("object", object);
                                    dataToFindObjectInTDB.put("property", property);
                                    dataToFindObjectInTDB.put("ng", ng);
                                    dataToFindObjectInTDB.put("directory", directory);

                                    subject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                                    currObjectType = calculateObjectTypeForAnnotationProperty(object, currObjectType);

                                    updateStatement.put("subject", subject);

                                    updateStatement.put("property", property);

                                    updateStatement.put("ng", ng);

                                    updateStatement.put("directory", directory);

                                    updateStatement.put("object_type", currObjectType);

                                    if (currObjectType.equals("l")) {
                                        // delete initial object from the jena tdb e.g. "true"^^http://www.w3.org/2001/XMLSchema#boolean

                                        String literalDatatypeResultVar = "?o";

                                        SelectBuilder literalDatatypeSelectBuilder = new SelectBuilder();

                                        PrefixesBuilder literalDatatypePrefixesBuilder = new PrefixesBuilder();

                                        literalDatatypeSelectBuilder = literalDatatypePrefixesBuilder.addPrefixes(literalDatatypeSelectBuilder);

                                        SelectBuilder literalDatatypeTripleSPO = new SelectBuilder();

                                        literalDatatypeTripleSPO.addWhere("<" + subject + ">", "<" + property + ">", "?o");

                                        literalDatatypeSelectBuilder.addVar(literalDatatypeSelectBuilder.makeVar(literalDatatypeResultVar));

                                        literalDatatypeSelectBuilder.addGraph("<" + ng + ">", literalDatatypeTripleSPO);

                                        String literalDatatypeSparqlQueryString = literalDatatypeSelectBuilder.buildString();

                                        updateStatement.put("object_data", connectionToTDB.pullSingleLiteralWithDatatypeFromTDB(directory, literalDatatypeSparqlQueryString, literalDatatypeResultVar));

                                    } else {

                                        updateStatement.put("object_data", object);

                                    }

                                    updateStatement.put("operation", "d");

                                }

                            }

                        }

                        System.out.println();
                        System.out.println("updateStatement c = " + updateStatement);
                        System.out.println();

                    }

                }

            }

            if (axiomStatement) {

                JSONArray jsonArrayForTheAxiomInput = updateAxiomStatement.getJSONArray("subject");

                for (int i = 0; i < jsonArrayForTheAxiomInput.length(); i++) {

                    currComponentObject.getJSONObject("input_data").append("subject", updateAxiomStatement.getJSONArray("subject").get(i).toString());
                    currComponentObject.getJSONObject("input_data").append("property", updateAxiomStatement.getJSONArray("property").get(i).toString());
                    currComponentObject.getJSONObject("input_data").append("ng", updateAxiomStatement.getJSONArray("ng").get(i).toString());
                    currComponentObject.getJSONObject("input_data").append("directory", updateAxiomStatement.getJSONArray("directory").get(i).toString());
                    currComponentObject.getJSONObject("input_data").append("object_data", updateAxiomStatement.getJSONArray("object_data").get(i).toString());
                    currComponentObject.getJSONObject("input_data").append("object_type", updateAxiomStatement.getJSONArray("object_type").get(i).toString());
                    currComponentObject.getJSONObject("input_data").append("operation", updateAxiomStatement.getJSONArray("operation").get(i).toString());

                }

            } else if (updateStatement.keys().hasNext()) {

                Iterator stmtIter = updateStatement.keys();

                while (stmtIter.hasNext()) {

                    String currKey = stmtIter.next().toString();

                    if (!currComponentObject.has("input_data")) {

                        currComponentObject.put("input_data", new JSONObject());

                    }

                    currComponentObject.getJSONObject("input_data").append(currKey, updateStatement.get(currKey));

                }

                if (calculateNewResourceForInput) {

                    for (int i = 0; i < currExecStep.length(); i++) {

                        if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.objectSOCCOMAS.toString())
                                && calculateNewObjectInput) {

                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.updateWithResourceValue.toString())) {

                                    String object;

                                    if (currExecStep.getJSONObject(j).get("object").toString().equals(SprO.sproVARIABLEPosition1Add1.toString())
                                            || currExecStep.getJSONObject(j).get("object").toString().equals(SprO.sproVARIABLE1Plus1.toString())) {

                                        object = String.valueOf(Integer.parseInt(objectFromStore) + 1);

                                        updateStatement.put("object_data", object);

                                    } else if (currExecStep.getJSONObject(j).get("object").toString().equals(SprO.sproVARIABLEPosition1Subtract1.toString())) {

                                        if (highThenExist) {

                                            if (Integer.parseInt(objectFromStore) > higherThenValue) {

                                                object = String.valueOf(Integer.parseInt(objectFromStore) - 1);

                                            } else {
                                                // this means do not update >>> remove last element from JSONObject

                                                System.out.println("INFO: Update was aborted!");

                                                int positionLastElement = (currComponentObject.getJSONObject("input_data").getJSONArray("subject").length()) - 1;

                                                System.out.println("remove subject = " + currComponentObject.getJSONObject("input_data").getJSONArray("subject").get(positionLastElement).toString());
                                                System.out.println("remove property = " + currComponentObject.getJSONObject("input_data").getJSONArray("property").get(positionLastElement).toString());
                                                System.out.println("remove ng = " + currComponentObject.getJSONObject("input_data").getJSONArray("ng").get(positionLastElement).toString());
                                                System.out.println("remove directory = " + currComponentObject.getJSONObject("input_data").getJSONArray("directory").get(positionLastElement).toString());
                                                System.out.println("remove object_data = " + currComponentObject.getJSONObject("input_data").getJSONArray("object_data").get(positionLastElement).toString());
                                                System.out.println("remove object_type = " + currComponentObject.getJSONObject("input_data").getJSONArray("object_type").get(positionLastElement).toString());
                                                System.out.println("remove operation = " + currComponentObject.getJSONObject("input_data").getJSONArray("operation").get(positionLastElement).toString());

                                                currComponentObject.getJSONObject("input_data").getJSONArray("subject").remove(positionLastElement);
                                                currComponentObject.getJSONObject("input_data").getJSONArray("property").remove(positionLastElement);
                                                currComponentObject.getJSONObject("input_data").getJSONArray("ng").remove(positionLastElement);
                                                currComponentObject.getJSONObject("input_data").getJSONArray("directory").remove(positionLastElement);
                                                currComponentObject.getJSONObject("input_data").getJSONArray("object_data").remove(positionLastElement);
                                                currComponentObject.getJSONObject("input_data").getJSONArray("object_type").remove(positionLastElement);
                                                currComponentObject.getJSONObject("input_data").getJSONArray("operation").remove(positionLastElement);

                                                return currComponentObject;

                                            }


                                        } else {

                                            object = String.valueOf(Integer.parseInt(objectFromStore) - 1);

                                        }

                                        updateStatement.put("object_data", object);

                                    } else {

                                        currExecStep.getJSONObject(i).put("object", currExecStep.getJSONObject(j).get("object").toString());

                                        JSONObject dataToFindObjectInTDB = new JSONObject();

                                        object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, updateStatement.get("object_type").toString(), connectionToTDB);

                                        updateStatement.put("object_data", object);

                                    }

                                    String objectType = calculateObjectType(updateStatement.get("property").toString());

                                    objectType = calculateObjectTypeForAnnotationProperty(object, objectType);

                                    updateStatement.put("object_type", objectType);

                                } else if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.updateWithThisEntrySSpecificIndividualOf.toString())) {

                                    // update triple statement for the object
                                    currExecStep.getJSONObject(i).put("property", SprO.objectThisEntrySSpecificIndividualOf.toString());

                                    currExecStep.getJSONObject(i).put("object", currExecStep.getJSONObject(j).get("object").toString());

                                    JSONObject dataToFindObjectInTDB = new JSONObject();

                                    String object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, updateStatement.get("object_type").toString(), connectionToTDB);

                                    updateStatement.put("object_data", object);

                                    String objectType = calculateObjectType(updateStatement.get("property").toString());

                                    objectType = calculateObjectTypeForAnnotationProperty(object, objectType);

                                    updateStatement.put("object_type", objectType);

                                } else if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.updateWithUniqueIndividualOf.toString())) {

                                    // update triple statement for the object
                                    String newPropertyURI = SprO.objectUniqueIndividualOf.toString();

                                    String newObjectURI = currExecStep.getJSONObject(j).get("object").toString();

                                    for (int k = 0; k < currExecStep.length(); k++) {

                                        if (currExecStep.getJSONObject(k).get("property").toString().equals(SprO.objectUniqueIndividualOf.toString())) {

                                            currExecStep.getJSONObject(k).put("object", newObjectURI);

                                        }

                                    }

                                    currExecStep.getJSONObject(i).put("property", newPropertyURI);

                                    currExecStep.getJSONObject(i).put("object", newObjectURI);

                                    JSONObject dataToFindObjectInTDB = new JSONObject();

                                    String object = calculateObject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, updateStatement.get("object_type").toString(), connectionToTDB);

                                    updateStatement.put("object_data", object);

                                    String objectType = calculateObjectType(updateStatement.get("property").toString());

                                    objectType = calculateObjectTypeForAnnotationProperty(object, objectType);

                                    updateStatement.put("object_type", objectType);

                                }

                            }

                        } else if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.subjectSOCCOMAS.toString())
                                && calculateNewSubjectInput) {

                            for (int j = 0; j < currExecStep.length(); j++) {

                                if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.updateWithResourceValue.toString())) {

                                    currExecStep.getJSONObject(i).put("object", currExecStep.getJSONObject(j).get("object").toString());

                                    JSONObject dataToFindObjectInTDB = new JSONObject();

                                    String subject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                                    updateStatement.put("subject", subject);

                                } else if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.updateWithThisEntrySSpecificIndividualOf.toString())) {

                                    // update triple statement for the subject
                                    currExecStep.getJSONObject(i).put("property", SprO.subjectThisEntrySSpecificIndividualOf.toString());

                                    currExecStep.getJSONObject(i).put("object", currExecStep.getJSONObject(j).get("object").toString());

                                    JSONObject dataToFindObjectInTDB = new JSONObject();

                                    String subject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                                    updateStatement.put("subject", subject);

                                } else if (currExecStep.getJSONObject(j).get("property").toString().equals(SprO.updateWithUniqueIndividualOf.toString())) {

                                    String newPropertyURI = SprO.subjectUniqueIndividualOf.toString();

                                    String newObjectURI = currExecStep.getJSONObject(j).get("object").toString();

                                    for (int k = 0; k < currExecStep.length(); k++) {

                                        if (currExecStep.getJSONObject(k).get("property").toString().equals(SprO.subjectUniqueIndividualOf.toString())) {

                                            currExecStep.getJSONObject(k).put("object", newObjectURI);

                                        }

                                    }

                                    currExecStep.getJSONObject(i).put("property", newPropertyURI);

                                    currExecStep.getJSONObject(i).put("object", newObjectURI);

                                    JSONObject dataToFindObjectInTDB = new JSONObject();

                                    String subject = calculateSubject(dataToFindObjectInTDB, currExecStep, currComponentObject, jsonInputObject, connectionToTDB);

                                    updateStatement.put("subject", subject);

                                }

                            }

                        }

                    }

                }

                updateStatement.put("operation", "s");

                System.out.println();
                System.out.println("updateStatement d = " + updateStatement);
                System.out.println();

                if (updateStatement.get("property").toString().equals(SprO.hiddenBOOLEAN.toString())
                        && updateStatement.get("object_data").toString().equals("false")) {

                    this.updateComposition = true;

                    if (this.compositionUpdateJSON.has("children")
                            && this.compositionUpdateJSON.has("ngs")
                            && this.compositionUpdateJSON.has("directories")) {

                        JSONArray childrenJSON = this.compositionUpdateJSON.getJSONArray("children");
                        JSONArray ngsJSON = this.compositionUpdateJSON.getJSONArray("ngs");
                        JSONArray directoriesJSON = this.compositionUpdateJSON.getJSONArray("directories");

                        childrenJSON.put(updateStatement.get("subject").toString());
                        ngsJSON.put(updateStatement.get("ng").toString());
                        directoriesJSON.put(updateStatement.get("directory").toString());

                        this.compositionUpdateJSON.put("children", childrenJSON);
                        this.compositionUpdateJSON.put("ngs", ngsJSON);
                        this.compositionUpdateJSON.put("directories", directoriesJSON);

                    } else {

                        JSONArray childrenJSON = new JSONArray();
                        JSONArray ngsJSON = new JSONArray();
                        JSONArray directoriesJSON = new JSONArray();

                        childrenJSON.put(updateStatement.get("subject").toString());
                        ngsJSON.put(updateStatement.get("ng").toString());
                        directoriesJSON.put(updateStatement.get("directory").toString());

                        this.compositionUpdateJSON.put("children", childrenJSON);
                        this.compositionUpdateJSON.put("ngs", ngsJSON);
                        this.compositionUpdateJSON.put("directories", directoriesJSON);

                    }

                } else if (updateStatement.get("property").toString().equals(SprO.hiddenBOOLEAN.toString())
                        && updateStatement.get("object_data").toString().equals("true")) {

                    if (currComponentObject.has("delete_uri")) {

                        currComponentObject.getJSONArray("delete_uri").put(ResourceFactory.createResource(updateStatement.get("subject").toString()).getLocalName());

                    } else {

                        JSONArray updateURIsJSON = new JSONArray();

                        updateURIsJSON.put(ResourceFactory.createResource(updateStatement.get("subject").toString()).getLocalName());

                        currComponentObject.put("delete_uri", updateURIsJSON);

                    }

                }

                stmtIter = updateStatement.keys();

                while (stmtIter.hasNext()) {

                    String currKey = stmtIter.next().toString();

                    currComponentObject.getJSONObject("input_data").append(currKey, updateStatement.get(currKey));

                }

            }

        }

        return currComponentObject;

    }


    /**
     * This method saves the data to the jena tdb and the mongoDB.
     * @param currComponentObject contains the current component information for the output json
     * @param jsonInputObject contains the information for the calculation
     */
    private void saveToStores(JSONObject currComponentObject, JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        JSONObject inputData = currComponentObject.getJSONObject("input_data");

        System.out.println();
        System.out.println("saveToStore");
        System.out.println();

        JSONArray deleteNamedGraphs = new JSONArray();

        if (inputData.has("deleteNamedGraphs")) {
            // remove deleteNamedGraphs from inputData for other calculation

            deleteNamedGraphs = inputData.getJSONArray("deleteNamedGraphs");

            inputData.remove("deleteNamedGraphs");

        }

        DataFactory dataFactory = new DataFactory();

        ArrayList<ArrayList<String>> generatedCoreIDData = dataFactory.generateCoreIDNGData(inputData);

        inputData = convertArrayListToJSONObject(generatedCoreIDData);

        JSONInputInterpreter jsonInputInterpreter = new JSONInputInterpreter();

        if (deleteNamedGraphs.length() > 0) {
            // put deleteNamedGraphs back to inputData

            inputData.put("deleteNamedGraphs", deleteNamedGraphs);

        }

        ArrayList<String> dummyArrayList = jsonInputInterpreter.interpretObject(inputData, connectionToTDB);

        for (String aDummyArrayList : dummyArrayList) {

            System.out.println("jsonInputInterpreter: " + aDummyArrayList);

        }

        currComponentObject.remove("input_data");

        if ((jsonInputObject.has("localID")) & (jsonInputObject.has("mdbueid"))) {

            String updateMongoDBKey = jsonInputObject.get("localID").toString();

            if ((updateMongoDBKey.equals(SCBasic.signUPMODULEITEMSignUpButton.getLocalName()))
                    && ((!(jsonInputObject.get("mdbueid").toString()).equals("")))) {

                System.out.println("Update mongoDB with " + this.mdbUEID + " where mdbueid is = " + jsonInputObject.get("mdbueid").toString());

                if (this.mongoDBConnection.documentExist("mdb-prototyp", "users", "mdbueid", jsonInputObject.get("mdbueid").toString())) {
                    // insert mdbueiduri in "users" collection in mongoDB

                    String objectID = this.mongoDBConnection.findObjectID("mdb-prototyp", "users", "mdbueid", jsonInputObject.get("mdbueid").toString());

                    this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "users", objectID, "mdbueiduri", this.mdbUEID);

                    currComponentObject.put("mdbueid_uri", this.mdbUEID);

                }

                if (this.mongoDBConnection.documentExist("mdb-prototyp", "sessions", "session", jsonInputObject.get("connectSID").toString())) {
                    // insert mdbueid + mdbueiduri in "sessions" collection in mongoDB

                    String objectID = this.mongoDBConnection.findObjectID("mdb-prototyp", "sessions", "session", jsonInputObject.get("connectSID").toString());

                    this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "sessions", objectID, "mdbueid", jsonInputObject.get("mdbueid").toString());

                    this.mongoDBConnection.insertDataToMongoDB("mdb-prototyp", "sessions", objectID, "mdbueiduri", this.mdbUEID);

                }

            }

        }

        if (this.updateComposition
                && this.compositionUpdateJSON.has("children")
                && this.compositionUpdateJSON.has("directories")
                && this.compositionUpdateJSON.has("ngs")) {

            JSONArray childrenJSON = this.compositionUpdateJSON.getJSONArray("children");

            for (int i = 0; i < childrenJSON.length(); i++) {

                String parent = calculateMDBParent(i, connectionToTDB);

                JSONArray parentToCheck = new JSONArray();

                parentToCheck.put(parent);

                System.out.println();
                System.out.println("child = " + childrenJSON.get(i).toString());
                System.out.println("parent = " + parent);

                Model subCompositionCopyModel = ModelFactory.createDefaultModel(),
                        subCompositionUpdateModel = ModelFactory.createDefaultModel();

                subCompositionCopyModel =
                        subCompositionCopyModel
                                .union(
                                        connectionToTDB
                                                .pullNamedModelFromTDB(
                                                        this.compositionUpdateJSON
                                                                .getJSONArray("directories").get(i).toString(),
                                                        this.compositionUpdateJSON.getJSONArray("ngs").get(i).toString()));

                while (!parentToCheck.isNull(0)) {

                    if (subCompositionCopyModel.contains(ResourceFactory.createResource(parentToCheck.get(0).toString()), RDF.type, OWL2.NamedIndividual)) {

                        Selector parentTripleSelector = new SimpleSelector(ResourceFactory.createResource(parentToCheck.get(0).toString()), null, null, "");

                        StmtIterator parentStmts = subCompositionCopyModel.listStatements(parentTripleSelector);

                        while (parentStmts.hasNext()) {

                            Statement parentStmt = parentStmts.nextStatement();

                            subCompositionUpdateModel.add(parentStmt);

                            if (parentStmt.getSubject().toString().equals(parentToCheck.get(0).toString())
                                    && parentStmt.getPredicate().equals(SprO.hasEntryComponent)) {

                                parentToCheck.put(parentStmt.getObject().toString());

                            } else if (parentStmt.getObject().isURIResource()
                                    && parentStmt.getPredicate().equals(RDF.type)) {

                                Resource currSubject = parentStmt.getSubject().asResource();

                                Property currProperty = parentStmt.getPredicate();

                                Resource currObject = parentStmt.getObject().asResource();

                                if (currSubject.equals(ResourceFactory.createResource(parentToCheck.get(0).toString()))
                                        && currProperty.equals(RDF.type)
                                        && !currObject.equals(OWL2.NamedIndividual)) {

                                    Selector classSelector = new SimpleSelector(currObject, null, null, "");

                                    StmtIterator classStmts = subCompositionCopyModel.listStatements(classSelector);

                                    Resource classSubject = null;

                                    while (classStmts.hasNext()) {

                                        Statement classStmt = classStmts.nextStatement();

                                        classSubject = classStmt.getSubject();

                                        if ((!classStmt.getObject().equals(OWL2.Class))
                                                && (!classStmt.getPredicate().equals(RDFS.label))
                                                && (!classStmt.getPredicate().equals(RDFS.subClassOf))
                                                && (!classStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                && (!classStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                            subCompositionUpdateModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(parentToCheck.get(0).toString()), classStmt.getPredicate(), classStmt.getObject()));

                                        }

                                    }

                                    if (subCompositionCopyModel.contains(null, OWL2.annotatedSource, classSubject)) {

                                        ResIterator axiomsForClassSubject = subCompositionCopyModel.listSubjectsWithProperty(OWL2.annotatedSource, classSubject);

                                        while (axiomsForClassSubject.hasNext()) {

                                            Resource axiomClassSubject = axiomsForClassSubject.next();

                                            Selector axiomClassSelector = new SimpleSelector(axiomClassSubject, null, null, "");

                                            StmtIterator axiomClassStmts = subCompositionCopyModel.listStatements(axiomClassSelector);

                                            while (axiomClassStmts.hasNext()) {

                                                Statement axiomClassStmt = axiomClassStmts.nextStatement();

                                                if ((!axiomClassStmt.getObject().equals(OWL2.Axiom))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedSource))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedTarget))
                                                        && (!axiomClassStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                                    subCompositionUpdateModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(parentToCheck.get(0).toString()), axiomClassStmt.getPredicate(), axiomClassStmt.getObject()));

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                        if (subCompositionCopyModel.contains(null, OWL2.annotatedSource, ResourceFactory.createResource(parentToCheck.get(0).toString()))) {

                            ResIterator axiomsForSubject = subCompositionCopyModel.listSubjectsWithProperty(OWL2.annotatedSource, ResourceFactory.createResource(parentToCheck.get(0).toString()));

                            while (axiomsForSubject.hasNext()) {

                                Resource axiomSubject = axiomsForSubject.next();

                                Selector axiomSelector = new SimpleSelector(axiomSubject, null, null, "");

                                StmtIterator axiomStmts = subCompositionCopyModel.listStatements(axiomSelector);

                                while (axiomStmts.hasNext()) {

                                    Statement axiomStmt = axiomStmts.nextStatement();

                                    if ((!axiomStmt.getObject().equals(OWL2.Axiom))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedSource))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedTarget))
                                            && (!axiomStmt.getPredicate().equals(OWL2.annotatedProperty))) {

                                        subCompositionUpdateModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(parentToCheck.get(0).toString()), axiomStmt.getPredicate(), axiomStmt.getObject()));

                                    }

                                }

                            }

                        }

                    }

                    // remove the old key
                    parentToCheck.remove(0);

                }

                StmtIterator entryComponentsModelIter = subCompositionUpdateModel.listStatements();

                OutputGenerator outputGenerator = new OutputGenerator(this.mongoDBConnection);

                JSONObject entryComponents = new JSONObject();

                while (entryComponentsModelIter.hasNext()) {

                    Statement resStmt = entryComponentsModelIter.nextStatement();

                    entryComponents = outputGenerator
                            .manageProperty(resStmt.getSubject().toString(), resStmt, entryComponents,
                                    jsonInputObject, connectionToTDB);

                }

                entryComponents = outputGenerator.reorderEntryComponentsValues(entryComponents);

                Iterator<String> iter = entryComponents.keys();

                JSONArray outputDataJSON = new JSONArray();

                while (iter.hasNext()) {

                    String currKey = iter.next();

                    JSONObject wrapperJSON = new JSONObject();

                    wrapperJSON.put(currKey, entryComponents.getJSONObject(currKey));

                    outputDataJSON.put(wrapperJSON);

                }

                outputDataJSON = outputGenerator.orderSubCompositionOutputJSON(parent, outputDataJSON);

                // update mongo composition for html form
                outputGenerator.getOutputJSONObject(jsonInputObject.get("html_form").toString(), jsonInputObject, outputDataJSON);

                String parentLocalID = ResourceFactory.createResource(parent).getLocalName();

                if (currComponentObject.has("update_uri")) {

                    currComponentObject.getJSONArray("update_uri").put(parentLocalID);

                } else {

                    JSONArray updateURIsJSON = new JSONArray();

                    updateURIsJSON.put(parentLocalID);

                    currComponentObject.put("update_uri", updateURIsJSON);

                }

                currComponentObject.put(parentLocalID, outputDataJSON.getJSONObject(0));

            }

        }

    }

    /**
     * This method calculates a parent for an overlay child.
     * @param arrayPosition contains the position of the child in a JSONArray
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the URI of the parent
     */
    private String calculateMDBParent(int arrayPosition, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectWhereBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectWhereBuilder.addWhere("?s", SprO.hasEntryComponent,"<" + this.compositionUpdateJSON.getJSONArray("children").get(arrayPosition).toString() + ">");

        SelectBuilder selectBuilder = new SelectBuilder();

        selectBuilder.addGraph("<" + this.compositionUpdateJSON.getJSONArray("ngs").get(arrayPosition).toString() + ">", selectWhereBuilder);

        selectBuilder.addVar(selectBuilder.makeVar("?s"));

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        String sparqlQueryString = selectBuilder.buildString();

        return connectionToTDB.pullSingleDataFromTDB(this.compositionUpdateJSON.getJSONArray("directories").get(arrayPosition).toString(), sparqlQueryString, "?s");

    }


    /**
     * This method finds the URI of a resource in the jena tdb and set the focus to this URI.
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @param classToFocus contains the URI of a class to focus on
     */
    private void setFocusOnClass(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB, String classToFocus) {

        if (classToFocus.equals(SCBasic.userEntryID.toString())) {

            SOCCOMASResourceFactory SOCCOMASResourceFactory = new SOCCOMASResourceFactory();

            String potentialMDBUEID = SOCCOMASResourceFactory.createMDBUserEntryID(jsonInputObject.get("mdbueid").toString());

            SelectBuilder selectWhereBuilder = new SelectBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

            selectWhereBuilder.addWhere("<" + potentialMDBUEID + ">", RDF.type, SCBasic.userEntryID);

            AskBuilder askBuilder = new AskBuilder();

            askBuilder = prefixesBuilder.addPrefixes(askBuilder);

            askBuilder.addGraph("?g", selectWhereBuilder);

            String sparqlQueryString = askBuilder.buildString();

            TDBPath tdbPath = new TDBPath();

            boolean mdmUEIDExistInTDB = connectionToTDB.statementExistInTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), sparqlQueryString);

            if (mdmUEIDExistInTDB) {

                this.mdbUEID = potentialMDBUEID;

                this.mdbUEIDNotEmpty = true;

                this.currentFocus = this.mdbUEID;

                System.out.println("focus is on: " + this.currentFocus);

            }

        } else if (classToFocus.equals(SCBasic.entryID.toString())) {

            this.mdbEntryID = jsonInputObject.get("mdbentryid").toString();

            this.mdbEntryIDNotEmpty = true;

            this.currentFocus = this.mdbEntryID;

            System.out.println("focus is on: " + this.currentFocus);


        }

    }


    /**
     * This method generates a resource. This resource is used for the generation of other dependent resources.
     * @param newFocusKey contains a key to find the new focus
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param jsonInputObject contains the information for the calculation
     * @param generateResourceFor is an empty String
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a String with an URI
     */
    private String setFocusOnIndividual(String newFocusKey, JSONArray currExecStep, JSONObject jsonInputObject,
                                        String generateResourceFor, JenaIOTDBFactory connectionToTDB) {

        String changeVersionIDToStatus = "";

        for (int i = 0; i < currExecStep.length();i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.changeVersionIDToStatus.toString())) {

                changeVersionIDToStatus = currExecStep.getJSONObject(i).get("object").toString();

            }

        }

        if (newFocusKey.contains("__SPRO_")) {

            newFocusKey = newFocusKey.substring(newFocusKey.indexOf("__") + 2);

            boolean newFocusWasSet = false;

            Iterator<String> generatedKeys = this.generatedResources.keys();

            while (generatedKeys.hasNext()
                    && !newFocusWasSet) {

                String currKey = generatedKeys.next();

                if (currKey.contains(newFocusKey)) {

                    String potentialIndividualURI = this.generatedResources.get(currKey).toString().contains("#")
                            ? this.generatedResources.get(currKey).toString()
                            : this.generatedResources.get(currKey).toString() + "#Dummy_0000000000";

                    SOCCOMASIDFinder soccomasIDFinder = new SOCCOMASIDFinder(potentialIndividualURI, connectionToTDB);

                    if (soccomasIDFinder.hasMDBCoreID() &&
                            soccomasIDFinder.hasMDBEntryID() &&
                            soccomasIDFinder.hasMDBUEID()) {

                        this.mdbUEID = soccomasIDFinder.getMDBUEID();

                        this.mdbUEIDNotEmpty = true;

                        this.mdbCoreID = soccomasIDFinder.getMDBCoreID();

                        this.mdbCoreIDNotEmpty = true;

                        this.mdbEntryID = soccomasIDFinder.getMDBEntryID();

                        this.mdbEntryIDNotEmpty = true;

                        // add info for later calculation
                        jsonInputObject.put("mdbentryid", this.mdbEntryID);

                        if (soccomasIDFinder.getMDBEntryID().equals(this.generatedResources.get(currKey).toString())) {

                            this.currentFocus = this.mdbEntryID;

                            this.focusHasNewNS = true;

                            generateResourceFor = this.generatedResources.get(currKey).toString();

                            System.out.println("INFO: The focus was changed to " + this.currentFocus);

                        }

                    }

                    newFocusWasSet = true;

                }

            }

            if (!newFocusWasSet) {

                for (int i = 0; i < currExecStep.length();i++) {

                    String localNameOfProperty = ResourceFactory.createResource(currExecStep.getJSONObject(i).get("property").toString()).getLocalName();

                    if (localNameOfProperty.equals(newFocusKey)) {

                        generateResourceFor = currExecStep.getJSONObject(i).get("object").toString();

                        if (generateResourceFor.contains(SCBasic.userEntryID.toString())) {

                            SOCCOMASResourceFactory SOCCOMASResourceFactory = new SOCCOMASResourceFactory();

                            this.mdbUEID = SOCCOMASResourceFactory.createMDBUserEntryID(jsonInputObject.get("mdbueid").toString());

                            this.mdbUEIDNotEmpty = true;

                            this.currentFocus = this.mdbUEID;

                            System.out.println("mdbUEID = " + mdbUEID);

                        } else if (generateResourceFor.contains(SCBasic.entryID.toString())) {

                            SOCCOMASResourceFactory SOCCOMASResourceFactory = new SOCCOMASResourceFactory();

                            System.out.println();

                            boolean newCoreIDWasGenerated = false;

                            if (!this.mdbCoreIDNotEmpty) {

                                this.mdbCoreID = SOCCOMASResourceFactory.createMDBCoreID(currExecStep, this.infoInput, jsonInputObject, this.pathToOntologies, connectionToTDB);

                                this.mdbCoreIDNotEmpty = true;

                                newCoreIDWasGenerated = true;

                                System.out.println("MDBCoreID = " + this.mdbCoreID);

                            }

                            System.out.println();

                            if (newCoreIDWasGenerated) {
                                // if a new MDBCoreID was generated >>> this new MDBEntryID starts with the minimum

                                this.mdbEntryID = this.mdbCoreID + "-d_1_1";

                            } else {

                                if (changeVersionIDToStatus.equals(SCBasic.soccomasENTRYSTATUSBASICCurrentPublished.toString())) {

                                    this.mdbEntryID = SOCCOMASResourceFactory.createMDBEntryID(currExecStep, this.mdbCoreID, 'p', connectionToTDB);

                                } else if (changeVersionIDToStatus.equals(SCBasic.soccomasENTRYSTATUSBASICCurrentDraft.toString())) {

                                    if (jsonInputObject.has("mdbentryid")) {

                                        if (jsonInputObject.get("mdbentryid").toString().contains("-d_")) {
                                            // transition: save current draft >>> that means increase only the counter of version id

                                            this.mdbEntryID = SOCCOMASResourceFactory.createNewDraftVersionForMDBEntryID(currExecStep, jsonInputObject.get("mdbentryid").toString(), connectionToTDB);

                                        } else {

                                            // create a complete new draft version for example from the current published version

                                            this.mdbEntryID = SOCCOMASResourceFactory.createMDBEntryID(currExecStep, this.mdbCoreID, 'd', connectionToTDB);

                                        }

                                    } else {
                                        // create a complete new draft version for example from the current published version

                                        this.mdbEntryID = SOCCOMASResourceFactory.createMDBEntryID(currExecStep, this.mdbCoreID, 'd', connectionToTDB);

                                    }

                                }

                            }

                            // add info for later calculation
                            jsonInputObject.put("mdbentryid", this.mdbEntryID);

                            this.mdbEntryIDNotEmpty = true;

                            this.currentFocus = this.mdbEntryID;

                            this.focusHasNewNS = true;

                            System.out.println("MDBEntryID = " + this.mdbEntryID);

                            if (jsonInputObject.has("mdbueid_uri")) {

                                if (!(jsonInputObject.get("mdbueid_uri").toString().equals(""))) {

                                    this.mdbUEID = jsonInputObject.get("mdbueid_uri").toString();

                                } else {

                                    this.mdbUEID = SOCCOMASResourceFactory.findMDBUserEntryID(jsonInputObject.get("mdbueid").toString(), connectionToTDB);

                                }

                            } else {

                                this.mdbUEID = SOCCOMASResourceFactory.findMDBUserEntryID(jsonInputObject.get("mdbueid").toString(), connectionToTDB);

                            }

                            this.mdbUEIDNotEmpty = true;

                            System.out.println("mdbUEID = " + this.mdbUEID);

                            System.out.println();

                        }

                    }

                }

            }

        } else if (this.identifiedResources.has(newFocusKey)) {

            String potentialIndividualURI = this.identifiedResources.get(newFocusKey).toString().contains("#")
                    ? this.identifiedResources.get(newFocusKey).toString()
                    : this.identifiedResources.get(newFocusKey).toString() + "#Dummy_0000000000";

            SOCCOMASIDFinder soccomasIDFinder = new SOCCOMASIDFinder(potentialIndividualURI, connectionToTDB);

            if (soccomasIDFinder.hasMDBCoreID() &&
                    soccomasIDFinder.hasMDBEntryID() &&
                    soccomasIDFinder.hasMDBUEID()) {

                this.mdbUEID = soccomasIDFinder.getMDBUEID();

                this.mdbUEIDNotEmpty = true;

                this.mdbCoreID = soccomasIDFinder.getMDBCoreID();

                this.mdbCoreIDNotEmpty = true;

                this.mdbEntryID = soccomasIDFinder.getMDBEntryID();

                this.mdbEntryIDNotEmpty = true;

                // add info for later calculation
                jsonInputObject.put("mdbentryid", this.mdbEntryID);

                if (soccomasIDFinder.getMDBEntryID().equals(this.identifiedResources.get(newFocusKey).toString())) {

                    this.currentFocus = this.mdbEntryID;

                    this.focusHasNewNS = true;

                    generateResourceFor = this.identifiedResources.get(newFocusKey).toString();

                    System.out.println("INFO: The focus was changed to " + this.currentFocus);

                }

            }

        } else if (this.infoInput.has(newFocusKey)) {

            String potentialIndividualURI = this.infoInput.get(newFocusKey).toString().contains("#")
                    ? this.infoInput.get(newFocusKey).toString()
                    : this.infoInput.get(newFocusKey).toString() + "#Dummy_0000000000";

            if (potentialIndividualURI.equals(SprO.sproVARIABLEEmpty.toString())) {

                this.currentFocus = potentialIndividualURI;

            }

            SOCCOMASIDFinder soccomasIDFinder = new SOCCOMASIDFinder(potentialIndividualURI, connectionToTDB);

            if (soccomasIDFinder.hasMDBCoreID() &&
                    soccomasIDFinder.hasMDBEntryID() &&
                    soccomasIDFinder.hasMDBUEID()) {

                this.mdbUEID = soccomasIDFinder.getMDBUEID();

                this.mdbUEIDNotEmpty = true;

                this.mdbCoreID = soccomasIDFinder.getMDBCoreID();

                this.mdbCoreIDNotEmpty = true;

                this.mdbEntryID = soccomasIDFinder.getMDBEntryID();

                this.mdbEntryIDNotEmpty = true;

                // add info for later calculation
                jsonInputObject.put("mdbentryid", this.mdbEntryID);

                if (soccomasIDFinder.getMDBEntryID().equals(this.infoInput.get(newFocusKey).toString())) {

                    this.currentFocus = this.mdbEntryID;

                    this.focusHasNewNS = true;

                    generateResourceFor = this.infoInput.get(newFocusKey).toString();

                    System.out.println("INFO: The focus was changed to " + this.currentFocus);

                }

            }

        }

        return generateResourceFor;

    }


    /**
     * This method checks if a transition contains the property "mdbuiap:MDB_UIAP_0000000464" with object value "true".
     * @param transitionToCheck contains the URI of the transition
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return "true" if the statement exist, else "false"
     */
    private boolean updateStoreAfterTrackingProcedureExist(String transitionToCheck, JenaIOTDBFactory connectionToTDB) {

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        AskBuilder askBuilder = new AskBuilder();

        askBuilder = prefixesBuilder.addPrefixes(askBuilder);

        SelectBuilder tripleSPO = new SelectBuilder();

        UrlValidator urlValidator = new UrlValidator();

        if (urlValidator.isValid(transitionToCheck)) {

            tripleSPO.addWhere("<" + transitionToCheck + ">", SprO.updateStoreAfterTrackingProcedureBOOLEAN, "true");

        } else {

            tripleSPO.addWhere("<" + transitionToCheck + ">", SprO.updateStoreAfterTrackingProcedureBOOLEAN, "true");

        }

        askBuilder.addGraph("?g", tripleSPO);

        String sparqlQueryString = askBuilder.buildString();

        return connectionToTDB.statementExistInTDB(this.pathToOntologies, sparqlQueryString);

    }


    /**
     * This method checks for a property if the object of this property will be used as input for a transition or
     * workflow.
     * @param potentialInputProperty contains the uri of a property
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return "true" if the object of the property must use for later calculation else "false"
     */
    private boolean useObjectAsInput(String potentialInputProperty, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectWhereBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

        selectWhereBuilder.addWhere("<" + potentialInputProperty + ">", SprO.usedInTransitionOrWorkflowBOOLEAN, "true");

        AskBuilder askBuilder = new AskBuilder();

        askBuilder = prefixesBuilder.addPrefixes(askBuilder);

        askBuilder.addGraph("?g", selectWhereBuilder);

        String sparqlQueryString = askBuilder.buildString();

        return connectionToTDB.statementExistInTDB(this.pathToOntologies, sparqlQueryString);

    }


}
