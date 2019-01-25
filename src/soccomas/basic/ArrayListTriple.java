/*
 * Created by Roman Baum on 02.06.15.
 * Last modified by Roman Baum on 18.08.15.
 */
package soccomas.basic;

import java.util.ArrayList;

/**
 * This Class provides a method to organize a subject, a property and an object in an ArrayList
 */
public class ArrayListTriple {

    private String subjectInput;
    private String propertyInput;
    private String objectInput;

    /**
     * This Constructor initializes the private variables
     * @param subjectInput is the subject of the triple
     * @param propertyInput is the property of the triple
     * @param objectInput is the object of the triple
     */
    public ArrayListTriple(String subjectInput, String propertyInput, String objectInput) {
        this.subjectInput = subjectInput;
        this.propertyInput = propertyInput;
        this.objectInput = objectInput;
    }

    /**
     * This method organizes the input variables as a triple in an ArrayList
     * @return an ArrayList with content (subject, property, object)
     */
    public ArrayList<String> getTripleArrayList() {

        ArrayList<String> tripleArrayList = new ArrayList<>();

        // add the triple to the collection
        tripleArrayList.add(this.subjectInput);
        tripleArrayList.add(this.propertyInput);
        tripleArrayList.add(this.objectInput);

        return tripleArrayList;
    }
}
