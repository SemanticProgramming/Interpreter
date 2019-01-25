/*
 * Created by Roman Baum on 11.05.15.
 * Last modified by Roman Baum on 27.01.17.
 */

package soccomas.packages.querybuilder;

import java.util.ArrayList;
import java.util.Iterator;



/**
 * The class "SPARQLFilter" provides one default constructor and two methods to create a String for a SPARQL Filter.
 */
public class SPARQLFilter {

    public SPARQLFilter () {

    }


    /**
     *  creates for the input items a filter for a SPARQL - Query
     * @param variable contains a SPARQL variable (e.g. ?var)
     * @param filterItems contains multiple URIs
     * @return a ArrayList<String> with this formal structure "(regex(STR(?var), URI_N)"
     */
    public ArrayList<String> getRegexSTRFilter(String variable, ArrayList<String> filterItems) {

        // create Iterator
        Iterator<String> filterIterator = filterItems.iterator();

        ArrayList<String> filterString = new ArrayList<>();

        if (filterIterator.hasNext()) {

            // iterate through the input array list
            while (filterIterator.hasNext()) {

                // differ between the cases
                filterString.add("regex(STR(" + variable + "), '" + filterIterator.next() + "')\n");

            }

        }

        return filterString;

    }



    /**
     *  creates for the input items a filter for a SPARQL - Query
     * @param variable contains a SPARQL variable (e.g. ?var)
     * @param filterItems contains multiple URIs
     * @return a ArrayList<String> with this formal structure "(strstarts(STR(?var), URI_N)"
     */
    public ArrayList<String> getSTRStartsSTRFilter(String variable, ArrayList<String> filterItems) {

        // create Iterator
        Iterator<String> filterIterator = filterItems.iterator();

        ArrayList<String> filterString = new ArrayList<>();

        if (filterIterator.hasNext()) {

            // iterate through the input array list
            while (filterIterator.hasNext()) {

                // differ between the cases
                filterString.add("strstarts(STR(" + variable + "), '" + filterIterator.next() + "')\n");

            }

        }

        return filterString;

    }

    /**
     * creates for the input items a filter for a SPARQL - Query
     * @param filterItems contains an array list with syntax [var_I, URI_N]
     * @return a ArrayList<String> with this formal structure "?var_I IN (URI_N)"
     */
    public ArrayList<String> getINFilter(ArrayList<ArrayList<String>> filterItems) {

        return getINOrNotINFilter(filterItems, "IN");

    }


    /**
     * creates for the input items a filter for a SPARQL - Query
     * @param filterItems contains an array list with syntax [var_I, exp_N] (exp is an URI or Literal)
     * @return a ArrayList<String> with this formal structure "CONTAINS(?var_I , exp_N)"
     */
    public ArrayList<String> getContainsFilter(ArrayList<ArrayList<String>> filterItems) {

        // create Iterators
        Iterator<ArrayList<String>> filterIterator = filterItems.iterator();

        ArrayList<String> filterString = new ArrayList<>();

        if (filterIterator.hasNext()) {

            // iterate through the input array list
            while (filterIterator.hasNext()) {

                ArrayList<String> currFilter = filterIterator.next();

                // differ between the cases
                filterString.add(" CONTAINS (" + currFilter.get(0) + " , '" + currFilter.get(1) + "')\n");

            }

        }

        return filterString;

    }


    /**
     * creates for the input items a filter for a SPARQL - Query
     * @param filterItems contains an array list with syntax [var_I, URI_N]
     * @return a ArrayList<String> with this formal structure "?var_I NOT IN (URI_N)"
     */
    public ArrayList<String> getNotINFilter(ArrayList<ArrayList<String>> filterItems) {

        return getINOrNotINFilter(filterItems, "NOT IN");

    }

    /**
     * creates for the input items a filter for a SPARQL - Query
     * @param filterItems contains an array list with syntax [var_I, URI_N]
     * @return a ArrayList<String> with the formal structure "?var_I IN (URI_N)" or the formal structure
     * "?var_I NOT IN (URI_N)"
     */
    private ArrayList<String> getINOrNotINFilter(ArrayList<ArrayList<String>> filterItems, String inOrNot) {

        // create Iterators
        Iterator<ArrayList<String>> filterIterator = filterItems.iterator();

        ArrayList<String> filterString = new ArrayList<>();

        if (filterIterator.hasNext()) {

            // iterate through the input array list
            while (filterIterator.hasNext()) {

                ArrayList<String> currFilter = filterIterator.next();

                // differ between the cases
                filterString.add(currFilter.get(0) + " " + inOrNot +" (" + currFilter.get(1) + ")\n");

            }

        }

        return filterString;

    }


}
