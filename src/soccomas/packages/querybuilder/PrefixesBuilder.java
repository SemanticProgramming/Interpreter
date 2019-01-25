/*
 * Created by Roman Baum on 18.01.16.
 * Last modified by Roman Baum on 20.12.18.
 */

package soccomas.packages.querybuilder;

import soccomas.vocabulary.*;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * This class contains methods to add some filter to a SPARQL Query.
 */
public class PrefixesBuilder {


    JSONObject pfxJSONObject = new JSONObject();


    public PrefixesBuilder() {

        this.pfxJSONObject = generatePfxJSONObject(this.pfxJSONObject);
    }


    /**
     * This method add(s) prefixes to an askBuilder.
     * @param askBuilder the askBuilder
     * @return the updated input askBuilder
     */
    public AskBuilder addPrefixes (AskBuilder askBuilder) {

        Iterator<String> pfxIterator = this.pfxJSONObject.keys();

        while (pfxIterator.hasNext()) {

            String currPrefix = pfxIterator.next();

            askBuilder.addPrefix(currPrefix, this.pfxJSONObject.get(currPrefix).toString());

        }


        return askBuilder;
    }


    /**
     * This method add(s) prefixes to a constructBuilder.
     * @param constructBuilder the constructBuilder
     * @return the updated input constructBuilder
     */
    public ConstructBuilder addPrefixes (ConstructBuilder constructBuilder) {

        Iterator<String> pfxIterator = this.pfxJSONObject.keys();

        while (pfxIterator.hasNext()) {

            String currPrefix = pfxIterator.next();

            constructBuilder.addPrefix(currPrefix, this.pfxJSONObject.get(currPrefix).toString());

        }


        return constructBuilder;
    }

    /**
     * This method add(s) prefixes to a selectBuilder.
     * @param selectBuilder the selectBuilder
     * @return the updated input selectBuilder
     */
    public SelectBuilder addPrefixes (SelectBuilder selectBuilder) {

        Iterator<String> pfxIterator = this.pfxJSONObject.keys();

        while (pfxIterator.hasNext()) {

            String currPrefix = pfxIterator.next();

            selectBuilder.addPrefix(currPrefix, this.pfxJSONObject.get(currPrefix).toString());

        }

        return selectBuilder;
    }



    /**
     * This method generates an JSONObject which contains many known prefixes.
     * @return an JSONObject with known prefixes
     */
    public JSONObject generatePfxJSONObject (JSONObject pfxJSONObject) {

        pfxJSONObject.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pfxJSONObject.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        pfxJSONObject.put("owl", "http://www.w3.org/2002/07/owl#");
        pfxJSONObject.put("text", "http://jena.apache.org/text#");
        pfxJSONObject.put("dcterms", "http://purl.org/dc/terms/");
        pfxJSONObject.put("foaf", "http://xmlns.com/foaf/0.1/");
        pfxJSONObject.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        pfxJSONObject.put("skos", "http://www.w3.org/2004/02/skos/core#");

        pfxJSONObject.put("spro", SprO.getURI());
        pfxJSONObject.put("scbasic", SCBasic.getURI());
        pfxJSONObject.put("scmdbbasic", SCMDBBasic.getURI());
        pfxJSONObject.put("scmdbmd", SCMDBMD.getURI());
        pfxJSONObject.put("scmdbs", SCMDBS.getURI());

        return pfxJSONObject;
    }
}
