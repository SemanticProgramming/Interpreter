/**
 * Created by Roman Baum on 31.10.16.
 * Last modified by Roman Baum on 03.11.17.
 */
package soccomas.basic;


public class StringChecker {


    /**
     * This method checks if a String contains a valid integer value.
     * @param stringToCheck contains a String with a potential integer value
     * @return "true" if the String is a valid integer value, else "false"
     */
    public boolean checkIfStringIsAnInteger(String stringToCheck) {

        try {

            Integer.parseInt(stringToCheck);

            return true;

        } catch( NumberFormatException e ) {

            return false;

        }

    }

    /**
     * This method checks if a String contains a valid float value.
     * @param stringToCheck contains a String with a potential float value
     * @return "true" if the String is a valid float value, else "false"
     */
    public boolean checkIfStringIsAFloat(String stringToCheck) {

        try {

            Float.parseFloat(stringToCheck);

            return true;

        } catch( NumberFormatException e ) {

            return false;

        }

    }

    /**
     * This method checks if a String contains a valid hex value.
     * @param stringToCheck contains a String with a potential hex value
     * @return "true" if the String is a valid hex value, else "false"
     */
    public boolean checkIfStringIsAHex(String stringToCheck) {

        try {

            Long.parseLong(stringToCheck,16);

            return true;

        } catch( NumberFormatException e ) {

            return false;

        }

    }

}
