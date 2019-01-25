/*
 * Created by Roman Baum on 20.05.15.
 * Last modified by Roman Baum on 22.01.19.
 */
package soccomas.packages;


import org.apache.jena.rdf.model.Model;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


// TODO: create Test

/**
 * The class provide a method to modify named graphs in the jena tdb.
 */
public class JSONInputInterpreter {

    public JSONInputInterpreter() {
    }


    /**
     * The method save and/or delete triple(s) or deletes complete named graphs from a jena tdb. It use a JSON object as input.
     * @param inputDataObject is an object with our internal JSON data structure
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return an array list with output information
     */
    public ArrayList<String> interpretObject(JSONObject inputDataObject, JenaIOTDBFactory connectionToTDB) {

        // get the subjects from the JSON object
        JSONArray datasetsJSONArray = inputDataObject.getJSONArray("datasets");

        ArrayList<String> returnArrayList = new ArrayList<>();

        long dummy = System.currentTimeMillis();

        System.out.println((System.currentTimeMillis() - dummy) + "\t before loop");

        if (inputDataObject.has("deleteNamedGraphs")) {
            // remove complete named graphs from the store

            JSONArray deleteNamedGraphs = inputDataObject.getJSONArray("deleteNamedGraphs");

            for (int i = 0; i < deleteNamedGraphs.length(); i++) {

                connectionToTDB.removeNamedModelFromTDB(deleteNamedGraphs.getJSONObject(i).get("directory").toString(), deleteNamedGraphs.getJSONObject(i).get("ng").toString());

            }

            inputDataObject.remove("deleteNamedGraphs");

        }

        // iterate over all dataset(s)/directorie(s)
        for (int i = 0; i < datasetsJSONArray.length(); i++ ) {

            String currDirectory = datasetsJSONArray.getJSONObject(i).get("dataset").toString();

            JSONArray ngsJSONArray = datasetsJSONArray.getJSONObject(i).getJSONArray("ngs");

            ArrayList<String> ngNameList = new ArrayList<>();

            ArrayList<Model> modelsToInsert = new ArrayList<>();

            ArrayList<Model> modelsToDelete = new ArrayList<>();

            // iterate over all named graph(s)
            for (int j = 0; j < ngsJSONArray.length(); j++ ) {

                ArrayList<Model> currNGDataModelsList;

                // add name to array list
                ngNameList.add(ngsJSONArray.getJSONObject(j).get("ng").toString());

                JSONArray triplesJSONArray = ngsJSONArray.getJSONObject(j).getJSONArray("triples");

                // get an instance to create a new input model
                SOCCOMASInputModelFactory mdbInputModelFactory = new SOCCOMASInputModelFactory();

                // convert the JSON input to an input Model
                currNGDataModelsList = mdbInputModelFactory.createMDBInputModel(triplesJSONArray);

                // add the current specific insert() and the current specific delete model to an array list
                modelsToInsert.add(currNGDataModelsList.get(0));
                modelsToDelete.add(currNGDataModelsList.get(1));

                //System.out.println(currNGDataModelsList);

                // some logging information
                //System.out.println("Model 0 " + currNGDataModelsList.get(0));
                //System.out.println("Model 1 " + currNGDataModelsList.get(1));

            }

            String currOutputMessage;

            // look if their minimal one element exist
            // delete old statements from the jena tdb
            if (modelsToDelete.get(0) != null) {

                // delete the model data from the tdb
                currOutputMessage = connectionToTDB.removeModelsFromTDB(currDirectory, ngNameList, modelsToDelete);

                // add the current output message to the return value
                returnArrayList.add(currOutputMessage);

            }

            // look if their minimal one element exist
            // insert new statements in the jena tdb
            if (modelsToInsert.get(0) != null) {

                // save the model data to the tdb
                currOutputMessage = connectionToTDB.addModelsInTDB(currDirectory, ngNameList, modelsToInsert);

                // add the current output message to the return value
                returnArrayList.add(currOutputMessage);

            }

        }

        System.out.println((System.currentTimeMillis() - dummy) + "\t after loop");

        return returnArrayList;
    }
}
