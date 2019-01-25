/*
 * Created by Roman Baum on 29.04.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.basic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * The Class MDBDate provides helpful methods to generate a date as a string with different formats.
 */
public class SOCCOMASDate {

    Date date;
    long timeInMillis;

    public SOCCOMASDate() {

        // create a new time instance
        Calendar calendar = Calendar.getInstance();

        this.timeInMillis = calendar.getTimeInMillis();

        this.date = calendar.getTime();

    }


    /**
     * This method creates a date String with format "dd.MM.yyyy".
     * @return a date String with format "dd.MM.yyyy"
     */
    public String getDate () {

        // set date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        // get the current date and return this result
        return dateFormat.format(this.date);
    }


    /**
     * This method returns a time in milliseconds.
     * @return a time in a String
     */
    public String getTimeInMillis() {

        // get the current date and return this result
        return String.valueOf(this.timeInMillis);
    }

    /**
     * This method creates a date String with format "yyyyMMdd".
     * @return a date String with format "yyyyMMdd"
     */
    public String getDateForURI () {

        // set date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

        // get the current date and return this result
        return dateFormat.format(this.date);
    }


    /**
     * This method checks if the input has the format "yyyyMMdd".
     * @param dateToCheck contains a date in a String
     * @return "true" if the input has the correct format, else "false"
     */
    public boolean isValidURIDateFormat(String dateToCheck) {

        try {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

            Date date = simpleDateFormat.parse(dateToCheck);

            return dateToCheck.equals(simpleDateFormat.format(date));

        } catch (ParseException ex) {

            return false;
        }


    }

}
