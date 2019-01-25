/**
 * Created by Roman Baum on 20.12.18.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.basic;


public class ApplicationConfigurator {

    protected static final String domain = "http://www.your-domain.com";

    private static final String mainDirectory = "/path/to/tuple-store-root-directory/";

    private static final String pathToJenaStore = mainDirectory + "tdb/";

    private static final String pathToApplicationOntologyStore = pathToJenaStore + "MDB_ontology_workspace/";

    private static final String pathToCoreOntologyStore = pathToJenaStore + "mdb_core_workspace/";

    private static final String pathToDraftOntologyStore = pathToJenaStore + "mdb_draft_workspace/";

    private static final String pathToPublishedOntologyStore = pathToJenaStore + "mdb_published_workspace/";

    private static final String pathToAdminOntologyStore = pathToJenaStore + "mdb_admin_workspace/";

    private static final String pathToLuceneStore = mainDirectory + "tdb-lucene/";

    public static String getDomain() {
        return domain;
    }

    public static String getPathToApplicationOntologyStore() {
        return pathToApplicationOntologyStore;
    }

    public static String getPathToCoreOntologyStore() {
        return pathToCoreOntologyStore;
    }

    public static String getPathToDraftOntologyStore() {
        return pathToDraftOntologyStore;
    }

    public static String getPathToPublishedOntologyStore() {
        return pathToPublishedOntologyStore;
    }

    public static String getPathToAdminOntologyStore() {
        return pathToAdminOntologyStore;
    }

    public static String getPathToJenaStore() {
        return pathToJenaStore;
    }

    public static String getPathToLuceneStore() {
        return pathToLuceneStore;
    }


}
