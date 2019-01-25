/*
 * Created by Roman Baum on 09.10.15.
 * Last modified by Roman Baum on 22.01.19.
 */
package soccomas.packages.keywords;

import soccomas.basic.SOCCOMASDate;
import soccomas.vocabulary.SCBasic;
import soccomas.vocabulary.SprO;

//TODO write tests

/**
 * The class "KeywordInterpreter" provide a method which combine the different input keywords with the corresponding
 * class URI of the MDB Ontologies.
 */
public class KeywordInterpreter {

    private String mdbUser;


    public KeywordInterpreter() {

    }


    public KeywordInterpreter(String mdbUser) {
        this.mdbUser = mdbUser;
    }





    /**
     * calculate the corresponding ontology entity of an input URI
     * @param keywordURI a specific keyword URI
     * @return the corresponding class of the specific keyword URI
     */
    public String getEntityByKeyword(String keywordURI) {

        String entity;

        // TODO provide more cases for other keywords
        if (keywordURI.equals(SprO.iNPUTCONTROLDateTimeStamp.toString())) {

            SOCCOMASDate soccomasDate = new SOCCOMASDate();

            entity = soccomasDate.getDate();

        } else if (keywordURI.equals(SprO.sproVARIABLEThisContributionsNamedGraph.toString())) {

            entity = SCBasic.contributionsNamedGraph.toString();

        } else if (keywordURI.equals(SprO.sproVARIABLENameOfThisUserID.toString())) {

            entity = "User Name";

        } else {

            entity = keywordURI;

        }

        return entity;

    }

    /**
     * returns the MDB User of the class
     * @return the MDB User of the class
     */
    public String getMdbUser() {
        return mdbUser;
    }


    /**
     * set the input as the internal mdbUser
     * @param mdbUser name of the mdb user
     */
    public void setMdbUser(String mdbUser) {
        this.mdbUser = mdbUser;
    }


}
