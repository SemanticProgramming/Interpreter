/*
 * Created by Roman Baum on 19.02.16.
 * Last modified by Roman Baum on 22.01.19.
 */
package soccomas.packages.operation;


import soccomas.basic.ApplicationConfigurator;
import soccomas.basic.SOCCOMASURLEncoder;
import soccomas.basic.StringChecker;
import soccomas.basic.TDBPath;
import soccomas.mongodb.MongoDBConnection;
import soccomas.packages.JenaIOTDBFactory;
import soccomas.packages.KBOrder;
import soccomas.packages.SOCCOMASExecutionStepHandler;
import soccomas.packages.SOCCOMASIDFinder;
import soccomas.packages.querybuilder.FilterBuilder;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.packages.querybuilder.QueryBuilderConverter;
import soccomas.packages.querybuilder.SPARQLFilter;
import soccomas.vocabulary.*;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

public class InputInterpreter {

    private String pathToOntologies = ApplicationConfigurator.getPathToApplicationOntologyStore();

    private boolean currInputIsValid = true;

    private boolean inputIsValid;

    private JSONObject currComponentObject = new JSONObject();

    private String individualURI;

    private JSONObject jsonInputObject;

    private Model overlayModel = ModelFactory.createDefaultModel();

    private MongoDBConnection mongoDBConnection;

    public InputInterpreter(JSONObject jsonInputObject, MongoDBConnection mongoDBConnection) {

        this.jsonInputObject = jsonInputObject;
        this.mongoDBConnection = mongoDBConnection;

    }

    public InputInterpreter(String individualURI, JSONObject jsonInputObject, MongoDBConnection mongoDBConnection) {

        this.individualURI = individualURI;

        this.jsonInputObject = jsonInputObject;

        this.mongoDBConnection = mongoDBConnection;

    }

    public InputInterpreter(String individualURI, JSONObject jsonInputObject, Model overlayModel, MongoDBConnection mongoDBConnection) {

        this.individualURI = individualURI;

        this.jsonInputObject = jsonInputObject;

        this.overlayModel = overlayModel;

        this.mongoDBConnection = mongoDBConnection;

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

        String sparqlQueryString = constructBuilder.buildString();

        Model constructResult = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

        constructResult = checkInputTypeInModel(constructResult, entryComponents, connectionToTDB);

        checkUseKeywordsFromComposition(constructResult);

        Selector selector = new SimpleSelector(null, SprO.triggersClickForEntryComponent, null, "");
        // triggers 'click' for entry component

        StmtIterator typeStmts = constructResult.listStatements(selector);

        ArrayList<Statement> stmtList = new ArrayList<>();

        while (typeStmts.hasNext() && this.currInputIsValid) {

            Statement currStatement = typeStmts.next();

            entryComponents = manageProperty(resourceSubject, currStatement, entryComponents, connectionToTDB);

            stmtList.add(currStatement);

        }

        constructResult.remove(stmtList);

        StmtIterator resultIterator = constructResult.listStatements();

        while (resultIterator.hasNext() && this.currInputIsValid) {

            Statement currStatement = resultIterator.next();

            entryComponents = manageProperty(resourceSubject, currStatement, entryComponents, connectionToTDB);

        }

        return entryComponents;

    }

    /**
     * This method adds the version type to a JSONObject for the output.
     * @param jsonOutputObject contains a JSONObject
     * @param mdbID contains a URI
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject addVersionType (JSONObject jsonOutputObject, String mdbID, JenaIOTDBFactory connectionToTDB) {

        PrefixesBuilder listPB = new PrefixesBuilder();

        SelectBuilder listSWB = new SelectBuilder();

        SelectBuilder innerListSWB = new SelectBuilder();

        Var userVar = listSWB.makeVar("?o");

        listSWB.addVar(userVar);

        innerListSWB = listPB.addPrefixes(innerListSWB);

        innerListSWB.addWhere("<" + mdbID + ">", "<http://purl.org/spar/pso/withStatus>", "?o");

        listSWB = listPB.addPrefixes(listSWB);

        if (mdbID.contains("-s-")) {
            // specimens

            listSWB.fromNamed(SCMDBBasic.namedgraphDraftSpecimenVersionsList.toString());
            // NAMED_GRAPH: draft specimen versions list

            listSWB.fromNamed(SCMDBBasic.namedgraphPublishedSpecimenVersionsList.toString());
            // NAMED_GRAPH: published specimen versions list

            listSWB.fromNamed(SCMDBBasic.namedgraphRecycleBinSpecimenVersionsList.toString());
            // NAMED_GRAPH: recycle bin specimen versions list

        } else if (mdbID.contains("-md-")) {

            listSWB.fromNamed(SCMDBBasic.namedgraphDraftMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: draft morphological description versions list

            listSWB.fromNamed(SCMDBBasic.namedgraphPublishedMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: published morphological description versions list

            listSWB.fromNamed(SCMDBBasic.namedgraphRecycleBinMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: recycle bin morphological description versions list

        }

        listSWB.addGraph("?g", innerListSWB);

        String sparqlQueryString = listSWB.buildString();

        TDBPath tdbPath = new TDBPath();

        String mdbEntryStatus = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYCoreWorkspaceDirectory.toString()), sparqlQueryString, "?o");
        // SOCCOMAS_WORKSPACE_DIRECTORY: core workspace directory

        if (mdbEntryStatus.equals(SCBasic.soccomasENTRYSTATUSBASICCurrentDraft.toString())) {
            // SOCCOMAS_ENTRY_STATUS_BASIC: current draft

            System.out.println(mdbID + " is a current draft!");

            jsonOutputObject.put("inputIsActive" , "true");

        } else {

            System.out.println(mdbID + " is no current draft!");

            jsonOutputObject.put("inputIsActive" , "false");

        }

        return jsonOutputObject;

    }

    /**
     * This method generate the output data for input check of an entry list query
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject checkInputForListEntry(JenaIOTDBFactory connectionToTDB) {

        Model unionModel = ModelFactory.createDefaultModel();

        FilterBuilder filterBuilder = new FilterBuilder();

        PrefixesBuilder draftListPB = new PrefixesBuilder();

        ConstructBuilder draftListCB = new ConstructBuilder();

        SelectBuilder draftListSWB = new SelectBuilder();

        Var sVar = draftListCB.makeVar("?s"), pVar = draftListCB.makeVar("?publishedPVar"), oVar = draftListCB.makeVar("?publishedOVar");

        draftListCB.addConstruct(sVar, pVar, oVar);

        draftListSWB = draftListPB.addPrefixes(draftListSWB);

        draftListSWB.addWhere(sVar, "<http://purl.org/spar/pso/withStatus>", "?oDummy");

        draftListSWB.addWhere(sVar, pVar, oVar);

        draftListCB = draftListPB.addPrefixes(draftListCB);

        if (this.jsonInputObject.get("value").toString().equals("all")) {

            draftListCB.fromNamed(SCMDBBasic.namedgraphDraftMediaVersionsList.toString());
            // NAMED_GRAPH: draft media versions list
            draftListCB.fromNamed(SCMDBBasic.namedgraphDraftMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: draft morphological description versions list
            draftListCB.fromNamed(SCMDBBasic.namedgraphDraftSpecimenVersionsList.toString());
            // NAMED_GRAPH: draft specimen versions list
            draftListCB.fromNamed(SCMDBBasic.namedgraphRecycleBinMediaVersionsList.toString());
            // NAMED_GRAPH: recycle bin media versions list
            draftListCB.fromNamed(SCMDBBasic.namedgraphRecycleBinMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: recycle bin morphological description versions list
            draftListCB.fromNamed(SCMDBBasic.namedgraphRecycleBinSpecimenVersionsList.toString());
            // NAMED_GRAPH: recycle bin specimen versions list

        } else if (this.jsonInputObject.get("value").toString().equals("md")) {

            draftListCB.fromNamed(SCMDBBasic.namedgraphDraftMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: draft morphological description versions list
            draftListCB.fromNamed(SCMDBBasic.namedgraphRecycleBinMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: recycle bin morphological description versions list

        } else if (this.jsonInputObject.get("value").toString().equals("s")) {

            draftListCB.fromNamed(SCMDBBasic.namedgraphDraftSpecimenVersionsList.toString());
            // NAMED_GRAPH: draft specimen versions list
            draftListCB.fromNamed(SCMDBBasic.namedgraphRecycleBinSpecimenVersionsList.toString());
            // NAMED_GRAPH: recycle bin specimen versions list

        }

        draftListCB.addGraph("?g", draftListSWB);

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

        filterItems = filterBuilder.addItems(filterItems, pVar.toString(), "<" + SCBasic.hasHeader.toString() + ">");
        // has header

        ArrayList<String> filter = sparqlFilter.getINFilter(filterItems);

        draftListCB = filterBuilder.addFilter(draftListCB, filter);

        filterItems.clear();

        String sparqlQueryString = draftListCB.buildString();

        TDBPath tdbPath = new TDBPath();

        Model draftModel = connectionToTDB.pullDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYCoreWorkspaceDirectory.toString()), sparqlQueryString);
        // SOCCOMAS_WORKSPACE_DIRECTORY: core workspace directory

        unionModel.add(draftModel);

        filterBuilder = new FilterBuilder();

        PrefixesBuilder publishedListPB = new PrefixesBuilder();

        ConstructBuilder publishedListCB = new ConstructBuilder();

        SelectBuilder publishedListSWB = new SelectBuilder();

        Var publishedSVar = publishedListCB.makeVar("?publishedSVar"), publishedPVar = publishedListCB.makeVar("?publishedPVar"), publishedOVar = publishedListCB.makeVar("?publishedOVar");

        publishedListCB.addConstruct(publishedSVar, publishedPVar, publishedOVar);

        publishedListSWB = publishedListPB.addPrefixes(publishedListSWB);

        publishedListSWB.addWhere(publishedSVar, "<http://purl.org/spar/pso/withStatus>", "?oDummy");

        publishedListSWB.addWhere(publishedSVar, publishedPVar, publishedOVar);

        publishedListCB = publishedListPB.addPrefixes(publishedListCB);

        if (this.jsonInputObject.get("value").toString().equals("all")) {

            publishedListCB.fromNamed(SCMDBBasic.namedgraphPublishedMediaVersionsList.toString());
            // NAMED_GRAPH: published media versions list
            publishedListCB.fromNamed(SCMDBBasic.namedgraphPublishedMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: published morphological description versions list
            publishedListCB.fromNamed(SCMDBBasic.namedgraphPublishedSpecimenVersionsList.toString());
            // NAMED_GRAPH: published specimen versions list

        } else if (this.jsonInputObject.get("value").toString().equals("md")) {

            publishedListCB.fromNamed(SCMDBBasic.namedgraphPublishedMorphologicalDescriptionVersionsList.toString());
            // NAMED_GRAPH: published morphological description versions list

        } else if (this.jsonInputObject.get("value").toString().equals("s")) {

            publishedListCB.fromNamed(SCMDBBasic.namedgraphPublishedSpecimenVersionsList.toString());
            // NAMED_GRAPH: published specimen versions list

        }

        publishedListCB.addGraph("?g", publishedListSWB);

        SPARQLFilter publishedSPARQLFilter = new SPARQLFilter();

        ArrayList<ArrayList<String>> publishedFilterItems = new ArrayList<>();

        publishedFilterItems = filterBuilder.addItems(publishedFilterItems, publishedPVar.toString(), "<" + SCBasic.hasHeader.toString() + ">");

        ArrayList<String> publishedFilter = publishedSPARQLFilter.getINFilter(publishedFilterItems);

        publishedListCB = filterBuilder.addFilter(publishedListCB, publishedFilter);

        publishedFilterItems.clear();

        String publishedSPARQLQueryString = publishedListCB.buildString();

        Model publishedModel = connectionToTDB.pullDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYCoreWorkspaceDirectory.toString()), publishedSPARQLQueryString);

        unionModel.add(publishedModel);

        Selector selector = new SimpleSelector(null, SCBasic.hasHeader, null, "");

        Iterator<Statement> stmtIter = unionModel.listStatements(selector);

        JSONArray outputDataJSON = new JSONArray();

        JSONObject knownUser = new JSONObject();

        while (stmtIter.hasNext()) {

            Statement currStmt = stmtIter.next();

            Resource mdbEntryID = currStmt.getSubject().asResource();

            Resource headerNG = currStmt.getObject().asResource();

            Model headerModel;

            if (headerNG.toString().contains("-d_")
                    && headerNG.toString().contains(this.jsonInputObject.get("mdbueid").toString())) {

                headerModel = connectionToTDB.pullNamedModelFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString()), headerNG.toString());

            } else {

                headerModel = connectionToTDB.pullNamedModelFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYPublishedWorkspaceDirectory.toString()), headerNG.toString());

            }

            JSONObject entryComponents = new JSONObject();

            entryComponents.put("uri", mdbEntryID.toString());

            if (headerModel.contains(mdbEntryID, SCBasic.ofEntryType)) {

                Statement stmt = headerModel.getProperty(mdbEntryID, SCBasic.ofEntryType);

                entryComponents.put(stmt.getPredicate().getLocalName(), stmt.getObject().asResource().getLocalName());

            }

            if (headerModel.contains(mdbEntryID, SprO.hasVisibleLabel1)) {

                Statement stmt = headerModel.getProperty(mdbEntryID, SprO.hasVisibleLabel1);

                entryComponents.put("entryLabel", stmt.getObject().asLiteral().getLexicalForm());

            }

            if (headerModel.contains(mdbEntryID, SprO.hasVisibleLabel2)) {

                Statement stmt = headerModel.getProperty(mdbEntryID, SprO.hasVisibleLabel2);

                entryComponents.put("entryLabel2", stmt.getObject().asLiteral().getLexicalForm());

            }

            if (headerModel.contains(mdbEntryID, ResourceFactory.createProperty("http://purl.org/spar/pso/withStatus"))) {
                // withStatus

                Statement stmt = headerModel.getProperty(mdbEntryID, ResourceFactory.createProperty("http://purl.org/spar/pso/withStatus"));

                entryComponents.put(stmt.getPredicate().getLocalName(), stmt.getObject().asResource().getLocalName());

            }

            if (headerModel.contains(mdbEntryID, ResourceFactory.createProperty("http://purl.org/dc/terms/issued"))) {
                // issued

                Statement stmt = headerModel.getProperty(mdbEntryID, ResourceFactory.createProperty("http://purl.org/dc/terms/issued"));

                String[] dateParts = (stmt.getObject().asLiteral().getString()).split("\\.");

                String date = "";

                if (dateParts.length > 1) {

                    date = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];

                }

                entryComponents.put("createdOn", date);

            } else if (headerModel.contains(mdbEntryID, PAV.createdOn)) {
                // created on

                Statement stmt = headerModel.getProperty(mdbEntryID, PAV.createdOn);

                String[] dateParts = (stmt.getObject().asLiteral().getString()).split("\\.");

                String date = "";

                if (dateParts.length > 1) {

                    date = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];

                }

                entryComponents.put("createdOn", date);

            }

            if (headerModel.contains(mdbEntryID, PAV.createdBy)) {
                // createdBy

                Statement stmt = headerModel.getProperty(mdbEntryID, PAV.createdBy);

                String ueidString = stmt.getObject().asResource().getNameSpace();

                Resource ueid = ResourceFactory.createResource(ueidString.substring(0, (ueidString.length()-1)));

                if (!(knownUser.has(ueid.toString()))) {

                    PrefixesBuilder userListPB = new PrefixesBuilder();

                    SelectBuilder userListCB = new SelectBuilder();

                    SelectBuilder userListSWB = new SelectBuilder();

                    Var userVar = userListCB.makeVar("?o");

                    userListCB.addVar(userVar);

                    userListSWB = userListPB.addPrefixes(userListSWB);

                    userListSWB.addWhere(ueid , SCBasic.hasUserEntryIDNamedGraph, userVar);
                    // has user entry ID named graph

                    userListCB = userListPB.addPrefixes(userListCB);

                    userListCB.addGraph(SCBasic.namedgraphUserEntryList, userListSWB);
                    // NAMED_GRAPH: MDB user entry list

                    String userListSPARQLQueryString = userListCB.buildString();

                    String userEntryIDNG = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), userListSPARQLQueryString, userVar.toString());
                    // SOCCOMAS_WORKSPACE_DIRECTORY: admin workspace directory

                    Model userEntryModel = connectionToTDB.pullNamedModelFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), userEntryIDNG);
                    // SOCCOMAS_WORKSPACE_DIRECTORY: admin workspace directory

                    if (userEntryModel.contains(ueid, SprO.hasVisibleLabel1)) {
                        // has visible label 1

                        Statement userStmt = userEntryModel.getProperty(ueid, SprO.hasVisibleLabel1);

                        entryComponents.put(userStmt.getPredicate().getLocalName(), userStmt.getObject().asLiteral().getLexicalForm());

                        if (!(knownUser.has(ueid.toString()))) {

                            JSONObject currKnownUser = new JSONObject();

                            currKnownUser.put(userStmt.getPredicate().getLocalName(), userStmt.getObject().asLiteral().getLexicalForm());

                            knownUser.put(ueid.toString(), currKnownUser);

                        } else {

                            (knownUser.getJSONObject(ueid.toString()))
                                    .put(userStmt.getPredicate().getLocalName(), userStmt.getObject().asLiteral().getLexicalForm());

                        }

                    }

                    if (userEntryModel.contains(ueid, SprO.hasVisibleLabel3)) {

                        Statement userStmt = userEntryModel.getProperty(ueid, SprO.hasVisibleLabel3);

                        entryComponents.put(userStmt.getPredicate().getLocalName(), userStmt.getObject().asLiteral().getLexicalForm());

                        if (!(knownUser.has(ueid.toString()))) {

                            JSONObject currKnownUser = new JSONObject();

                            currKnownUser.put(userStmt.getPredicate().getLocalName(), userStmt.getObject().asLiteral().getLexicalForm());

                            knownUser.put(ueid.toString(), currKnownUser);

                        } else {

                            (knownUser.getJSONObject(ueid.toString()))
                                    .put(userStmt.getPredicate().getLocalName(), userStmt.getObject().asLiteral().getLexicalForm());

                        }

                    }

                } else {

                    Iterator<String> keysIter = knownUser.getJSONObject(ueid.toString()).keys();

                    while (keysIter.hasNext()) {

                        String currKey = keysIter.next();

                        entryComponents.put(currKey, knownUser.getJSONObject(ueid.toString()).get(currKey).toString());

                    }

                }

            }

            int numberOfKeys = 0;

            Iterator<String> keys = entryComponents.keys();

            while (keys.hasNext()) {

                keys.next();

                numberOfKeys++;

            }

            if (numberOfKeys > 6) {

                outputDataJSON.put(entryComponents);

            }

        }

        JSONObject outputObject = new JSONObject();

        outputObject.put("data", outputDataJSON);

        return outputObject;

    }

    /**
     * This method extract(s) statement(s) from the model.
     * @param constructResult contains the gui input type in one statement
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return the input model without the extracted statement(s)
     */
    private Model checkInputTypeInModel(Model constructResult, JSONObject entryComponents, JenaIOTDBFactory connectionToTDB) {

        while (constructResult.contains(null, SprO.hasGUIInputType) && !this.inputIsValid) {
            // check all gui input types - multiple statements are possible

            Statement hasGUIInputTypeStmt = constructResult.getProperty(null, SprO.hasGUIInputType);

            //System.out.println("hasGUIInputTypeStmt = " + hasGUIInputTypeStmt);

            checkInputTypeInStmt(hasGUIInputTypeStmt, entryComponents, connectionToTDB);

            constructResult.remove(hasGUIInputTypeStmt);

        }

        return constructResult;

    }

    /**
     * This method check if the input has the correct mdb data type.
     * @param stmtToCheck contains the gui input type in the object
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    private JSONObject checkInputTypeInStmt(Statement stmtToCheck, JSONObject entryComponents, JenaIOTDBFactory connectionToTDB) {

        String typeToCheck = stmtToCheck.getObject().toString();

        //System.out.println("typeToCheck = " + typeToCheck);

        UrlValidator annotationValidator = new UrlValidator();

        // get a MDB url Encoder to encode the uri with utf-8
        SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

        StringChecker stringChecker = new StringChecker();

        if (typeToCheck.equals(SprO.iNPUTCONTROLOntologyClass.toString())) {

            if (annotationValidator
                    .isValid(soccomasURLEncoder.encodeUrl(this.jsonInputObject.get("value").toString(), "UTF-8"))) {

                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                SelectBuilder selectWhereBuilder = new SelectBuilder();

                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                selectWhereBuilder.addWhere("<" + this.jsonInputObject.get("value").toString() + ">", RDF.type, OWL2.Class);

                AskBuilder askBuilder = new AskBuilder();

                askBuilder = prefixesBuilder.addPrefixes(askBuilder);

                askBuilder.addGraph("?g", selectWhereBuilder);

                // create a Query
                String sparqlQueryString = askBuilder.buildString();

                boolean validInput = connectionToTDB.statementExistInTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryString);

                if (validInput) {

                    this.currComponentObject.put("valid", "true");

                    this.currInputIsValid = true;

                    this.inputIsValid = true;

                    return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                }

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLOntologyInstanceIndividual.toString())) {

            if (annotationValidator
                    .isValid(soccomasURLEncoder.encodeUrl(this.jsonInputObject.get("value").toString(), "UTF-8"))) {

                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                SelectBuilder selectWhereBuilder = new SelectBuilder();

                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                selectWhereBuilder.addWhere("<" + this.jsonInputObject.get("value").toString() + ">", RDF.type, OWL2.NamedIndividual);

                AskBuilder askBuilder = new AskBuilder();

                askBuilder = prefixesBuilder.addPrefixes(askBuilder);

                askBuilder.addGraph("?g", selectWhereBuilder);

                // create a Query
                String sparqlQueryString = askBuilder.buildString();

                boolean validInput = connectionToTDB.statementExistInTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryString);

                if (validInput) {

                    this.currComponentObject.put("valid", "true");

                    this.currInputIsValid = true;

                    this.inputIsValid = true;

                    return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                }

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLLiteral.toString())
                || typeToCheck.equals(SprO.iNPUTCONTROLPassword.toString())) {

            if (((this.jsonInputObject.get("value").toString()).getClass().equals("".getClass()))) {

                this.currComponentObject.put("valid", "true");

                this.currInputIsValid = true;

                this.inputIsValid = true;

                return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

            }


        } else if (typeToCheck.equals(SprO.iNPUTCONTROLFloat.toString())) {

            if (stringChecker.checkIfStringIsAFloat(this.jsonInputObject.get("value").toString())) {

                if (Float.parseFloat(this.jsonInputObject.get("value").toString()) > 0) {

                    this.currComponentObject.put("valid", "true");

                    this.currInputIsValid = true;

                    this.inputIsValid = true;

                    return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                }

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLPositiveInteger.toString())) {

            if (stringChecker.checkIfStringIsAnInteger(this.jsonInputObject.get("value").toString())) {

                if (Integer.parseInt(this.jsonInputObject.get("value").toString()) > 0) {

                    this.currComponentObject.put("valid", "true");

                    this.currInputIsValid = true;

                    this.inputIsValid = true;

                    return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                }

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLEmailAddress.toString())) {

            if (EmailValidator.getInstance().isValid(this.jsonInputObject.get("value").toString())) {

                this.currComponentObject.put("valid", "true");

                this.currInputIsValid = true;

                this.inputIsValid = true;

                return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLWebAddress.toString())) {

            SOCCOMASURLEncoder mdbLEncoderSomeValue = new SOCCOMASURLEncoder();

            UrlValidator urlValidatorSomeValue = new UrlValidator();

            if (urlValidatorSomeValue.isValid(mdbLEncoderSomeValue.encodeUrl(this.jsonInputObject.get("value").toString(), "UTF-8"))) {

                this.currComponentObject.put("valid", "true");

                this.currInputIsValid = true;

                this.inputIsValid = true;

                return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLDateTimeStamp.toString())) {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            dateFormat.setLenient(false);

            boolean parsingSuccessful = true;

            try {

                dateFormat.parse((this.jsonInputObject.get("value").toString()).trim());

            } catch (ParseException pe) {

                parsingSuccessful = false;

            }

            if (parsingSuccessful) {

                this.currComponentObject.put("valid", "true");

                this.currInputIsValid = true;

                this.inputIsValid = true;

                return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLUserEntryID.toString())) {

            if (annotationValidator
                    .isValid(soccomasURLEncoder.encodeUrl(this.jsonInputObject.get("value").toString(), "UTF-8"))) {

                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                SelectBuilder selectWhereBuilder = new SelectBuilder();

                selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

                selectWhereBuilder.addWhere("<" + this.jsonInputObject.get("value").toString() + ">", RDF.type, SCBasic.userEntryID);

                AskBuilder askBuilder = new AskBuilder();

                askBuilder = prefixesBuilder.addPrefixes(askBuilder);

                askBuilder.addGraph("<" + this.jsonInputObject.get("value").toString() + "#" + SCBasic.userEntryIDIndividualsNamedGraph.getLocalName() + "_1>", selectWhereBuilder);

                // create a Query
                String sparqlQueryString = askBuilder.buildString();

                TDBPath tdbPath = new TDBPath();

                boolean validInput = connectionToTDB.statementExistInTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), sparqlQueryString);

                if (validInput) {

                    this.currComponentObject.put("valid", "true");

                    this.currInputIsValid = true;

                    this.inputIsValid = true;

                    return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                }

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLBoolean.toString())) {

            if (this.jsonInputObject.get("value").toString().equals("true")
                    || this.jsonInputObject.get("value").toString().equals("false")) {

                this.currComponentObject.put("valid", "true");

                this.currInputIsValid = true;

                this.inputIsValid = true;

                return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLClick.toString())) {

            this.currInputIsValid = true;

            this.inputIsValid = true;

            return entryComponents;

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLPhoneNumber.toString())) {

            String pattern = "^\\+?\\s?\\d{0,}\\s?\\(?\\s?\\+?\\s?\\d{0,}\\s?\\(?\\s?\\)?\\s?\\d{0,}\\s?\\-?\\s?\\d{1,}\\s?\\d{1,}\\s?\\d{1,}$";

            if ((this.jsonInputObject.get("value").toString()).matches(pattern)) {

                this.currComponentObject.put("valid", "true");

                this.currInputIsValid = true;

                this.inputIsValid = true;

                return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLOntologyResourceThroughLiteralAndAutocomplete.toString())) {

            if (annotationValidator
                    .isValid(soccomasURLEncoder.encodeUrl(this.jsonInputObject.get("value").toString(), "UTF-8"))) {

                this.currComponentObject.put("valid", "true");

                this.currInputIsValid = true;

                this.inputIsValid = true;

                return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLIntegerPercentage0100.toString())) {

            if (stringChecker.checkIfStringIsAnInteger(this.jsonInputObject.get("value").toString())) {

                if (Integer.parseInt(this.jsonInputObject.get("value").toString()) > 0
                        && Integer.parseInt(this.jsonInputObject.get("value").toString()) <= 100) {

                    this.currComponentObject.put("valid", "true");

                    this.currInputIsValid = true;

                    this.inputIsValid = true;

                    return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                }

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLFloatPercentage0100.toString())) {

            if (stringChecker.checkIfStringIsAFloat(this.jsonInputObject.get("value").toString())) {

                if (Float.parseFloat(this.jsonInputObject.get("value").toString()) > 0
                        && Float.parseFloat(this.jsonInputObject.get("value").toString()) <= 100) {

                    this.currComponentObject.put("valid", "true");

                    this.currInputIsValid = true;

                    this.inputIsValid = true;

                    return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                }

            }

        } else if (typeToCheck.equals(SprO.iNPUTCONTROLFloatPH.toString())) {

            if (stringChecker.checkIfStringIsAFloat(this.jsonInputObject.get("value").toString())) {

                if (Float.parseFloat(this.jsonInputObject.get("value").toString()) > 0
                        && Float.parseFloat(this.jsonInputObject.get("value").toString()) <= 14) {

                    this.currComponentObject.put("valid", "true");

                    this.currInputIsValid = true;

                    this.inputIsValid = true;

                    return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                }

            }

        }

        if ((this.jsonInputObject.get("value").toString()).trim().isEmpty()) {

            this.jsonInputObject.put("value", "");

            this.currComponentObject.put("valid", "true");

            this.currInputIsValid = true;

            this.inputIsValid = true;

        } else {

            SelectBuilder selectBuilder = new SelectBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

            SelectBuilder innerSelect = new SelectBuilder();

            innerSelect.addWhere("<" + typeToCheck + ">", SprO.applicationErrorMessage, "?o");

            selectBuilder.addVar(selectBuilder.makeVar("?o"));

            selectBuilder.addGraph("?g", innerSelect);

            String sparqlQueryString = selectBuilder.buildString();

            String errorMessage = connectionToTDB.pullSingleDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQueryString, "?o");

            if (!errorMessage.isEmpty()) {

                this.currComponentObject.put(SprO.applicationErrorMessage.getLocalName(), errorMessage);

            }

            this.currComponentObject.put("valid", "false");

            this.currInputIsValid = false;

        }



        return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

    }

    /**
     * This method get the corresponding properties for a subject class resource from the jena tdb and save the
     * corresponding statements in an JSONObject.
     * @param classSubject contains the uri of an resource
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject checkClassProperties (String classSubject, JSONObject entryComponents, JenaIOTDBFactory connectionToTDB) {

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

        String sparqlQueryString = constructBuilder.buildString();

        Model constructResult = connectionToTDB.pullDataFromTDB(this.pathToOntologies, sparqlQueryString);

        System.out.println("constructResult" + constructResult);

        StmtIterator resultIterator = constructResult.listStatements();

        while (resultIterator.hasNext() && this.currInputIsValid) {

            Statement currStatement = resultIterator.next();

            entryComponents = manageProperty(classSubject, currStatement, entryComponents, connectionToTDB);

        }

        return entryComponents;

    }

    /**
     * This method checks if the model contains the 'use keywords from composition  [BOOLEAN]' or not.
     * @param constructResult is a model to check for input
     */
    private void checkUseKeywordsFromComposition(Model constructResult) {

        if (constructResult.contains(null, SprO.useSPrOVariablesFromCompositionBOOLEAN) && this.inputIsValid) {

            Statement useKeywordsFromCompositionStmt = constructResult.getProperty(null, SprO.useSPrOVariablesFromCompositionBOOLEAN);

            this.jsonInputObject.put("useKeywordsFromComposition", useKeywordsFromCompositionStmt.getObject().asLiteral().getLexicalForm());

            constructResult.remove(useKeywordsFromCompositionStmt);

        }

    }

    /**
     * This method finds a value for a key, if the key exist in the entry components
     * @param entryComponents contains the data of an entry resource
     * @param key contains a key for investigation
     * @param value contains an empty string or the value of the key
     * @return the value for a key
     */
    private String findAndRemoveKeyInEntryComponents(JSONObject entryComponents, String key, String value) {

        Iterator<String> keyIter = entryComponents.keys();

        if (entryComponents.has(key)) {

            value = entryComponents.get(key).toString();

            entryComponents.remove(key);

            return value;

        } else {

            while (keyIter.hasNext()) {

                String currKey = keyIter.next();

                if (entryComponents.get(currKey) instanceof JSONObject) {

                    value = findAndRemoveKeyInEntryComponents(entryComponents.getJSONObject(currKey), key, value);

                }

            }

        }

        return value;

    }

    /**
     * This method reorders some <key, value> pairs, if necessary
     * @param outputObject contains the data for the output
     * @param entryComponents contains the data of an entry resource
     * @return the (reordered) JSON output object
     */
    private JSONObject reorderOutputObject (JSONObject outputObject, JSONObject entryComponents) {

        String keywordKnownResourceA = "";

        keywordKnownResourceA = findAndRemoveKeyInEntryComponents(entryComponents, SprO.sproVARIABLEKnownResourceA.toString(), keywordKnownResourceA);

        if (!keywordKnownResourceA.equals("")) {

            outputObject.put(SprO.sproVARIABLEKnownResourceA.toString(), keywordKnownResourceA);

        }

        String keywordKnownResourceB = "";

        keywordKnownResourceB = findAndRemoveKeyInEntryComponents(entryComponents, SprO.sproVARIABLEKnownResourceB.toString(), keywordKnownResourceB);

        if (!keywordKnownResourceB.equals("")) {

            outputObject.put(SprO.sproVARIABLEKnownResourceB.toString(), keywordKnownResourceB);

        }

        String loadPage = "";

        loadPage = findAndRemoveKeyInEntryComponents(entryComponents, "load_page", loadPage);

        if (!loadPage.equals("")) {

            outputObject.put("load_page", loadPage);

        }

        String loadPageLocalID = "";

        loadPageLocalID = findAndRemoveKeyInEntryComponents(entryComponents, "load_page_localID", loadPageLocalID);

        if ((!loadPageLocalID.equals("")) && (!loadPageLocalID.contains("."))) {

            outputObject.put("load_page_localID", loadPageLocalID);

        }

        return outputObject;

    }

    /**
     * This method generate the output data for autocomplete check
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject checkAutocomplete(JenaIOTDBFactory connectionToTDB) {

        JSONObject outputObject = new JSONObject();


        if (this.jsonInputObject.has(SprO.autocompleteForOntology.getLocalName())
                || this.jsonInputObject.has(SprO.autocompleteFor.getLocalName())) {

            JSONArray externalOntologyURIJSON = new JSONArray();

            if (this.jsonInputObject.has(SprO.autocompleteForOntology.getLocalName())) {

                if (this.jsonInputObject.get(SprO.autocompleteForOntology.getLocalName()) instanceof JSONArray) {

                    externalOntologyURIJSON = this.jsonInputObject.getJSONArray(SprO.autocompleteForOntology.getLocalName());

                } else if (this.jsonInputObject.get(SprO.autocompleteForOntology.getLocalName()) instanceof String) {

                    externalOntologyURIJSON.put(this.jsonInputObject.get(SprO.autocompleteForOntology.getLocalName()).toString());

                }

            }

            if (this.jsonInputObject.has(SprO.autocompleteFor.getLocalName())) {

                if (this.jsonInputObject.get(SprO.autocompleteFor.getLocalName()) instanceof JSONArray) {

                    externalOntologyURIJSON = this.jsonInputObject.getJSONArray(SprO.autocompleteFor.getLocalName());

                } else if (this.jsonInputObject.get(SprO.autocompleteFor.getLocalName()) instanceof String) {

                    externalOntologyURIJSON.put(this.jsonInputObject.get(SprO.autocompleteFor.getLocalName()).toString());

                }

            }

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            SelectBuilder selectBuilder = new SelectBuilder();

            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

            SelectBuilder subSelectBuilder = new SelectBuilder();

            subSelectBuilder = prefixesBuilder.addPrefixes(subSelectBuilder);

            // the angle brackets in the object are necessary to build a jena text query
            subSelectBuilder.addWhere("?s", "<http://jena.apache.org/text#query>", "<(rdfs:label '" + this.jsonInputObject.get("value").toString() + "*' 10)>");
            subSelectBuilder.addWhere("?s", RDFS.label, "?label");

            selectBuilder.addGraph("?g", subSelectBuilder);

            for (int i = 0; i < externalOntologyURIJSON.length(); i++) {

                selectBuilder.fromNamed("http://www.soccomas.org/Ontologies/SOCCOMAS/" + ResourceFactory.createResource(externalOntologyURIJSON.get(i).toString()).getLocalName());

            }

            selectBuilder.setDistinct(true);

            QueryBuilderConverter queryBuilderConverter = new QueryBuilderConverter();

            String sparqlQueryString = queryBuilderConverter.toString(selectBuilder);

            String basicPathToLucene = ApplicationConfigurator.getPathToLuceneStore() + "external-ontologies/";

            JSONArray autoCompleteResults = connectionToTDB.pullAutoCompleteFromTDBLucene(ApplicationConfigurator.getPathToJenaStore() + "external-ontologies/", basicPathToLucene, sparqlQueryString);

            this.currComponentObject.put("autoCompleteData", autoCompleteResults);

            JSONArray outputDataJSON = new JSONArray();

            JSONObject entryComponents = new JSONObject();

            entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

            outputDataJSON.put(entryComponents);

            outputObject.put("data", outputDataJSON);

        }

        outputObject.put("localID", jsonInputObject.get("localID").toString());

        return outputObject;

    }

    /**
     * This method generate the output data for input check
     * @param classURI contains the URI of a subject class
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject checkInput(String classURI, JenaIOTDBFactory connectionToTDB) {

        JSONArray outputDataJSON = new JSONArray();

        JSONObject entryComponents = new JSONObject();

        entryComponents = checkClassProperties(classURI, entryComponents, connectionToTDB);

        //System.out.println("entryComponents = " + entryComponents);

        outputDataJSON.put(entryComponents);

        //System.out.println("outputDataJSON = " + outputDataJSON);

        JSONObject outputObject = new JSONObject();

        outputObject.put("data", outputDataJSON);

        outputObject = reorderOutputObject(outputObject, entryComponents);

        if (outputObject.getJSONArray("data").getJSONObject(0).getJSONObject(this.jsonInputObject.get("localID").toString()).has("data")) {

            Iterator<String> keys = outputObject.getJSONArray("data").getJSONObject(0).getJSONObject(this.jsonInputObject.get("localID").toString()).keys();

            JSONObject dummyObject = new JSONObject();

            while (keys.hasNext()) {

                String currKey = keys.next();

                dummyObject.put(currKey, outputObject.getJSONArray("data").getJSONObject(0).getJSONObject(this.jsonInputObject.get("localID").toString()).get(currKey));

            }

            keys = dummyObject.keys();

            while (keys.hasNext()) {

                String currKey = keys.next();

                outputObject.put(currKey, dummyObject.get(currKey));

            }

        }

        if (this.jsonInputObject.has("create_new_entry")) {
            // move information from input to output

            outputObject.put("create_new_entry", this.jsonInputObject.get("create_new_entry").toString());

            this.jsonInputObject.remove("create_new_entry");

        }

        if (this.jsonInputObject.has("subsequently_workflow_action")) {
            // move information from input to output

            outputObject.put("subsequently_workflow_action", this.jsonInputObject.get("subsequently_workflow_action").toString());

            this.jsonInputObject.remove("subsequently_workflow_action");

            if (this.jsonInputObject.has("subsequently_root")) {
                // move information from input to output

                outputObject.put("subsequently_root", this.jsonInputObject.get("subsequently_root").toString());

                this.jsonInputObject.remove("subsequently_root");

            }

            if (this.jsonInputObject.has("keywords_to_transfer")) {
                // move information from input to output

                outputObject.put("keywords_to_transfer", this.jsonInputObject.getJSONObject("keywords_to_transfer"));

                this.jsonInputObject.remove("keywords_to_transfer");

            }

        }

        if (this.jsonInputObject.has("subsequently_redirected")) {
            // move information from input to output

            outputObject.put("subsequently_redirected", this.jsonInputObject.get("subsequently_redirected").toString());

            this.jsonInputObject.remove("subsequently_redirected");

            if (this.jsonInputObject.has("redirect_to_hyperlink")) {
                // move information from input to output

                outputObject.put("redirect_to_hyperlink", this.jsonInputObject.get("redirect_to_hyperlink").toString());

                this.jsonInputObject.remove("redirect_to_hyperlink");

            }

        }

        if (this.jsonInputObject.has("use_in_known_subsequent_WA")) {
            // move information from input to output

            outputObject.put("use_in_known_subsequent_WA", this.jsonInputObject.getJSONObject("use_in_known_subsequent_WA"));

            this.jsonInputObject.remove("use_in_known_subsequent_WA");

        }

        outputObject.put("localID", this.jsonInputObject.get("localID").toString());

        return outputObject;
    }


    /**
     * This method is a getter for the overlay named graph.
     * @return a jena model for a MDB overlay
     */
    public Model getOverlayModel() {

        return this.overlayModel;

    }


    /**
     * This method fills the JSONObject with data of an entry component corresponding to a specific property.
     * @param resourceSubject is the URI of a individual resource
     * @param currStatement contains a subject(class or individual), a property and an object for calculation
     * @param entryComponents contains the data of an entry resource
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a JSONObject with data of an entry component
     */
    public JSONObject manageProperty(String resourceSubject, Statement currStatement, JSONObject entryComponents, JenaIOTDBFactory connectionToTDB) {

        String propertyToCheck = currStatement.getPredicate().toString();

        if (propertyToCheck.equals(SprO.executionStepTrigger.toString())) {

            return checkAnnotationAnnotationProperties(resourceSubject, currStatement, entryComponents, connectionToTDB);

        } else if (propertyToCheck.equals(SprO.requiredInputBOOLEAN.toString())) {

            if (currStatement.getObject().isLiteral()) {

                if (currStatement.getObject().asLiteral().getBoolean()) {

                    if (!(this.jsonInputObject.get("value").toString().isEmpty())) {

                        this.currComponentObject.put("valid", "true");

                        return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

                    }

                }

            }

            this.currComponentObject.put("valid", "false");

            this.currInputIsValid = false;

            return entryComponents.put(this.jsonInputObject.get("localID").toString(), this.currComponentObject);

        } else if (propertyToCheck.equals(SprO.executionStepTriggered.toString())) {

            boolean keywordsToTransfer = false;

            JSONObject localIdentifiedResources = new JSONObject();

            if (this.jsonInputObject.has("keywords_to_transfer")) {

                localIdentifiedResources = new JSONObject(this.jsonInputObject.getJSONObject("keywords_to_transfer").toString());

                keywordsToTransfer = true;

            }

            KBOrder kbOrder = new KBOrder(connectionToTDB, this.pathToOntologies, resourceSubject);

            // get the sorted input knowledge base
            JSONArray sortedKBJSONArray = kbOrder.getSortedKBJSONArray();

            System.out.println("sortedKBJSONArray" + sortedKBJSONArray);

            // get the sorted indices of the knowledge base
            JSONArray sortedKBIndicesJSONArray = kbOrder.getSortedKBIndicesJSONArray();

            System.out.println("sortedKBIndicesJSONArray" + sortedKBIndicesJSONArray);

            SOCCOMASExecutionStepHandler soccomasExecutionStepHandler;

            SOCCOMASIDFinder soccomasIDFinder = new SOCCOMASIDFinder(this.individualURI, connectionToTDB);

            if (soccomasIDFinder.hasMDBCoreID() &&
                    soccomasIDFinder.hasMDBEntryID() &&
                    soccomasIDFinder.hasMDBUEID()) {

                this.jsonInputObject.put("mdbentryid", soccomasIDFinder.getMDBEntryID());
                this.jsonInputObject.put("mdbcoreid", soccomasIDFinder.getMDBCoreID());

                String mdbUEID;

                if (this.jsonInputObject.has("mdbueid_uri")) {

                    mdbUEID = this.jsonInputObject.get("mdbueid_uri").toString();

                } else {

                    mdbUEID = soccomasIDFinder.getMDBUEID();

                }

                if (keywordsToTransfer) {

                    soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(soccomasIDFinder.getMDBCoreID(), soccomasIDFinder.getMDBEntryID(), mdbUEID, localIdentifiedResources, this.overlayModel, this.mongoDBConnection);

                } else {

                    soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(soccomasIDFinder.getMDBCoreID(), soccomasIDFinder.getMDBEntryID(), mdbUEID, this.overlayModel, this.mongoDBConnection);

                }

            } else {

                SelectBuilder selectBuilder = new SelectBuilder();

                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                SelectBuilder innerSelect = new SelectBuilder();

                innerSelect.addWhere("<" + this.individualURI + ">", RDF.type, "<" + resourceSubject + ">");
                innerSelect.addWhere("<" + this.individualURI + ">", SprO.isGeneralApplicationItemBOOLEAN, "?isGeneralItem");
                // is general application item  [BOOLEAN]

                selectBuilder.addVar(selectBuilder.makeVar("?isGeneralItem"));

                selectBuilder.addGraph("?g", innerSelect);

                String sparqlQueryString = selectBuilder.buildString();

                String isGeneralItem = connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?isGeneralItem");

                String resourceFromHTMLForm;

                if (isGeneralItem.equals("true")) {

                    resourceFromHTMLForm = ApplicationConfigurator.getDomain() + "/" + this.jsonInputObject.get("html_form").toString();

                } else {

                    resourceFromHTMLForm = ApplicationConfigurator.getDomain() + "/resource/" + this.jsonInputObject.get("html_form").toString();

                }

                System.out.println("resourceFromHTMLForm = " + resourceFromHTMLForm);

                soccomasIDFinder = new SOCCOMASIDFinder(resourceFromHTMLForm, connectionToTDB);

                if (soccomasIDFinder.hasMDBCoreID() &&
                        soccomasIDFinder.hasMDBEntryID() &&
                        soccomasIDFinder.hasMDBUEID()) {

                    this.jsonInputObject.put("mdbentryid", soccomasIDFinder.getMDBEntryID());
                    this.jsonInputObject.put("mdbcoreid", soccomasIDFinder.getMDBCoreID());

                    String mdbUEID;

                    if (this.jsonInputObject.has("mdbueid_uri")) {

                        mdbUEID = this.jsonInputObject.get("mdbueid_uri").toString();

                    } else {

                        mdbUEID = soccomasIDFinder.getMDBUEID();

                    }

                    if (keywordsToTransfer) {

                        soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(soccomasIDFinder.getMDBCoreID(), soccomasIDFinder.getMDBEntryID(), mdbUEID, localIdentifiedResources, this.overlayModel, this.mongoDBConnection);

                    } else {

                        soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(soccomasIDFinder.getMDBCoreID(), soccomasIDFinder.getMDBEntryID(), mdbUEID, this.overlayModel, this.mongoDBConnection);

                    }

                } else if(this.jsonInputObject.has("mdbueid_uri")) {

                    if (this.individualURI.startsWith(ApplicationConfigurator.getDomain() + "/resource/dummy-overlay#")) {
                        // special case for GUI_COMPONENT__BASIC_WIDGET: specify required information

                        this.mongoDBConnection.putJSONInputObjectInMongoDB(this.jsonInputObject);

                        String mdbCoreID = ResourceFactory.createResource(this.individualURI).getNameSpace().substring(0, ResourceFactory.createResource(this.individualURI).getNameSpace().length() - 1);
                        String mdbEntryID = ResourceFactory.createResource(this.individualURI).getNameSpace().substring(0, ResourceFactory.createResource(this.individualURI).getNameSpace().length() - 1);
                        String mdbUEID = this.jsonInputObject.get("mdbueid_uri").toString();

                        this.jsonInputObject.put("mdbentryid", mdbEntryID);
                        this.jsonInputObject.put("mdbcoreid", mdbCoreID);

                        soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(mdbCoreID, mdbEntryID, mdbUEID, localIdentifiedResources, this.overlayModel, this.mongoDBConnection);

                    } else {

                        soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.jsonInputObject.get("mdbueid_uri").toString(), this.overlayModel, this.mongoDBConnection);

                    }

                } else {

                    soccomasExecutionStepHandler = new SOCCOMASExecutionStepHandler(this.mongoDBConnection);

                }

            }

            JSONObject outputObject = soccomasExecutionStepHandler.convertKBToJSONObject(sortedKBJSONArray, sortedKBIndicesJSONArray, this.currComponentObject, this.jsonInputObject, connectionToTDB);

            this.overlayModel = soccomasExecutionStepHandler.getOverlayModel();

            if (outputObject.has("valid")) {
                // not in every case exist a valid information in the JSON structure

                if (!Boolean.valueOf(outputObject.get("valid").toString())) {

                    this.currInputIsValid = false;

                }

            }

            if (outputObject.has("subsequently_workflow_action")) {

                this.jsonInputObject.put("subsequently_workflow_action", outputObject.get("subsequently_workflow_action").toString());

                if (outputObject.has("subsequently_root")) {

                    this.jsonInputObject.put("subsequently_root", outputObject.get("subsequently_root").toString());

                    outputObject.remove("subsequently_root");

                }

                if (outputObject.has("keywords_to_transfer")) {

                    this.jsonInputObject.put("keywords_to_transfer", outputObject.getJSONObject("keywords_to_transfer"));

                    outputObject.remove("keywords_to_transfer");

                }

                outputObject.remove("subsequently_workflow_action");

            }

            if (outputObject.has("subsequently_redirected")) {

                this.jsonInputObject.put("subsequently_redirected", outputObject.get("subsequently_redirected").toString());

                if (outputObject.has("redirect_to_hyperlink")) {

                    this.jsonInputObject.put("redirect_to_hyperlink", outputObject.get("redirect_to_hyperlink").toString());

                    outputObject.remove("redirect_to_hyperlink");

                }

                outputObject.remove("subsequently_redirected");

            }

            if (outputObject.has("use_in_known_subsequent_WA")) {

                this.jsonInputObject.put("use_in_known_subsequent_WA", outputObject.getJSONObject("use_in_known_subsequent_WA"));

                outputObject.remove("use_in_known_subsequent_WA");

            }

            return entryComponents.put(this.jsonInputObject.get("localID").toString(), outputObject);

        } else if (propertyToCheck.equals(SprO.triggersClickForEntryComponent.toString())) {

            RDFNode currObject = currStatement.getObject();

            SelectBuilder selectBuilder = new SelectBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            FilterBuilder filterBuilder = new FilterBuilder();

            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

            SelectBuilder innerSelect = new SelectBuilder();

            innerSelect.addWhere(currObject, RDF.type, "?o");

            selectBuilder.addVar(selectBuilder.makeVar("?o"));

            SPARQLFilter sparqlFilter = new SPARQLFilter();

            ArrayList<ArrayList<String>> filterItems = new ArrayList<>();

            filterItems = filterBuilder.addItems(filterItems, "?o", "<" + OWL2.NamedIndividual + ">");

            ArrayList<String> filter = sparqlFilter.getNotINFilter(filterItems);

            selectBuilder = filterBuilder.addFilter(selectBuilder, filter);

            selectBuilder.addGraph("?g", innerSelect);

            String sparqlQueryString = selectBuilder.buildString();

            String classID = connectionToTDB.pullSingleDataFromTDB(this.pathToOntologies, sparqlQueryString, "?o");

            JSONObject dummyJSONObject = checkInput(classID, connectionToTDB);

            if (dummyJSONObject.getJSONArray("data").getJSONObject(0).getJSONObject(this.jsonInputObject.get("localID").toString()).has("valid")) {
                // not in every case exist a valid information in the JSON structure

                if (!Boolean.valueOf(dummyJSONObject.getJSONArray("data").getJSONObject(0).getJSONObject(this.jsonInputObject.get("localID").toString()).get("valid").toString())) {

                    this.currInputIsValid = false;

                }

            }

            // entryComponents was already created in methods call "checkInput" above
            return dummyJSONObject.getJSONArray("data").getJSONObject(0);

        } else if (propertyToCheck.equals(SprO.triggersStatusTransitionCreateNewEntryFromOverlayBOOLEAN.toString())) {

            boolean notCreateNewEntry = true;

            if (currStatement.getObject().isLiteral()) {

                if (currStatement.getObject().asLiteral().getBoolean()) {
                    // true case

                    this.jsonInputObject.put("create_new_entry", "true");

                    notCreateNewEntry = false;

                }

            }

            if (notCreateNewEntry) {

                this.jsonInputObject.put("create_new_entry", "false");

            }

        }

        return entryComponents;

    }


}
