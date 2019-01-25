/**
 * Created by Roman Baum on 28.10.16.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages;

import soccomas.basic.SOCCOMASIDChecker;
import org.apache.jena.rdf.model.ResourceFactory;


public class SOCCOMASIDFinder {

    private String mdbUEID, mdbCoreID, mdbEntryID;

    private boolean mdbUEIDNotEmpty = false, mdbEntryIDNotEmpty = false, mdbCoreIDNotEmpty = false;


    public SOCCOMASIDFinder(String uri, JenaIOTDBFactory connectionToTDB) {

        extractMDBID(uri, connectionToTDB);

    }


    /**
     * This method calculates the MDBCoreID for a known corresponding MDBEntryID.
     */
    private void calculateMDBCoreIDFromMDBEntryID () {

        this.mdbCoreID = this.mdbEntryID.substring(0, this.mdbEntryID.lastIndexOf("-"));

        this.mdbCoreIDNotEmpty = true;

        System.out.println(this.mdbCoreID + " is a MDBCoreID!");

        calculateMDBUEIDFromDBCoreID();

    }


    /**
     * This method calculates the MDBUEID for a known corresponding MDBCoreID.
     */
    private void calculateMDBUEIDFromDBCoreID() {

        this.mdbUEID = this.mdbCoreID.substring(0, this.mdbCoreID.indexOf("-"));

        this.mdbUEIDNotEmpty = true;

        System.out.println(this.mdbUEID + " is a MDBUEID!");

    }

    /**
     * This method calculate(s) MDB ID(s) in relation to an input uri.
     * @param uri contains an input uri
     * @param connectionToTDB contains a JenaIOTDBFactory object
     */
    private void extractMDBID(String uri, JenaIOTDBFactory connectionToTDB) {

        String potentialID = ResourceFactory.createResource(uri).getNameSpace();

        if (potentialID.endsWith("#")) {

            potentialID = potentialID.substring(0, potentialID.length() - 1);

            SOCCOMASIDChecker mdbIDChecker = new SOCCOMASIDChecker();

            boolean idIsCorrect = mdbIDChecker.isMDBID(potentialID, connectionToTDB);

            if (idIsCorrect) {

                if (mdbIDChecker.isMDBEntryID()) {

                    this.mdbEntryID = mdbIDChecker.getMDBEntryID();

                    this.mdbEntryIDNotEmpty = mdbIDChecker.isMDBEntryID();

                    calculateMDBCoreIDFromMDBEntryID();

                } else if (mdbIDChecker.isMDBCoreID()) {

                    this.mdbCoreID = mdbIDChecker.getMDBCoreID();

                    this.mdbCoreIDNotEmpty = mdbIDChecker.isMDBCoreID();

                    calculateMDBUEIDFromDBCoreID();

                } else if (mdbIDChecker.isMDBUEID()) {

                    this.mdbCoreID = mdbIDChecker.getMDBUEID();

                    this.mdbCoreIDNotEmpty = mdbIDChecker.isMDBUEID();

                }

            } else {

                System.out.println("The input URI has no MDB ID.");

            }

        } else {

            System.out.println("The input URI contains no '#'-sign.");

        }

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
    public boolean hasMDBCoreID() {
        return this.mdbCoreIDNotEmpty;
    }


    /**
     * This method checks if a MDBEntryID exist for this class.
     * @return "true" if the MDBEntryID exist, else "false"
     */
    public boolean hasMDBEntryID() {
        return this.mdbEntryIDNotEmpty;
    }


    /**
     * This method checks if a MDBUEID exist for this class.
     * @return "true" if the MDBUEID exist, else "false"
     */
    public boolean hasMDBUEID() {
        return this.mdbUEIDNotEmpty;
    }

}
