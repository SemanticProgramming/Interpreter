/*
 * Created by Roman Baum on 11.06.15.
 * Last modified by Roman Baum on 22.01.19.
 */
package soccomas.basic;

import soccomas.vocabulary.SCBasic;

/**
 *
 */
public class TDBPath {

    /**
     * differ between the entry status of the input variables (admin, core, draft or published)
     * @param workspace is the current workspace
     * @return the path to the jena tdb for the input variables
     */
    public String getPathToTDB (String workspace) {

        String pathToTDB = "WARN: No path was found!";

        if (workspace.equals(SCBasic.workspaceBASICCoreWorkspace.toString())
                || workspace.equals(SCBasic.soccomasWORKSPACEDIRECTORYCoreWorkspaceDirectory.toString())) {

            pathToTDB = ApplicationConfigurator.getPathToCoreOntologyStore();

        } else if (workspace.equals(SCBasic.workspaceBASICAdminWorkspace.toString())
                || workspace.equals(SCBasic.soccomasWORKSPACEDIRECTORYAdminWorkspaceDirectory.toString())) {

            pathToTDB = ApplicationConfigurator.getPathToAdminOntologyStore();

        } else if (workspace.equals(SCBasic.workspaceBASICOntologyWorkspace.toString())) {

            pathToTDB = ApplicationConfigurator.getPathToApplicationOntologyStore();

        } else if (workspace.equals(SCBasic.workspaceBASICPublishedWorkspace.toString())
                || workspace.equals(SCBasic.soccomasWORKSPACEDIRECTORYPublishedWorkspaceDirectory.toString())) {

            pathToTDB = ApplicationConfigurator.getPathToPublishedOntologyStore();

        } else if (workspace.equals(SCBasic.workspaceBASICDraftWorkspace.toString())
                || workspace.equals(SCBasic.soccomasWORKSPACEDIRECTORYDraftWorkspaceDirectory.toString())) {

            pathToTDB = ApplicationConfigurator.getPathToDraftOntologyStore();

        }
        
        return pathToTDB;
    }


}

