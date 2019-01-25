/*
 * Created by Roman Baum on 29.04.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages;

import soccomas.basic.ApplicationConfigurator;
import soccomas.basic.SOCCOMASDate;
import soccomas.basic.SOCCOMASURLEncoder;
import soccomas.basic.TDBPath;
import soccomas.packages.querybuilder.FilterBuilder;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.packages.querybuilder.SPARQLFilter;
import soccomas.vocabulary.SCBasic;
import soccomas.vocabulary.SprO;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * The Class MDBResourceFactory provides methods to create new MDBCoreIDs or MDBEntryIDs.
 */
public class SOCCOMASResourceFactory {

    private String coreIDDomain = ApplicationConfigurator.getDomain();

    private String coreIDPath = "/resource";

    private SOCCOMASDate soccomasDat = new SOCCOMASDate();

    private String mdbUEID;

    /**
     * This method checks if a statement (MDB data entry type, has abbreviation, abbreviation) in the jena tdb exist or
     * not.
     * @param potentialDataEntryType a uri which is a potential "MDB data entry" individual
     * @param pathToTDB the path to the tdb directory
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return "true" if statement exist else "false"
     */
    private boolean findAbbreviation (String potentialDataEntryType, String pathToTDB, JenaIOTDBFactory connectionToTDB) {

        UrlValidator annotationValidator = new UrlValidator();

        // get a MDB url Encoder to encode the uri with utf-8
        SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

        if (annotationValidator
                .isValid(soccomasURLEncoder.encodeUrl(potentialDataEntryType, "UTF-8")) ||
                ResourceFactory.createResource(potentialDataEntryType).isAnon()) {

            SelectBuilder selectWhereBuilder = new SelectBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

            selectWhereBuilder.addWhere("<" + potentialDataEntryType + ">", SprO.hasAbbreviation,"?o");

            AskBuilder askBuilder = new AskBuilder();

            askBuilder = prefixesBuilder.addPrefixes(askBuilder);

            askBuilder.addGraph("?g", selectWhereBuilder);

            String sparqlQueryString = askBuilder.buildString();

            return connectionToTDB.statementExistInTDB(pathToTDB, sparqlQueryString);

        } else {

            return false;

        }

    }


    /**
     * This method gets the abbreviation of a MDB data entry type from the jena tdb
     * @param dataEntryType contains the uri of a "MDB data entry type" individual
     * @param pathToTDB the path to the tdb directory
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an abbreviation for a "MDB data entry type"
     */
    private String getAbbreviation (String dataEntryType, String pathToTDB, JenaIOTDBFactory connectionToTDB) {

        SelectBuilder selectBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        SelectBuilder innerSelect = new SelectBuilder();

        innerSelect.addWhere("<" + dataEntryType + ">", SprO.hasAbbreviation, "?o");

        selectBuilder.addVar(selectBuilder.makeVar("?o"));

        selectBuilder.addGraph("?g", innerSelect);

        String sparqlQueryString = selectBuilder.buildString();

        return (connectionToTDB.pullSingleDataFromTDB(pathToTDB, sparqlQueryString, "?o")).toLowerCase();

    }


    /**
     * This methods gets the value of the property 'next version counter' from the jena tdb.
     * @param coreID contains the URI of a coreID
     * @param pathToTDB contains the path to the jena tdb
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return value of the property 'next version counter'
     */
    private int getNextVersionCounter(String coreID, String pathToTDB, JenaIOTDBFactory connectionToTDB) {

        String versionAndProvenanceNG = coreID + "-d_1_1#" + SCBasic.versionsAndProvenanceNamedGraph.getLocalName() + "_1";

        SelectBuilder selectWhereBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

        selectWhereBuilder.addWhere("<" + coreID + ">", SCBasic.nextVersionCounter,"?o");

        SelectBuilder selectBuilder = new SelectBuilder();

        selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

        ExprVar exprVar = new ExprVar("o");

        selectBuilder.addVar(exprVar);

        selectBuilder.addGraph("<" + versionAndProvenanceNG + ">", selectWhereBuilder);

        String sparqlQueryString = selectBuilder.buildString();

        return Integer.parseInt(connectionToTDB.pullSingleDataFromTDB(pathToTDB, sparqlQueryString, "?o"));


    }


    /**
     * This method creates a new draft MDBEntryID if a new version must be created.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param oldMDBEntryID contains an old MDBEntryID
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a new created MDBEntryID
     */
    public String createNewDraftVersionForMDBEntryID(JSONArray currExecStep, String oldMDBEntryID, JenaIOTDBFactory connectionToTDB) {

        String pathToTDB = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                TDBPath tdbPath = new TDBPath();

                pathToTDB = tdbPath.getPathToTDB(currExecStep.getJSONObject(i).get("object").toString());

            }

        }

        String coreID = oldMDBEntryID.substring(0, oldMDBEntryID.lastIndexOf("-"));

        String MDBEntryIDWithoutVersionCounter = oldMDBEntryID.substring(0, oldMDBEntryID.lastIndexOf("_"));

        return MDBEntryIDWithoutVersionCounter + "_" + String.valueOf(getNextVersionCounter(coreID, pathToTDB, connectionToTDB));

    }


    /**
     * This method creates a new MDBCoreID.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param infoInput a URI to find a single character in the ontology specifying the type of entry (s=Specimen,
     *                  t=Taxon, m=Media, l=Literature, d=Description, mx=Matrix, td=Taxonomic Description).
     * @param jsonInputObject contains a eight characters long hexadecimal to identify the corresponding user
     * @param pathToOntologies contains the path to the ontology tdb
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a new created MDBCoreID
     */
    public String createMDBCoreID(JSONArray currExecStep, JSONObject infoInput, JSONObject jsonInputObject, String pathToOntologies, JenaIOTDBFactory connectionToTDB) {

        String mdbType = "";

        boolean abbreviationExist = false;

        Iterator<String> infoInputKeys = infoInput.keys();

        while (infoInputKeys.hasNext() || abbreviationExist) {

            String currKey = infoInputKeys.next();

            String potentialResource = infoInput.get(currKey).toString();

            SOCCOMASURLEncoder mdbLEncoderSomeValue = new SOCCOMASURLEncoder();

            UrlValidator urlValidatorSomeValue = new UrlValidator();

            if (urlValidatorSomeValue.isValid(mdbLEncoderSomeValue.encodeUrl(potentialResource, "UTF-8"))) {

                abbreviationExist = findAbbreviation(potentialResource, pathToOntologies, connectionToTDB);

                if (abbreviationExist) {

                    mdbType = getAbbreviation(potentialResource, pathToOntologies, connectionToTDB);

                }

            }

        }

        String pathToTDB = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                TDBPath tdbPath = new TDBPath();

                pathToTDB = tdbPath.getPathToTDB(currExecStep.getJSONObject(i).get("object").toString());

            }

        }

        String MDBCoreIDWithoutSerialNo = this.coreIDDomain + this.coreIDPath + "/"
                + jsonInputObject.get("mdbueid").toString() + "-" + this.soccomasDat.getDateForURI() + "-" + mdbType + "-";

        SelectBuilder selectWhereBuilder = new SelectBuilder();

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

        selectWhereBuilder.addWhere("?s", RDF.type, SCBasic.coreID);
        // MDB core ID

        FilterBuilder filterBuilder = new FilterBuilder();

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        // create an array list to collect the filter parts
        ArrayList<String> filterCollection= new ArrayList<>();

        // add a part to the collection
        filterCollection.add(MDBCoreIDWithoutSerialNo);

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

        System.out.println("core id = " + sparqlQueryString);

        return MDBCoreIDWithoutSerialNo + String.valueOf(Integer.parseInt(connectionToTDB.pullSingleDataFromTDB(pathToTDB, sparqlQueryString, "?count")) + 1);

    }


    /**
     * This method creates a new MDBEntryID.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param mdbCoreID contains a MDBCoreID for the creation of a MDBEntryID
     * @param versionType contains a single character (p=publish, d=draft, r=revision draft)
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a new created MDBEntryID
     */
    public String createMDBEntryID (JSONArray currExecStep, String mdbCoreID, char versionType, JenaIOTDBFactory connectionToTDB) {

        String MDBEntryIDWithoutVersionNo = mdbCoreID + "-" + versionType;

        String pathToTDB = "";

        for (int i = 0; i < currExecStep.length(); i++) {

            if (currExecStep.getJSONObject(i).get("property").toString().equals(SprO.namedGraphBelongsToWorkspace.toString())) {

                TDBPath tdbPath = new TDBPath();

                pathToTDB = tdbPath.getPathToTDB(currExecStep.getJSONObject(i).get("object").toString());

            }

        }

        if (versionType != 'p') {

            SelectBuilder selectWhereBuilder = new SelectBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

            selectWhereBuilder.addWhere("?s", RDF.type, SCBasic.entryID);

            FilterBuilder filterBuilder = new FilterBuilder();

            SPARQLFilter sparqlFilter = new SPARQLFilter();

            // create an array list to collect the filter parts
            ArrayList<String> filterCollection= new ArrayList<>();

            // add a part to the collection
            filterCollection.add(MDBEntryIDWithoutVersionNo);

            // generate a filter string
            ArrayList<String> filter = sparqlFilter.getRegexSTRFilter("?s", filterCollection);

            selectWhereBuilder = filterBuilder.addFilter(selectWhereBuilder, filter);

            SelectBuilder selectBuilder = new SelectBuilder();

            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

            selectBuilder.addVar("?s");

            selectBuilder.addGraph("?g", selectWhereBuilder);

            String sparqlQueryString = selectBuilder.buildString();

            JSONArray versionsFromTDB = connectionToTDB.pullMultipleDataFromTDB(pathToTDB, sparqlQueryString, "?s");

            int maxFirstVersionNumber = 0;

            for (int i = 0; i < versionsFromTDB.length(); i++) {

                String currFirstVersionNo = versionsFromTDB.get(i).toString().substring(versionsFromTDB.get(i).toString().lastIndexOf("-"));

                currFirstVersionNo = currFirstVersionNo.substring(currFirstVersionNo.indexOf("_") + 1, currFirstVersionNo.lastIndexOf("_"));

                if (maxFirstVersionNumber < Integer.parseInt(currFirstVersionNo)) {

                    maxFirstVersionNumber = Integer.parseInt(currFirstVersionNo);

                }

            }

            int nextVersionCounter = getNextVersionCounter(mdbCoreID, pathToTDB, connectionToTDB);

            if (nextVersionCounter > 1) {
                // there existed already deleted version in the past

                MDBEntryIDWithoutVersionNo = MDBEntryIDWithoutVersionNo + "_" + String.valueOf(maxFirstVersionNumber);

                return MDBEntryIDWithoutVersionNo + "_" + String.valueOf(nextVersionCounter);

            } else {

                MDBEntryIDWithoutVersionNo = MDBEntryIDWithoutVersionNo + "_" + String.valueOf(maxFirstVersionNumber + 1);

                return MDBEntryIDWithoutVersionNo + "_1";

            }

        } else {

            SelectBuilder selectWhereBuilder = new SelectBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

            selectWhereBuilder.addWhere("?s", RDF.type, SCBasic.entryID);

            FilterBuilder filterBuilder = new FilterBuilder();

            SPARQLFilter sparqlFilter = new SPARQLFilter();

            // create an array list to collect the filter parts
            ArrayList<String> filterCollection= new ArrayList<>();

            // add a part to the collection
            filterCollection.add(MDBEntryIDWithoutVersionNo);

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

            return MDBEntryIDWithoutVersionNo + "_" + String.valueOf(Integer.parseInt(connectionToTDB.pullSingleDataFromTDB(pathToTDB, sparqlQueryString, "?count")) + 1);

        }

    }


    /**
     * This method creates a new MDBUserEntryID(mdbUEID).
     * @param mdbUEIDLocal contains a eight characters long hexadecimal String to create a MDBUserEntryID
     * @return a new created MDBUserEntryID
     */
    public String createMDBUserEntryID(String mdbUEIDLocal) {

        this.mdbUEID = this.coreIDDomain + this.coreIDPath + "/" + mdbUEIDLocal;

        return this.mdbUEID;

    }


    /**
     * This method identifies the corresponding MDBUserEntryID from the store or provide the already identified MDBUEID.
     * @param mdbueidHex contains a hex code to find the MDBUserEntryID in the jena tdb
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return a corresponding MDBUserEntryID
     */
    public String findMDBUserEntryID(String mdbueidHex, JenaIOTDBFactory connectionToTDB) {

        if (this.mdbUEID != null) {

            return this.mdbUEID;

        } else {

            SelectBuilder selectWhereBuilder = new SelectBuilder();

            PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

            selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

            selectWhereBuilder.addWhere("?s", RDF.type, SCBasic.userEntryID);

            FilterBuilder filterBuilder = new FilterBuilder();

            SPARQLFilter sparqlFilter = new SPARQLFilter();

            // create an array list to collect the filter parts
            ArrayList<String> filterCollection = new ArrayList<>();

            // add a part to the collection
            filterCollection.add(mdbueidHex);

            // generate a filter string
            ArrayList<String> filter = sparqlFilter.getRegexSTRFilter("?s", filterCollection);

            selectWhereBuilder = filterBuilder.addFilter(selectWhereBuilder, filter);

            SelectBuilder selectBuilder = new SelectBuilder();

            selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

            selectBuilder.addVar("?s");

            selectBuilder.addGraph("?g", selectWhereBuilder);

            String sparqlQueryString = selectBuilder.buildString();

            System.out.println("ueid = " + sparqlQueryString);

            TDBPath tdbPath = new TDBPath();

            this.mdbUEID = connectionToTDB.pullSingleDataFromTDB(tdbPath.getPathToTDB(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString()), sparqlQueryString, "?s");

            return this.mdbUEID;

        }

    }

}
