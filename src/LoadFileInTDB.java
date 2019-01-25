/*
 * Created by Roman Baum on 07.07.15.
 * Last modified by Roman Baum on 25.01.18.
 */

import soccomas.basic.ApplicationConfigurator;
import soccomas.packages.JenaIOTDBFactory;
import soccomas.vocabulary.PAV;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.tdb.base.block.FileMode;
import org.apache.jena.tdb.sys.SystemTDB;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LoadFileInTDB {

    private static void calculateSubGraphOWLFiles(String potentialResource, String pathToFile, Model directoryModel) throws IOException {

        if (!new File(pathToFile).exists()) {

            if (directoryModel.contains(ResourceFactory.createStatement(ResourceFactory.createResource(potentialResource), RDF.type, OWL2.Class))) {

                Model subModel = ModelFactory.createDefaultModel();

                JSONArray classToCheck = new JSONArray();

                classToCheck.put(potentialResource);

                while (!classToCheck.isNull(0)) {

                    SimpleSelector selector = new SimpleSelector(ResourceFactory.createResource(classToCheck.get(0).toString()), null, null, null);

                    StmtIterator directoryIter = directoryModel.listStatements(selector);

                    while (directoryIter.hasNext()) {

                        Statement stmt = directoryIter.nextStatement();

                        if (stmt.getPredicate().equals(RDF.type)
                                || stmt.getPredicate().equals(RDFS.label)
                                || stmt.getPredicate().equals(ResourceFactory.createProperty("http://purl.obolibrary.org/obo/IAO_0000115"))) {
                            // definition

                            if (stmt.getPredicate().equals(RDF.type)
                                    && (!stmt.getObject().equals(OWL2.Axiom)
                                    || !stmt.getObject().equals(OWL2.Restriction))) {

                                subModel.add(stmt);

                            } else {

                                subModel.add(stmt);

                            }

                        }

                    }

                    SimpleSelector childrenSelector = new SimpleSelector(null, RDFS.subClassOf, ResourceFactory.createResource(classToCheck.get(0).toString()));

                    StmtIterator childrenIter = directoryModel.listStatements(childrenSelector);

                    while (childrenIter.hasNext()) {

                        Statement childrenStmt = childrenIter.nextStatement();

                        classToCheck.put(childrenStmt.getSubject().toString());

                    }

                    classToCheck.remove(0);

                }

                File file = new File(pathToFile);

                subModel.write(new FileWriter(file), "RDF/XML");

            }

        } else {

            System.out.println("The file" + pathToFile + " already exist!");

        }

    }


    private static void calculateOWLFiles(String pathToFile, File currFile) throws IOException {

        System.out.println("File: " + currFile.getName() + " is a directory.");

        File innerFolder = new File(pathToFile + currFile.getName());

        // save all files of the folder in a list
        File[] innerListOfFiles = innerFolder.listFiles();

        Model directoryModel = ModelFactory.createDefaultModel();

        for (File currInnerFile : innerListOfFiles) {

            if (currInnerFile.isFile()
                    && ((FilenameUtils.getExtension(currInnerFile.getName()).equals("owl"))
                    || (FilenameUtils.getExtension(currInnerFile.getName()).equals("rdf"))
                    || (FilenameUtils.getExtension(currInnerFile.getName()).equals("xrdf")))) {

                // create the default model/graph
                Model tdb = ModelFactory.createDefaultModel();
                // load model to the Jena TDB
                TDBLoader.loadModel(tdb, currInnerFile.toString());

                directoryModel.add(tdb);

                System.out.println("The directory " + currFile + " contains the inner File: " + currInnerFile.getName());

            }

        }

        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/OBI_0000968", pathToFile + "SC_MDB_BASIC_0000000053.owl", directoryModel);
        // device
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/CHEBI_24431", pathToFile + "SC_MDB_BASIC_0000000056.owl", directoryModel);
        // chemical Entity
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/BFO_0000034", pathToFile + "SC_MDB_BASIC_0000000055.owl", directoryModel);
        // function
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/PATO_0000014", pathToFile + "SC_MDB_BASIC_0000000103.owl", directoryModel);
        // color
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/PATO_0000019", pathToFile + "SC_MDB_BASIC_0000000101.owl", directoryModel);
        // color pattern
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/PATO_0000025", pathToFile + "SC_MDB_BASIC_0000000008.owl", directoryModel);
        // composition
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/PATO_0000052", pathToFile + "SC_MDB_BASIC_0000000005.owl", directoryModel);
        // shape
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/PATO_0000060", pathToFile + "SC_MDB_BASIC_0000000047.owl", directoryModel);
        // spatial pattern
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/PATO_0000140", pathToFile + "SC_MDB_BASIC_0000000004.owl", directoryModel);
        // position
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/PATO_0000150", pathToFile + "SC_MDB_BASIC_0000000088.owl", directoryModel);
        // texture
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/PATO_0001018", pathToFile + "SC_MDB_BASIC_0000000003.owl", directoryModel);
        // physical quality
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/GO_0008150", pathToFile + "SC_MDB_BASIC_0000000013.owl", directoryModel);
        // biological process
        calculateSubGraphOWLFiles("http://purl.obolibrary.org/obo/UO_0000000", pathToFile + "SC_MDB_BASIC_0000000012.owl", directoryModel);
        // unit

        StmtIterator stmtIter = directoryModel.listStatements();

        List<Statement> stmtToDeleteList = new ArrayList<>();

        while (stmtIter.hasNext()) {

            Statement stmt = stmtIter.nextStatement();

            if (!stmt.getPredicate().equals(RDF.type)
                    && !stmt.getPredicate().equals(RDFS.label)
                    && !stmt.getPredicate().equals(ResourceFactory.createProperty("http://purl.obolibrary.org/obo/IAO_0000115"))) {
                // definition

                stmtToDeleteList.add(stmt);

            } else if (stmt.getPredicate().equals(RDF.type)
                    && (stmt.getObject().equals(OWL2.Axiom)
                    || stmt.getObject().equals(OWL2.Restriction))) {

                stmtToDeleteList.add(stmt);

            }

        }

        directoryModel.remove(stmtToDeleteList);

        // create the outputStream
        directoryModel.write(new FileWriter(currFile.toString() + ".owl"), "RDF/XML");

    }


    public static void main(String[] args) throws IOException, ParseException {

        // hide the first 3 rows
        LogCtl.setCmdLogging();

        //reduce the size of the TDB
        TDB.getContext().set(SystemTDB.symFileMode, FileMode.direct);

        String pathToOntologies  = ApplicationConfigurator.getPathToApplicationOntologyStore();

        // maybe change path to ontologies
        String pathToApplicationRDFFilesInputFolder = "../application-ontologies/";

        // get input folder by path
        File inputFolder = new File(pathToApplicationRDFFilesInputFolder);

        // save all files of the folder in a list
        File[] listOfFiles = inputFolder.listFiles();


        // set date format to: dd.MM.yyyy hh:mm:ss
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

        // create a buffer reader to process the TDBLastUpdatedOn file
        BufferedReader brLastUpdatedOn = new BufferedReader(new FileReader(pathToApplicationRDFFilesInputFolder + "TDBLastUpdatedOn"));

        // read the first line of the file (get the last updated on date)
        String firstLineUpdatedOn = brLastUpdatedOn.readLine();

        // transform the last updated on date from String to Date
        Date lastUpdatedOn = df.parse(firstLineUpdatedOn);

        // create a date instance for the potential new update date
        Date newUpdateDate = lastUpdatedOn;

        ArrayList<String> modelNameArList = new ArrayList<>();

        ArrayList<Model> addedModelArList = new ArrayList<>();

        // iterate the file list
        assert listOfFiles != null;

        for (File listOfFile : listOfFiles) {

            if (listOfFile.isFile() & (FilenameUtils.getExtension(listOfFile.getName()).equals("owl"))) {

                Date currDate = df.parse(df.format(listOfFile.lastModified()));

                if (currDate.compareTo(lastUpdatedOn) > 0) {

                    if (currDate.compareTo(newUpdateDate) > 0) {

                        newUpdateDate = currDate;
                    }

                    String basicNG = "http://www.soccomas.org/Ontologies/SOCCOMAS/SCMDBBasic/";

                    if (listOfFile.getName().contains("0v1")) {

                        String currNG = FilenameUtils.getBaseName(listOfFile.getName());

                        basicNG += currNG.replace("0v1", "");

                    } else {

                        basicNG += FilenameUtils.getBaseName(listOfFile.getName());

                    }


                    modelNameArList.add(basicNG);


                    Model currModel = ModelFactory.createDefaultModel();

                    // load model to the Jena TDB

                    TDBLoader.loadModel(currModel, pathToApplicationRDFFilesInputFolder + listOfFile.getName());

                    addedModelArList.add(currModel);

                    System.out.println("basicNG " + basicNG);

                }

            }

        }

        System.out.println("last updated on " + df.format(lastUpdatedOn));

        System.out.println("modelNameArList " + modelNameArList.size());
        System.out.println("addedModelArList " + addedModelArList.size());

        System.out.println("pathToOntologies " + pathToOntologies);

        if (newUpdateDate.compareTo(lastUpdatedOn) > 0) {

            JenaIOTDBFactory mdbFactory = new JenaIOTDBFactory();

            mdbFactory.removeNamedModelsFromTDB(pathToOntologies, modelNameArList);

            System.out.println("addedmodels " + mdbFactory.addModelsInTDB(pathToOntologies, modelNameArList, addedModelArList));

            PrintWriter writer = new PrintWriter(pathToApplicationRDFFilesInputFolder + "TDBLastUpdatedOn", "UTF-8");
            writer.println(df.format(newUpdateDate));
            writer.close();



        }

        String pathToExternalOntologiesInputFolder = "../application-ontologies/external-ontologies/";

        // get input folder by path
        File inputFolderExternalOntologies = new File(pathToExternalOntologiesInputFolder);

        // save all files of the folder in a list
        File[] listOfFilesExternalOntologies = inputFolderExternalOntologies.listFiles();

        // create a buffer reader to process the TDBLastUpdatedOn file
        BufferedReader brExternalOntologiesLastUpdatedOn = new BufferedReader
                (new FileReader(pathToExternalOntologiesInputFolder + "TDBLastUpdatedOn"));

        // read the first line of the file (get the last updated on date)
        String firstLineExternalOntologiesUpdatedOn = brExternalOntologiesLastUpdatedOn.readLine();

        // transform the last updated on date from String to Date
        Date externalOntologiesLastUpdatedOn = df.parse(firstLineExternalOntologiesUpdatedOn);

        // create a date instance for the potential new update date
        newUpdateDate = externalOntologiesLastUpdatedOn;

        ArrayList<String> modelNameExternalOntologiesArList = new ArrayList<>();

        ArrayList<Model> addedModelExternalOntologiesArList = new ArrayList<>();

        // iterate the file list
        assert listOfFilesExternalOntologies != null;

        for (File currFile : listOfFilesExternalOntologies) {

            if (currFile.isDirectory()) {

                if (new File(pathToExternalOntologiesInputFolder, currFile.getName() + ".owl").exists()) {

                    Date currDate = df.parse(df.format(new File(pathToExternalOntologiesInputFolder, currFile.getName() + ".owl").lastModified()));

                    if (currDate.compareTo(externalOntologiesLastUpdatedOn) > 0) {

                        calculateOWLFiles(pathToExternalOntologiesInputFolder, currFile);

                    }

                } else {

                    calculateOWLFiles(pathToExternalOntologiesInputFolder, currFile);

                }

            }

        }

        listOfFilesExternalOntologies = inputFolderExternalOntologies.listFiles();

        String basicPathToLucene = ApplicationConfigurator.getPathToLuceneStore();

        ArrayList<String> basicPathsToLucene = new ArrayList<>();

        for (File listOfFile : listOfFilesExternalOntologies) {

            if (listOfFile.isFile() & (FilenameUtils.getExtension(listOfFile.getName()).equals("owl"))) {

                Date currDate = df.parse(df.format(listOfFile.lastModified()));

                if (currDate.compareTo(externalOntologiesLastUpdatedOn) > 0) {

                    if (currDate.compareTo(newUpdateDate) > 0) {

                        newUpdateDate = currDate;
                    }

                    String basicNG = "http://www.soccomas.org/Ontologies/SOCCOMAS/";

                    if (listOfFile.getName().contains("0v1")) {

                        String currNG = FilenameUtils.getBaseName(listOfFile.getName());

                        basicNG += currNG.replace("0v1", "");

                        basicPathsToLucene.add(basicPathToLucene + FilenameUtils.removeExtension(currNG.replace("0v1", "")));

                    } else {

                        basicNG += FilenameUtils.getBaseName(listOfFile.getName());

                        basicPathsToLucene.add(basicPathToLucene + FilenameUtils.removeExtension(listOfFile.getName()));

                    }


                    modelNameExternalOntologiesArList.add(basicNG);

                    Model currModel = ModelFactory.createDefaultModel();

                    TDBLoader.loadModel(currModel, pathToExternalOntologiesInputFolder + listOfFile.getName());

                    addedModelExternalOntologiesArList.add(currModel);

                    System.out.println("basicNG " + basicNG);

                }

            }

        }

        System.out.println("external ontologies last updated on " + df.format(externalOntologiesLastUpdatedOn));

        System.out.println("modelNameExternalOntologiesArList " + modelNameExternalOntologiesArList.size());
        System.out.println("addedModelExternalOntologiesArList " + addedModelExternalOntologiesArList.size());

        pathToOntologies = ApplicationConfigurator.getPathToJenaStore() + "external-ontologies/";

        System.out.println("pathToOntologies " + pathToOntologies);

        if (newUpdateDate.compareTo(externalOntologiesLastUpdatedOn) > 0) {

            JenaIOTDBFactory mdbFactory = new JenaIOTDBFactory();

            basicPathToLucene = ApplicationConfigurator.getPathToLuceneStore() +"external-ontologies/";

            mdbFactory.addModelsInTDBLucene(pathToOntologies, basicPathToLucene, modelNameExternalOntologiesArList, addedModelExternalOntologiesArList);

            PrintWriter writer = new PrintWriter(pathToExternalOntologiesInputFolder + "TDBLastUpdatedOn", "UTF-8");
            writer.println(df.format(newUpdateDate));
            writer.close();

        }

        JenaIOTDBFactory jenaIOTDBFactory = new JenaIOTDBFactory();

        // create sub query structure

        SelectBuilder selectWhereBuilder = new SelectBuilder();

        selectWhereBuilder.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        selectWhereBuilder.addPrefix("owl", "http://www.w3.org/2002/07/owl#");

        selectWhereBuilder.addWhere(PAV.createdOn, RDF.type, OWL2.DatatypeProperty);

        // create main query structure

        AskBuilder askBuilder = new AskBuilder();

        askBuilder.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        askBuilder.addPrefix("owl", "http://www.w3.org/2002/07/owl#");

        askBuilder.addGraph("?g", selectWhereBuilder);

        // create a Query
        Query sparqlQuery = QueryFactory.create(askBuilder.buildString());

        System.out.println(jenaIOTDBFactory.pullStringDataFromTDB(pathToOntologies, sparqlQuery, "RDF/XML-ABBREV"));

        System.out.println("newUpdateDate " + df.format(newUpdateDate));

    }

}
