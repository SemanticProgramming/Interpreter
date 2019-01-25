package soccomas.vocabulary;

/**
 * Created by rbaum on 16.01.15.
 */

import org.apache.jena.rdf.model.Model ;
import org.apache.jena.rdf.model.ModelFactory ;
import org.apache.jena.rdf.model.Property ;

public class FOAFAdvanced {

    /** <p>The RDF model that holds the mdb.vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();

    /** <p>The first name of a person.</p> */
    public static final Property lastName = m_model.createProperty( "http://xmlns.com/foaf/0.1/lastName" );

}
