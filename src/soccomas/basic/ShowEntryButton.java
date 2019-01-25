/**
 * Created by Roman Baum on 18.10.17.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.basic;


import soccomas.vocabulary.SCBasic;

public class ShowEntryButton {

    private static final String nameSpace = SCBasic.getURI();

    //public static final String localClassID = SCBasic.sampleEntryButtonItem.getLocalName();
    // sample entry button item

    public static final String localClassID = SCBasic.resolveToEntryDummyButtonItem.getLocalName();
    // resolve to entry dummy button item

    public static final String classID = nameSpace + localClassID;

    //public static final String localIndividualID = SCBasic.bASICSOCCOMASCOMPONENTSampleEntryButton.getLocalName();
    // BASIC_SOCCOMAS_COMPONENT: sample entry button

    public static final String localIndividualID = SCBasic.basicSOCCOMASCOMPONENTITEMResolveToEntryDummyButton.getLocalName();
    // BASIC_SOCCOMAS_COMPONENT_ITEM: resolve to entry dummy button

    public static final String individualID = nameSpace + localIndividualID;

}
