/*
 * Created by Roman Baum on 30.12.15.
 * Last modified by Roman Baum on 18.01.16.
 */

package soccomas.packages.querybuilder;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.lang.sparql_11.ParseException;

import java.util.ArrayList;

/**
 * This class contains methods to add some filter to a SPARQL Query.
 */
public class FilterBuilder {


    public FilterBuilder() {

    }


    /**
     * This method add(s) filter statement(s) to a constructBuilder.
     * @param constructBuilder the constructBuilder
     * @param filterToAdd the filter which should be added to the constructBuilder
     * @return the updated input constructBuilder
     */
    public ConstructBuilder addFilter (ConstructBuilder constructBuilder, ArrayList<String> filterToAdd) {

        for (String currFilterLine : filterToAdd) {
            try {
                constructBuilder.addFilter(currFilterLine);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }


        return constructBuilder;

    }

    /**
     * This method add(s) filter statement(s) to a selectBuilder.
     * @param selectBuilder the selectBuilder
     * @param filterToAdd the filter which should be added to the selectBuilder
     * @return the updated input selectBuilder
     */
    public SelectBuilder addFilter (SelectBuilder selectBuilder, ArrayList<String> filterToAdd) {

        for (String currFilterLine : filterToAdd) {
            try {
                selectBuilder.addFilter(currFilterLine);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }


        return selectBuilder;

    }

    /**
     * This method adds a new filter pair to the input ArrayList
     * @param filterItems an ArrayList which contains all filter pairs
     * @param firstItem first generic part of a filter
     * @param secondItem second generic part of a filter
     * @return the input ArrayList with the new added filter pair
     */
    public ArrayList<ArrayList<String>> addItems (ArrayList<ArrayList<String>> filterItems,
                                                  String firstItem, String secondItem) {

        ArrayList<String> varAList = new ArrayList<>();

        varAList.add(firstItem);
        varAList.add(secondItem);

        filterItems.add(varAList);

        return filterItems;

    }

}
