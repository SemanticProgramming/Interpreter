/*
 * Created by Roman Baum on 29.09.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.basic;

import soccomas.packages.JenaIOTDBFactory;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.vocabulary.SprO;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class OrderDataset {

    private ArrayList<String> sortedObjects = new ArrayList<>();
    private ArrayList<String> sortedProperties = new ArrayList<>();

    public OrderDataset(JSONArray unorderedGenerateRes, String pathToOntologies, JenaIOTDBFactory connectionToTDB) {

        sortGenerateResources(unorderedGenerateRes, pathToOntologies, connectionToTDB);

    }

    /**
     * This method return an ordered ArrayList<String> with object resources.
     * @return an ordered ArrayList<String> with object resources
     */
    public ArrayList<String> getSortedObjects(){

        return this.sortedObjects;

    }

    /**
     * This method return an ordered ArrayList<String> with property resources.
     * @return an ordered ArrayList<String> with property resources
     */
    public ArrayList<String> getSortedProperties(){

        return this.sortedProperties;

    }

    /**
     * This method orders an input dataset.
     * @param currExecStep contains all information from the ontology for the current execution step
     * @param pathToTDB contains the path to the jena tdb
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    private void sortGenerateResources (JSONArray currExecStep, String pathToTDB, JenaIOTDBFactory connectionToTDB) {

        ArrayList<Integer> indices = new ArrayList<>();

        JSONObject unorderedObjectsJSONObject = new JSONObject();
        JSONObject unorderedPropertiesJSONObject = new JSONObject();

        for (int i = 0; i < currExecStep.length(); i++) {

            if (!currExecStep.getJSONObject(i).get("property").toString().contains(SprO.generatesResourcesForEntryID.toString())) {
                // generates resources for MDB entry ID

                // build a query
                SelectBuilder selectBuilder = new SelectBuilder();

                PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

                selectBuilder = prefixesBuilder.addPrefixes(selectBuilder);

                selectBuilder.addVar(selectBuilder.makeVar("?o"));

                SelectBuilder tripleSPO = new SelectBuilder();

                tripleSPO = prefixesBuilder.addPrefixes(tripleSPO);

                UrlValidator urlValidator = new UrlValidator();

                if (urlValidator.isValid(currExecStep.getJSONObject(i).get("property").toString())) {

                    tripleSPO.addWhere("<" + currExecStep.getJSONObject(i).get("property").toString() + ">", SprO.hasSortingValue, "?o");
                    // has sorting value
                    tripleSPO.addWhere("<" + currExecStep.getJSONObject(i).get("property").toString() + ">", RDFS.subPropertyOf, SprO.generateResourceOfClassAnnotation);
                    // 'generate resource of class' annotation

                } else {

                    tripleSPO.addWhere(currExecStep.getJSONObject(i).get("property").toString(), SprO.hasSortingValue, "?o");
                    // has sorting value
                    tripleSPO.addWhere(currExecStep.getJSONObject(i).get("property").toString(), RDFS.subPropertyOf, SprO.generateResourceOfClassAnnotation);
                    // 'generate resource of class' annotation

                }

                selectBuilder.addGraph("?g", tripleSPO);

                String sparqlQueryString = selectBuilder.buildString();

                //System.out.println("sparqlQueryString to change: " + sparqlQueryString);

                String selectResult = connectionToTDB.pullSingleDataFromTDB(pathToTDB, sparqlQueryString, "?o");

                //System.out.println("selectResult " + selectResult.replaceAll("[\\D]", ""));

                // check if the literal contains a number
                if (!(selectResult.replaceAll("[\\D]", "").isEmpty())) {
                    // remove all non digits to become the digits of the execution step (1, 2, 3, ...) and parse the result
                    // in Integer for a correct Order (e.g. "11", "2" ,"13" --> correct 2, 11, 13 vs. false "11", "13", "2"
                    indices.add(Integer.valueOf(selectResult.replaceAll("[\\D]", "")));

                    // save the context between the index and the value in a dummy JSON object
                    unorderedObjectsJSONObject.put(selectResult.replaceAll("[\\D]", ""), currExecStep.getJSONObject(i).get("object").toString());
                    unorderedPropertiesJSONObject.put(selectResult.replaceAll("[\\D]", ""), currExecStep.getJSONObject(i).get("property").toString());

                }

            }

        }

        // sort the values of the generic resource property
        Collections.sort(indices);

        // fill return ArrayList with values in the correct order
        for (Integer currIndex : indices) {

            this.sortedObjects.add(unorderedObjectsJSONObject.get(currIndex.toString()).toString());

            this.sortedProperties.add(unorderedPropertiesJSONObject.get(currIndex.toString()).toString());

        }

    }

}
