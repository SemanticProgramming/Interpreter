/*
 * Created by Roman Baum on 04.08.15.
 * Last modified by Roman Baum on 11.11.15.
 */

package soccomas.basic;

import java.util.Comparator;

/**
 * The class ExecutionStep contains three methods and implements the Comparator Interface for a specific problem.
 * The class sort an arbitrary execution order.
 */
public class ExecutionStep {


    public String number;
    public String character;
    public String subjectKey;

    public ExecutionStep(String number, String lastName, String subjectKey) {
        this.number = number;
        this.character = lastName;
        this.subjectKey = subjectKey;
    }

    /**
     * This method provide the number of the execution step.
     * @return a string with a number as content
     */
    public String getNumber() {
        return this.number;
    }

    /**
     * This method provide the character of the execution step.
     * @return a string with a character as content
     */
    public String getCharacter() {
        return this.character;
    }


    public static class ExecStepComparator implements Comparator<ExecutionStep> {

        /**
         * This method compares two input values and sort it.
         * @param exStep1 contains the values for an execution step
         * @param exStep2 contains the values for another execution step
         * @return an integer, which provide the correct order for this two steps
         */
        public int compare(ExecutionStep exStep1, ExecutionStep exStep2) {

            int nameComp = exStep1.getNumber().compareTo(exStep2.getNumber());

            return ((nameComp == 0) ? exStep1.getCharacter().compareTo(exStep2.getCharacter()) : nameComp);
        }
    }

}