/*
 * Created by Roman Baum on 18.05.15.
 * Last modified by Roman Baum on 18.05.15.
 */

package soccomas.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

/**
 * MDBAgent mdb.vocabulary class for namespace http://purl.org/pav/
 */
public class PAV {

    protected static final String uri ="http://purl.org/pav/";

    /** returns the URI for MDBCore
     * @return the URI for MDBCore
     */
    public static String getURI() {
        return uri;
    }

    private static Model m = ModelFactory.createDefaultModel();

    public static final Property createdBy = m.createProperty(uri, "createdBy" );
    public static final Property createdOn = m.createProperty(uri, "createdOn" );
    public static final Property createdWith = m.createProperty(uri, "createdWith" );
    public static final Property lastUpdatedOn = m.createProperty(uri, "lastUpdatedOn" );

}
