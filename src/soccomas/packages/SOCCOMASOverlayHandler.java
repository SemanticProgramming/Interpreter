/*
 * Created by Roman Baum on 27.04.17.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages;

import soccomas.basic.ApplicationConfigurator;
import soccomas.basic.SOCCOMASDate;
import soccomas.basic.TDBPath;
import soccomas.mongodb.MongoDBConnection;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.vocabulary.SCBasic;
import soccomas.vocabulary.SprO;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class SOCCOMASOverlayHandler {

    private String overlayOverviewNGURI = ApplicationConfigurator.getDomain() + "/resource/dummy-overlay#"
            + SCBasic.guiCOMPONENTBASICWIDGETSpecifyRequiredInformation.getLocalName();
    private String pathToTDB;
    private MongoDBConnection mongoDBConnection;

    /**
     * Default constructor
     */
    public SOCCOMASOverlayHandler(MongoDBConnection mongoDBConnection) {

        this.mongoDBConnection = mongoDBConnection;
        this.pathToTDB = getPath();

    }

    private String getPath() {

        TDBPath tdbPath = new TDBPath();

        return tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString());

    }

    /**
     * This method create a named graph for an overlay in a jena tdb.
     * @param overlayNGURI contains the URI of a named graph for the overlay
     * @param overlayModel contains the data of the overlay named graph
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void create(String overlayNGURI, Model overlayModel, JenaIOTDBFactory connectionToTDB) {

        SOCCOMASDate soccomasDat = new SOCCOMASDate();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        // count value in jena tdb
        SelectBuilder selectWhereBuilder = new SelectBuilder();

        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

        selectWhereBuilder.addWhere("<" + this.overlayOverviewNGURI + ">", RDF.value, "?o");

        SelectBuilder selectBuilder = new SelectBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        selectBuilder.addVar(selectBuilder.makeVar("?o"));

        selectBuilder.addGraph("<" + this.overlayOverviewNGURI + ">", selectWhereBuilder);

        String sparqlQueryString = selectBuilder.buildString();

        String result = connectionToTDB.pullSingleDataFromTDB(this.pathToTDB, sparqlQueryString, "?o");

        String newResourceIndexString;

        if (result.isEmpty()) {

            newResourceIndexString = "1";

        } else {

            int newResourceIndex = Integer.parseInt(result) + 1;

            newResourceIndexString = String.valueOf(newResourceIndex);

        }

        Model overlayOverviewModel = connectionToTDB.pullNamedModelFromTDB(this.pathToTDB, this.overlayOverviewNGURI);

        if (overlayOverviewModel
                .contains(ResourceFactory.createResource(this.overlayOverviewNGURI),
                        RDF.value)) {

            overlayOverviewModel.remove(
                    ResourceFactory.createResource(this.overlayOverviewNGURI),
                    RDF.value,
                    ResourceFactory.createPlainLiteral(result)
            );

        }

        overlayOverviewModel.add(
                ResourceFactory.createResource(this.overlayOverviewNGURI),
                RDF.value,
                ResourceFactory.createPlainLiteral(newResourceIndexString));

        overlayOverviewModel.add(
                ResourceFactory.createResource(this.overlayOverviewNGURI),
                SprO.hasAssociatedNamedGraph,
                ResourceFactory.createResource(overlayNGURI));

        overlayOverviewModel.add(
                ResourceFactory.createResource(overlayNGURI),
                ResourceFactory.createProperty("http://www.w3.org/2006/time#inDateTime"),
                ResourceFactory.createPlainLiteral(soccomasDat.getTimeInMillis()));

        ArrayList<String> modelNameArList = new ArrayList<>();

        ArrayList<Model> addedModelArList = new ArrayList<>();

        modelNameArList.add(this.overlayOverviewNGURI);
        addedModelArList.add(overlayOverviewModel);

        modelNameArList.add(overlayNGURI);
        addedModelArList.add(overlayModel);

        connectionToTDB.removeNamedModelFromTDB(this.pathToTDB, this.overlayOverviewNGURI);

        System.out.println(connectionToTDB.addModelsInTDB(this.pathToTDB, modelNameArList, addedModelArList));

    }

    /**
     * This method removes deprecated overlay data from the jena tdb and the mongoDB.
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void removeDeprecatedOverlays(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        SOCCOMASDate soccomasDat = new SOCCOMASDate();

        Model overlayOverviewModel = connectionToTDB.pullNamedModelFromTDB(this.pathToTDB, this.overlayOverviewNGURI);

        String currDate = soccomasDat.getTimeInMillis();

        // current date minus a half hour
        long borderDateMS = Long.parseLong(currDate) - 1800000;

        StmtIterator stmtIter = overlayOverviewModel.listStatements();

        while (stmtIter.hasNext()) {

            Statement currStmt = stmtIter.next();

            Resource currSubject = currStmt.getSubject();

            Property currProperty = currStmt.getPredicate();

            if ((!(currSubject.toString().equals(this.overlayOverviewNGURI)))
                    && currProperty.toString().equals("http://www.w3.org/2006/time#inDateTime")) {

                RDFNode currObject = currStmt.getObject();

                long dateToCheck = Long.parseLong(currObject.toString());

                if (dateToCheck < borderDateMS) {

                    System.out.println("Delete this statement " + currStmt + "from the store.");

                    String mongoDBKey = currSubject.toString();

                    try {

                        URL url = new URL(mongoDBKey);

                        mongoDBKey = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

                    } catch (MalformedURLException e) {

                        e.printStackTrace();

                    }

                    System.out.println("mongoDBKey = " + mongoDBKey);

                    if (this.mongoDBConnection.documentExist("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey)) {

                        String objectID = this.mongoDBConnection.findObjectID("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey);

                        this.mongoDBConnection.removeDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), objectID, mongoDBKey);

                    }

                    if (this.mongoDBConnection.collectionExist("mdb-prototyp", mongoDBKey)) {

                        this.mongoDBConnection.dropCollection("mdb-prototyp", mongoDBKey);

                    }

                    if (connectionToTDB.modelExistInTDB(this.pathToTDB, currSubject.toString())) {

                        connectionToTDB.removeNamedModelFromTDB(this.pathToTDB, currSubject.toString());

                        Model stmtsToRemoveModel = ModelFactory.createDefaultModel();

                        stmtsToRemoveModel.add(ResourceFactory.createResource(this.overlayOverviewNGURI), SprO.hasAssociatedNamedGraph, currSubject);

                        stmtsToRemoveModel.add(currStmt);

                        connectionToTDB.removeModelDataInTDB(this.pathToTDB, this.overlayOverviewNGURI, stmtsToRemoveModel);

                    }

                }

            }

        }

    }

    /**
     * This method removes deprecated overlay data from the jena tdb and the mongoDB.
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void removeOverlay(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB) {

        Model overlayOverviewModel = connectionToTDB.pullNamedModelFromTDB(this.pathToTDB, this.overlayOverviewNGURI);

        StmtIterator stmtIter = overlayOverviewModel.listStatements();

        while (stmtIter.hasNext()) {

            Statement currStmt = stmtIter.next();

            Resource currSubject = currStmt.getSubject();

            Property currProperty = currStmt.getPredicate();

            Resource currObject;

            if (currStmt.getObject().isResource()) {

                currObject = currStmt.getObject().asResource();

                if (currSubject.toString().equals(this.overlayOverviewNGURI)
                        && currProperty.equals(SprO.hasAssociatedNamedGraph)
                        && currObject.toString().contains(jsonInputObject.get("html_form").toString())) {

                    if (connectionToTDB.modelExistInTDB(this.pathToTDB, currObject.toString())) {

                        connectionToTDB.removeNamedModelFromTDB(this.pathToTDB, currObject.toString());

                        Model stmtsToRemoveModel = ModelFactory.createDefaultModel();

                        stmtsToRemoveModel.add(currStmt);

                        StmtIterator dateStmtIter = overlayOverviewModel.listStatements(new SimpleSelector(currObject, ResourceFactory.createProperty("http://www.w3.org/2006/time#inDateTime"), null, ""));

                        while (dateStmtIter.hasNext()) {

                            Statement currDateStmt = dateStmtIter.next();

                            stmtsToRemoveModel.add(currDateStmt);

                        }

                        connectionToTDB.removeModelDataInTDB(this.pathToTDB, this.overlayOverviewNGURI, stmtsToRemoveModel);

                        String mongoDBKey = currObject.toString();

                        try {

                            URL url = new URL(mongoDBKey);

                            mongoDBKey = url.getPath().substring(1, url.getPath().length()) + "#" + url.getRef();

                        } catch (MalformedURLException e) {

                            e.printStackTrace();

                        }

                        System.out.println("mongoDBKey = " + mongoDBKey);

                        if (this.mongoDBConnection.documentExist("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey)) {

                            String objectID = this.mongoDBConnection.findObjectID("mdb-prototyp", jsonInputObject.get("connectSID").toString(), mongoDBKey);

                            this.mongoDBConnection.removeDataToMongoDB("mdb-prototyp", jsonInputObject.get("connectSID").toString(), objectID, mongoDBKey);

                        }

                        if (this.mongoDBConnection.collectionExist("mdb-prototyp", mongoDBKey)) {

                            this.mongoDBConnection.dropCollection("mdb-prototyp", mongoDBKey);

                        }

                    }

                }

            }

        }


    }

    /**
     * This method updates the timestamp for an overlay.
     * @param jsonInputObject contains the information for the calculation
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    public void updateTimeStamp(JSONObject jsonInputObject, JenaIOTDBFactory connectionToTDB){

        Model overlayOverviewModel = connectionToTDB.pullNamedModelFromTDB(this.pathToTDB, this.overlayOverviewNGURI);

        StmtIterator stmtIter = overlayOverviewModel.listStatements();

        while (stmtIter.hasNext()) {

            Statement currStmt = stmtIter.next();

            Resource currSubject = currStmt.getSubject();

            Property currProperty = currStmt.getPredicate();

            if (currSubject.toString().contains(jsonInputObject.get("html_form").toString())
                    && currProperty.toString().equals("http://www.w3.org/2006/time#inDateTime")) {

                if (connectionToTDB.modelExistInTDB(this.pathToTDB, currSubject.toString())) {

                    Model stmtsToRemoveModel = ModelFactory.createDefaultModel();

                    stmtsToRemoveModel.add(currStmt);

                    Model newStmtsModel = ModelFactory.createDefaultModel();

                    SOCCOMASDate soccomasDate = new SOCCOMASDate();

                    newStmtsModel.add(currSubject, currProperty, ResourceFactory.createPlainLiteral(soccomasDate.getTimeInMillis()));

                    connectionToTDB.updateModelDataInTDB(this.pathToTDB, this.overlayOverviewNGURI, stmtsToRemoveModel, newStmtsModel);

                }

            }

        }

    }

}
