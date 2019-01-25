/*
 * Created by Roman Baum on 09.04.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.packages;

import soccomas.basic.SOCCOMASURLEncoder;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * The class "MDBInputModelFactory" provides one default constructor and one methods. The method "createMDBInputModel"
 * create a jena model from a JSONArray.
 */

public class SOCCOMASInputModelFactory {

    public SOCCOMASInputModelFactory() {

    }

    /**
     * This method creates an ArrayList with jena models inside from a JSONArray.
     * @param triplesJSONArray contains the triple information (subject, property, object data, object type and
     *                         operation)
     * @return a ArrayList with jena models, which contains the triple information
     */
    public ArrayList<Model> createMDBInputModel(JSONArray triplesJSONArray) {

        ArrayList<Model> returnModelList = new ArrayList<>();

        // create a default models
        Model insertModel = ModelFactory.createDefaultModel();
        Model deleteModel = ModelFactory.createDefaultModel();

        Resource currSubject;

        Property currProperty;

        RDFNode currObject;

        Statement currStatement;

        for (int i = 0; i < triplesJSONArray.length(); i++ ) {

            // get an url validator to check the input
            UrlValidator uriValidator = new UrlValidator();

            JSONObject currTripleJSONObject = triplesJSONArray.getJSONObject(i);

            // get a MDB url Encoder to encode the uri with utf-8
            SOCCOMASURLEncoder mdbURLEncoder = new SOCCOMASURLEncoder();

            // check if the subject has a valid uri and encode the uri with UTF-8
            if (uriValidator.isValid(mdbURLEncoder.encodeUrl(currTripleJSONObject.get("subject").toString(), "UTF-8"))) {

                currSubject = ResourceFactory.createResource(currTripleJSONObject.get("subject").toString());

            } else {

                AnonId anonId = new AnonId(currTripleJSONObject.get("subject").toString());

                currSubject = ModelFactory.createDefaultModel().createResource(anonId);

            }

            // check if the property has a valid uri
            if (uriValidator.isValid(mdbURLEncoder.encodeUrl(currTripleJSONObject.get("property").toString(), "UTF-8"))) {

                currProperty = ResourceFactory.createProperty(currTripleJSONObject.get("property").toString());

            } else {

                currProperty = ResourceFactory.createProperty("http://www.dummy.com/property");

                System.out.println("The input value for the property is not valid >>> " + currTripleJSONObject.get("property").toString());

            }

            // get the current sub JSON object
            JSONObject currInputSubObject = currTripleJSONObject.getJSONObject("object");

            // check if the input value is a resource and then if the URI is valid or not
            if (currInputSubObject.get("object_type").toString().equals("r") &&
                    (   (uriValidator.isValid(mdbURLEncoder.encodeUrl(currInputSubObject.get("object_data").toString(), "UTF-8"))) ||
                        (EmailValidator.getInstance().isValid(mdbURLEncoder.encodeUrl(currInputSubObject.get("object_data").toString(), "UTF-8"))))) {
                //System.out.println("The input value is an object.");

                currObject = ResourceFactory.createResource(currInputSubObject.get("object_data").toString());

            } else if (currInputSubObject.get("object_type").toString().equals("l")) {
                //System.out.println("The input value is a literal.");

                if (currInputSubObject.get("object_data").toString().contains("^^")) {
                    // special case object_data contains a type literal

                    String objectToCheck = currInputSubObject.get("object_data").toString();

                    String beforeSeparator = objectToCheck.substring(0, objectToCheck.indexOf("^^"));

                    String behindSeparator = objectToCheck.substring(objectToCheck.indexOf("^^") + 2);

                    UrlValidator urlValidator = new UrlValidator();

                    if (urlValidator.isValid(behindSeparator)) {

                        if (XSDDatatype.XSDboolean.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDboolean);

                        } else if (XSDDatatype.XSDbyte.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDbyte);

                        } else if (XSDDatatype.XSDdate.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDdate);

                        } else if (XSDDatatype.XSDdateTime.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDdateTime);

                        } else if (XSDDatatype.XSDdateTimeStamp.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDdateTimeStamp);

                        } else if (XSDDatatype.XSDdayTimeDuration.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDdayTimeDuration);

                        } else if (XSDDatatype.XSDdecimal.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDdecimal);

                        } else if (XSDDatatype.XSDdouble.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDdouble);

                        } else if (XSDDatatype.XSDduration.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDduration);

                        } else if (XSDDatatype.XSDENTITY.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDENTITY);

                        } else if (XSDDatatype.XSDfloat.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDfloat);

                        } else if (XSDDatatype.XSDgDay.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDgDay);

                        } else if (XSDDatatype.XSDgMonth.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDgMonth);

                        } else if (XSDDatatype.XSDgMonthDay.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDgMonthDay);

                        } else if (XSDDatatype.XSDgYear.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDgYear);

                        } else if (XSDDatatype.XSDgYearMonth.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDgYearMonth);

                        } else if (XSDDatatype.XSDhexBinary.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDhexBinary);

                        } else if (XSDDatatype.XSDID.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDID);

                        } else if (XSDDatatype.XSDIDREF.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDIDREF);

                        } else if (XSDDatatype.XSDint.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDint);

                        } else if (XSDDatatype.XSDinteger.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDinteger);

                        } else if (XSDDatatype.XSDlanguage.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDlanguage);

                        } else if (XSDDatatype.XSDlong.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDlong);

                        } else if (XSDDatatype.XSDName.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDName);

                        } else if (XSDDatatype.XSDNCName.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDNCName);

                        } else if (XSDDatatype.XSDnegativeInteger.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDnegativeInteger);

                        } else if (XSDDatatype.XSDNMTOKEN.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDNMTOKEN);

                        } else if (XSDDatatype.XSDnonNegativeInteger.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDnonNegativeInteger);

                        } else if (XSDDatatype.XSDnonPositiveInteger.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDnonPositiveInteger);

                        } else if (XSDDatatype.XSDnormalizedString.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDnormalizedString);

                        } else if (XSDDatatype.XSDNOTATION.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDNOTATION);

                        } else if (XSDDatatype.XSDpositiveInteger.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDpositiveInteger);

                        } else if (XSDDatatype.XSDQName.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDQName);

                        } else if (XSDDatatype.XSDshort.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDshort);

                        } else if (XSDDatatype.XSDstring.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDstring);

                        } else if (XSDDatatype.XSDtime.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDtime);

                        } else if (XSDDatatype.XSDtoken.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDtoken);

                        } else if (XSDDatatype.XSDunsignedByte.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDunsignedByte);

                        } else if (XSDDatatype.XSDunsignedInt.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDunsignedInt);

                        } else if (XSDDatatype.XSDunsignedLong.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDunsignedLong);

                        } else if (XSDDatatype.XSDunsignedShort.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDunsignedShort);

                        } else if (XSDDatatype.XSDyearMonthDuration.getURI().equals(behindSeparator)) {

                            currObject = ResourceFactory.createTypedLiteral(beforeSeparator, XSDDatatype.XSDyearMonthDuration);

                        } else {

                            currObject = ResourceFactory.createPlainLiteral(currInputSubObject.get("object_data").toString());

                        }

                    } else {

                        currObject = ResourceFactory.createPlainLiteral(currInputSubObject.get("object_data").toString());

                    }

                } else {

                    currObject = ResourceFactory.createPlainLiteral(currInputSubObject.get("object_data").toString());

                }

            } else {

                currObject = ResourceFactory.createResource("http://www.dummy.com/object");

                System.out.println("The input value for the object is not valid >>> " + currInputSubObject.get("object_data").toString());
            }

            currStatement = ResourceFactory.createStatement(currSubject, currProperty, currObject);

            // differ between save or delete triples
            if (currTripleJSONObject.get("operation").toString().equals("s")) {

                insertModel.add(currStatement);

            } else if (currTripleJSONObject.get("operation").toString().equals("d")) {

                deleteModel.add(currStatement);

            }


        }

        returnModelList.add(insertModel);

        returnModelList.add(deleteModel);

        return returnModelList;

    }
}
