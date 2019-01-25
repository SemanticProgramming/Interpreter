/**
 * Created by Roman Baum on 20.10.17.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.basic;

import soccomas.packages.JenaIOTDBFactory;
import soccomas.packages.querybuilder.FilterBuilder;
import soccomas.packages.querybuilder.PrefixesBuilder;
import soccomas.packages.querybuilder.SPARQLFilter;
import soccomas.vocabulary.SprO;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import java.util.ArrayList;


public class SOCCOMASIDChecker {

    private String mdbUEID, mdbCoreID, mdbEntryID;

    private boolean mdbUEIDNotEmpty = false, mdbEntryIDNotEmpty = false, mdbCoreIDNotEmpty = false;

    /**
     * This method checks if a String contains a valid integer value.
     * @param stringToCheck contains a String with a potential integer value
     * @return "true" if the String is a valid integer value, else "false"
     */
    private boolean checkIfStringIsAnInteger(String stringToCheck) {

        StringChecker stringChecker = new StringChecker();

        return stringChecker.checkIfStringIsAnInteger(stringToCheck);

    }


    /**
     * This method checks if a String contains a valid hex value.
     * @param stringToCheck contains a String with a potential hex value
     * @return "true" if the String is a valid hex value, else "false"
     */
    private boolean checkIfStringIsAHex(String stringToCheck) {

        StringChecker stringChecker = new StringChecker();

        return stringChecker.checkIfStringIsAHex(stringToCheck);

    }


    /**
     * This method checks if a String contains a valid MDB Entry Data Type.
     * @param stringToCheck contains a String with a potential MDB Entry Data Type
     * @param connectionToTDB contains a JenaIOTDBFactory object
     * @return "true" if the String is a valid MDB Entry Data Type, else "false"
     */
    private boolean checkIfStringIsMDBEntryDataType(String stringToCheck, JenaIOTDBFactory connectionToTDB) {

        PrefixesBuilder prefixesBuilder = new PrefixesBuilder();

        SelectBuilder selectWhereBuilder = new SelectBuilder();

        selectWhereBuilder = prefixesBuilder.addPrefixes(selectWhereBuilder);

        selectWhereBuilder.addWhere("?s", SprO.hasAbbreviation, "?o");
        // has abbreviation

        FilterBuilder filterBuilder = new FilterBuilder();

        SPARQLFilter sparqlFilter = new SPARQLFilter();

        ArrayList<String> filterItems = new ArrayList<>();

        filterItems.add(stringToCheck.toUpperCase());

        ArrayList<String> filter = sparqlFilter.getRegexSTRFilter("?o", filterItems);

        selectWhereBuilder = filterBuilder.addFilter(selectWhereBuilder, filter);

        //selectWhereBuilder.addWhere("?s", SprO.hasAbbreviation, "?o");

        // create main query structure

        AskBuilder askBuilder = new AskBuilder();

        askBuilder = prefixesBuilder.addPrefixes(askBuilder);

        askBuilder.addGraph("?g", selectWhereBuilder);

        // create a Query
        Query sparqlQuery = QueryFactory.create(askBuilder.buildString());

        return Boolean.parseBoolean(connectionToTDB.pullStringDataFromTDB(ApplicationConfigurator.getPathToApplicationOntologyStore(), sparqlQuery, "RDF/XML-ABBREV"));

    }


    /**
     * This method checks if a String contains a valid MDB Version ID.
     * @param stringToCheck contains a String with a potential MDB Version ID
     * @param numberOfUnderscoreOccurrences contains the number of underscores in the input String
     * @return "true" if the String is a valid MDB Version ID, else "false"
     */
    private boolean checkIfStringIsMDBVersion(String stringToCheck, int numberOfUnderscoreOccurrences) {

        if (numberOfUnderscoreOccurrences == 1) {
            // published entry

            String[] underscoreSplitParts = stringToCheck.split("_");

            return underscoreSplitParts[0].equals("p") &&
                    checkIfStringIsAnInteger(underscoreSplitParts[1]);


        } else if (numberOfUnderscoreOccurrences == 2) {
            // revision or draft entry

            String[] underscoreSplitParts = stringToCheck.split("_");

            // the combination of 2 different "String" integers must also be an integer
            return (underscoreSplitParts[0].equals("d")) &&
                    checkIfStringIsAnInteger(underscoreSplitParts[1] + underscoreSplitParts[2]);

        } else {

            return false;

        }

    }


    public boolean isMDBID(String potentialMDBID, JenaIOTDBFactory connectionToTDB) {

        String idPartToAnalyze = potentialMDBID.substring(potentialMDBID.lastIndexOf("/") + 1);

        String[] partsFromPotentialID = idPartToAnalyze.split("-");

        int numberOfHyphenOccurrences = StringUtils.countMatches(idPartToAnalyze, "-");

        boolean idIsCorrect;

        switch (numberOfHyphenOccurrences) {

            case 0 :

                idIsCorrect = checkIfStringIsAHex(idPartToAnalyze);

                if (idIsCorrect) {

                    System.out.println(potentialMDBID + " is a MDBUEID!");

                    this.mdbUEID = potentialMDBID;

                    this.mdbUEIDNotEmpty = true;

                    return true;

                }

                break;

            case 3 :

                idIsCorrect = checkIfStringIsAHex(partsFromPotentialID[0]);

                if (idIsCorrect) {

                    SOCCOMASDate soccomasDat = new SOCCOMASDate();

                    idIsCorrect = soccomasDat.isValidURIDateFormat(partsFromPotentialID[1]);

                }

                if (idIsCorrect) {

                    idIsCorrect = checkIfStringIsMDBEntryDataType(partsFromPotentialID[2], connectionToTDB);

                }

                if (idIsCorrect) {

                    idIsCorrect = checkIfStringIsAnInteger(partsFromPotentialID[3]);

                }

                if (idIsCorrect) {

                    System.out.println(potentialMDBID + " is a MDBCoreID!");

                    this.mdbCoreID = potentialMDBID;

                    this.mdbCoreIDNotEmpty = true;

                    return true;

                }

                break;

            case 4 :

                idIsCorrect = checkIfStringIsAHex(partsFromPotentialID[0]);

                if (idIsCorrect) {

                    SOCCOMASDate soccomasDat = new SOCCOMASDate();

                    idIsCorrect = soccomasDat.isValidURIDateFormat(partsFromPotentialID[1]);

                }

                if (idIsCorrect) {

                    idIsCorrect = checkIfStringIsMDBEntryDataType(partsFromPotentialID[2], connectionToTDB);

                }

                if (idIsCorrect) {

                    idIsCorrect = checkIfStringIsAnInteger(partsFromPotentialID[3]);

                }

                if (idIsCorrect) {

                    int numberOfUnderscoreOccurrences = StringUtils.countMatches(idPartToAnalyze, "_");

                    idIsCorrect = checkIfStringIsMDBVersion(partsFromPotentialID[4], numberOfUnderscoreOccurrences);

                }

                if (idIsCorrect) {

                    System.out.println(potentialMDBID + " is a MDBEntryID!");

                    this.mdbEntryID = potentialMDBID;

                    this.mdbEntryIDNotEmpty = true;

                    return true;

                }

                break;

        }

        System.out.println("The input URI has no MDB ID.");

        return false;

    }

    /**
     * This method is a getter for the class specific MDBCoreID.
     * @return a MDBCoreID
     */
    public String getMDBCoreID() {
        return this.mdbCoreID;
    }


    /**
     * This method is a getter for the class specific MDBEntryID.
     * @return a MDBEntryID
     */
    public String getMDBEntryID() {
        return this.mdbEntryID;
    }


    /**
     * This method is a getter for the class specific MDBUEID.
     * @return a MDBUEID
     */
    public String getMDBUEID() {
        return this.mdbUEID;
    }

    /**
     * This method checks if a MDBCoreID exist for this class.
     * @return "true" if the MDBCoreID exist, else "false"
     */
    public boolean isMDBCoreID() {
        return this.mdbCoreIDNotEmpty;
    }


    /**
     * This method checks if a MDBEntryID exist for this class.
     * @return "true" if the MDBEntryID exist, else "false"
     */
    public boolean isMDBEntryID() {
        return this.mdbEntryIDNotEmpty;
    }


    /**
     * This method checks if a MDBUEID exist for this class.
     * @return "true" if the MDBUEID exist, else "false"
     */
    public boolean isMDBUEID() {
        return this.mdbUEIDNotEmpty;
    }

}
