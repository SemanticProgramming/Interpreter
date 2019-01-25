/**
 * Created by Roman Baum on 17.11.16.
 * Last modified by Roman Baum on 22.11.16.
 */

package soccomas.packages.querybuilder;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;


public class QueryBuilderConverter {


    /**
     * This method converts a AskBuilder in a String
     * @return a SPARQL query String.
     */
    public String toString (AskBuilder askBuilder) {

        String sparqlQueryString = askBuilder.buildString();

        if (sparqlQueryString.contains("\"<(")) {

            sparqlQueryString = sparqlQueryString.replaceAll("\"<\\(", "(");

        }

        if (sparqlQueryString.contains(")>\"")) {

            sparqlQueryString = sparqlQueryString.replaceAll("\\)>\"", ")");

        }

        return sparqlQueryString;

    }

    /**
     * This method converts a ConstructBuilder in a String
     * @return a SPARQL query String.
     */
    public String toString (ConstructBuilder constructBuilder) {

        String sparqlQueryString = constructBuilder.buildString();

        if (sparqlQueryString.contains("\"<(")) {

            sparqlQueryString = sparqlQueryString.replaceAll("\"<\\(", "(");

        }

        if (sparqlQueryString.contains(")>\"")) {

            sparqlQueryString = sparqlQueryString.replaceAll("\\)>\"", ")");

        }

        return sparqlQueryString;

    }


    /**
     * This method converts a SelectBuilder in a String
     * @return a SPARQL query String.
     */
    public String toString (SelectBuilder selectBuilder) {

        String sparqlQueryString = selectBuilder.buildString();

        if (sparqlQueryString.contains("\"<(")) {

            sparqlQueryString = sparqlQueryString.replaceAll("\"<\\(", "(");

        }

        if (sparqlQueryString.contains(")>\"")) {

            sparqlQueryString = sparqlQueryString.replaceAll("\\)>\"", ")");

        }

        return sparqlQueryString;

    }

}
