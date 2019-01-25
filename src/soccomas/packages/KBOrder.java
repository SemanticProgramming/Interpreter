/*
 * Created by Roman Baum on 04.08.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages;

import soccomas.basic.ExecutionStep;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.vocabulary.SprO;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

/**
 * This class sorted an input knowledge base and provide some methods to use this sorted knowledge base(KB).
 */
public class KBOrder {

    // reorder the data and substitute the key
    private JSONArray sortedKBJSONArray = new JSONArray();

    // provide a connection between the indices with kb format and an indices with natural numbers as data format
    private JSONArray sortedKBIndicesJSONArray = new JSONArray();

    // create a new JSON object for the triple
    private JSONObject jsonDataObjects = new JSONObject();

    private ArrayList<ExecutionStep> ExecutionSteps = new ArrayList<>();

    /**
     * construct a instance of the class and initialize the private variables.
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @param pathToOntologyTDB describe the path to the ontology tdb, which contains the knowledge base
     * @param mdbStatusTransitionString contains the local identifier of the knowledge base
     */
    public KBOrder(JenaIOTDBFactory connectionToTDB, String pathToOntologyTDB, String mdbStatusTransitionString) {

        createKBOrder(connectionToTDB, pathToOntologyTDB,  mdbStatusTransitionString);
    }


    /**
     * provide a connection between the indices with kb format and an indices with natural numbers as data format
     * @return an ordered array list with indices which provide a connection between the corresponding kb data format &
     * a natural number format
     */
    public JSONArray getSortedKBIndicesJSONArray () {
        return this.sortedKBIndicesJSONArray;
    }

    /**
     * provide a sorted array list for the class
     * @return an ordered array list
     */
    public JSONArray getSortedKBJSONArray() {
        return this.sortedKBJSONArray;
    }


    /**
     * This method initialize the private variables and sort the knowledge base.
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @param pathToOntologyTDB describe the path to the ontology tdb, which contains the knowledge base
     * @param mdbStatusTransitionString contains the local identifier of the knowledge base
     */
    private void createKBOrder(JenaIOTDBFactory connectionToTDB, String pathToOntologyTDB, String mdbStatusTransitionString) {

        // find root element to create a KB Order
        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        ConstructBuilder constructBuilder = new ConstructBuilder();

        constructBuilder = prefixesBuilder.addPrefixes(constructBuilder);

        constructBuilder.addConstruct("?s", "?p", "?o");

        SelectBuilder tripleSPO = new SelectBuilder();

        tripleSPO.addWhere("?s", "?p", "?o");

        UrlValidator urlValidator = new UrlValidator();

        if (urlValidator.isValid(mdbStatusTransitionString)) {

            tripleSPO.addWhere("?s", OWL2.annotatedSource, ResourceFactory.createResource(mdbStatusTransitionString));

        } else {

            tripleSPO.addWhere("?s", OWL2.annotatedSource, mdbStatusTransitionString);

        }

        constructBuilder.addGraph("?g", tripleSPO);

        String sparqlQueryString = constructBuilder.buildString();

        Model constructResult = connectionToTDB.pullDataFromTDB(pathToOntologyTDB, sparqlQueryString);

        StmtIterator resultIterator = constructResult.listStatements();

        while (resultIterator.hasNext()) {

            Statement currStmt = resultIterator.nextStatement();

            Property currProperty = currStmt.getPredicate();

            // ignore uninterested triples like developer comment, the type of the axiom and the annotated source
            if ((!currProperty.equals(RDF.type))
                    & (!currProperty.equals(OWL2.annotatedSource))
                    & (!currProperty.equals(SprO.developmentComment))) {

                String currSubject = currStmt.getSubject().toString();
                String currObject = currStmt.getObject().toString();

                if ((currProperty.equals(OWL2.annotatedTarget))
                        & (currStmt.getObject().isLiteral())) {

                    currObject = currStmt.getObject().asLiteral().getLexicalForm();

                    // remove all non digits to become the digits of the execution step (1, 2, 3, ...)
                    String currObjectNumber = currObject.replaceAll("[\\D]", "");

                    // add a zero digit before the first position.
                    if (currObjectNumber.length() <= 1) {
                        currObjectNumber = "0"+currObjectNumber;
                    }

                    // remove all digits to become the non digits of the execution step (A, B, C ...)
                    String currObjectCharacters = currObject.replaceAll("[\\d]", "");


                    // create a new execution step with value in form of 1A and allocate his corresponding blank node
                    ExecutionStep currExecutionStep = new ExecutionStep(currObjectNumber, currObjectCharacters, currSubject);

                    this.ExecutionSteps.add(currExecutionStep);

                } else if (currProperty.equals(OWL2.annotatedProperty)) {

                    // create a new JSON object for the triple
                    JSONObject currTripleJSONObject = new JSONObject();

                    currTripleJSONObject.put("annotatedProperty", currObject);

                    // add a JSON array with key "triples" to the JSON object
                    this.jsonDataObjects.append(currSubject, currTripleJSONObject);

                } else if (currStmt.getObject().isLiteral()) {
                    // build an JSONObject to substitute later the key of the order

                    currObject = currStmt.getObject().asLiteral().getLexicalForm();

                    // create a new JSON object for the triple
                    JSONObject currTripleJSONObject = new JSONObject();

                    currTripleJSONObject.put("property", currProperty);
                    currTripleJSONObject.put("object", currObject);

                    // add a JSON array with key "triples" to the JSON object
                    this.jsonDataObjects.append(currSubject, currTripleJSONObject);

                } else {
                    // build an JSONObject to substitute later the key of the order

                    // create a new JSON object for the triple
                    JSONObject currTripleJSONObject = new JSONObject();

                    currTripleJSONObject.put("property", currProperty);
                    currTripleJSONObject.put("object", currObject);

                    // add a JSON array with key "triples" to the JSON object
                    this.jsonDataObjects.append(currSubject, currTripleJSONObject);

                }

            }

        }

        // reorder the data (1a, 1b, 1c, 2a, 2b , 3, 4a, 4b ...)
        Collections.sort(this.ExecutionSteps, new ExecutionStep.ExecStepComparator());
        for (ExecutionStep currExecStep : this.ExecutionSteps) {

            String number;

            // if the leading digit is a zero remove it
            if ((currExecStep.number.substring(0,1)).equals("0")) {

                number = (currExecStep.number).substring(1);

            } else {

                number = currExecStep.number;

            }

            System.out.println("currExecStep = " + number + currExecStep.character);

            // create an JSON array, which contains the execution step
            this.sortedKBIndicesJSONArray.put(number + currExecStep.character);

            // substitute the key from blanknode to the position in the JSON array
            this.sortedKBJSONArray.put(this.jsonDataObjects.getJSONArray(currExecStep.subjectKey));

        }

    }


}
