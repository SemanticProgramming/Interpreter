/*
 * Created by Roman Baum on 24.04.17.
 * Last modified by Roman Baum on 22.01.19.
 */
package soccomas.basic;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONObject;

public class ModelResourceExchanger {

    /**
     * This method renames subject individuals in a jena model.
     * @param model contains a model to handle with
     * @param jsonMap contains a map with resources to rename. The key contains the resource URI to rename.
     *                The corresponding value contains the new resource URI.
     * @return the updated input model
     */
    public Model substituteSubjectIndividualsInModel(Model model, JSONObject jsonMap) {

        ResIterator subIter = model.listSubjects();

        while (subIter.hasNext()) {

            Resource potentialSubject = subIter.next();

            if (model.contains(potentialSubject, RDF.type, OWL2.NamedIndividual) &&
                    !potentialSubject.toString().contains(ApplicationConfigurator.getDomain() + "/resource/")) {

                if (jsonMap.has(potentialSubject.toString())) {

                    ResourceUtils.renameResource(potentialSubject, jsonMap.get(potentialSubject.toString()).toString());

                }

            }

        }

        return model;

    }

}
