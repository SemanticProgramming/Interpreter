/**
 * Created by Roman Baum on 16.11.17.
 * Last modified by Roman Baum on 16.11.17.
 */
package soccomas.vocabulary;


import org.json.JSONObject;

public class OntologyPrefixList {

    private JSONObject ontologyPrefixList = new JSONObject();

    public OntologyPrefixList() {
        fillOntologyPrefixList();
    }


    private void fillOntologyPrefixList() {

        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/BSPO", "BSPO");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/CARO", "CARO");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/CL", "CL");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/CHEBI", "CHEBI");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/EMAP", "EMAP");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/ENVO", "ENVO");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/UBERON", "UBERON");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/HAO", "HAO");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/MA", "MA");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/NCBITaxon", "NCBITaxon");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/OARCS", "OARCS");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/OBI", "OBI");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/PATO", "PATO");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/PO", "PO");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/PR", "PR");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/RNAO", "RNAO");
        this.ontologyPrefixList.put("http://ccdb.ucsd.edu/SAO/1.2#", "SAO");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/SO", "SO");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/SPD", "SPD");
        this.ontologyPrefixList.put("http://purl.obolibrary.org/obo/VSAO", "VSAO");
        this.ontologyPrefixList.put("http://www.geonames.org/ontology#", "GeoNames");

    }

    public JSONObject getOntologyPrefixList(){

        return this.ontologyPrefixList;

    }


}
