/*
 * Created by Roman Baum on 02.11.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.mongodb;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.UpdateResult;
import soccomas.basic.SOCCOMASURLEncoder;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.rdf.model.ResourceFactory;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * This class build a connection to a local mongoDB and provide some methods to communicate with a db in the mongoDB.
 */
public class MongoDBConnection {

    MongoClient mongoClient = new MongoClient("localhost", 27017);

    BasicDBObject dbIdentifier;

    /**
     * Create a new connection to a mongoDB on a server with a specific port
     * @param server is the server address where the mongoDB exist (default: localhost)
     * @param port is the virtual data connection to the mongo db (default: 27017)
     */
    public MongoDBConnection(String server, Integer port) {

        this.mongoClient = new MongoClient(server, port);

    }

    /**
     * Checks if an input collection exist in the mongoDB or not.
     * @param db is the name of the mongoDB
     * @param collectionToCheck is the name of the collection to find in the mongoDB
     * @return true if the collection is in the database
     */
    public boolean collectionExist(String db, String collectionToCheck) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        MongoIterable<String> mongoIT = database.listCollectionNames();

        for (String currCollection : mongoIT) {

            if (currCollection.equals(collectionToCheck)) {

                return true;

            }

        }

        return false;
    }

    /**
     * This method close the connection of a mongo db client.
     */
    public void closeConnection() {

        this.mongoClient.close();

    }

    /**
     * Create a new collection in an input mongoDB.
     * @param db is the name of the mongoDB
     * @param collectionName is the name of the collection which will create in the mongoDB
     * @return a message which depends of the collection was successfully created or not
     */
    public String createCollection(String db, String collectionName) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        database.createCollection(collectionName);

        return collectionExist(db, collectionName) ?
                "The collection named \"" + collectionName + "\" was successfully created." :
                "The collection named \"" + collectionName + "\" wasn't successfully created.";

    }

    /**
     * Drop a collection of a certain db in mongoDB.
     * @param db is the name of the db
     * @param collectionName is the name of the collection which will we dropped
     * @return a message
     */
    public String dropCollection(String db, String collectionName) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> collection = database.getCollection(collectionName);

        collection.drop();

        return collectionExist(db, collectionName) ?
                "The collection named \"" + collectionName + "\" was successfully dropped." :
                "The collection named \"" + collectionName + "\" still exists.";

    }


    /**
     * This method checks if a document in the mongoDB contains a key or not.
     * @param db is the name of the mongoDB
     * @param collectionToCheck is the name of the collection in the mongoDB
     * @param key is the key to find
     * @return true if there is a document which contains the key
     */
    public boolean documentExist(String db, String collectionToCheck, String key) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collectionToCheck);

        return mongoCollection.find(new Document(key, new BasicDBObject("$exists", true))).iterator().hasNext();

    }


    /**
     * This method pull a JSONArray for an input key.
     * @param db is the name of the mongoDB
     * @param collectionToCheck is the name of the collection in the mongoDB
     * @param key is the key to find
     * @return a JSONArray from the mongoDB
     */
    public JSONArray getJSONArrayForKey(String db, String collectionToCheck, String key) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collectionToCheck);

        JSONObject docJSON = new JSONObject(mongoCollection.find(new Document(key, new BasicDBObject("$exists", true))).first().toJson());

        return docJSON.getJSONArray(key);

    }


    /**
     * Checks if a document in the mongoDB contains a (key, value)-pair or not.
     * @param db is the name of the mongoDB
     * @param collectionToCheck is the name of the collection in the mongoDB
     * @param key is the key to find
     * @param value is the value to find
     * @return true if there is a document which contains the (key, value)-pair
     */
    public boolean documentExist(String db, String collectionToCheck, String key, String value) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collectionToCheck);


        return mongoCollection.find(new Document(key, value)).iterator().hasNext();

    }

    /**
     * Checks if a document in the mongoDB contains a (key, value)-pair or not.
     * @param db is the name of the mongoDB
     * @param collectionToCheck is the name of the collection in the mongoDB
     * @param key is the key to find
     * @param valuesInList a BasicDBList with data to store in the mongoDB
     * @return true if there is a document which contains the (key, value)-pair
     */
    public boolean documentExist(String db, String collectionToCheck, String key, BasicDBList valuesInList) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collectionToCheck);

        return mongoCollection.find(new Document(key, valuesInList)).iterator().hasNext();

    }

    /**
     * Checks if a document in the mongoDB contains a key or not.
     * @param db is the name of the mongoDB
     * @param collectionToCheck is the name of the collection in the mongoDB
     * @param key is the key to find
     * @return true if there is a document which contains a key
     */
    public boolean documentExistNew(String db, String collectionToCheck, String key) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collectionToCheck);

        for (Document doc : mongoCollection.find()) {

            if (new JSONObject(doc.toJson()).has(key)) {

                // get a JSONObject from the mongoDB and then filter the correct inner JSONArray
                JSONArray jsonArray = new JSONObject(doc.toJson()).getJSONArray(key);

                if (!jsonArray.isNull(0)) {

                    return true;

                }

            }

        }

        return false;
    }

    /**
     * Checks if a document in the mongoDB contains a (key, value)-pair or not.
     * @param db is the name of the mongoDB
     * @param collectionToCheck is the name of the collection in the mongoDB
     * @param key is the key to find
     * @param valuesInJSON a JSONArray with data to check if it exist in the mongoDB
     * @return true if there is a document which contains the (key, value)-pair
     */
    public boolean documentWithDataExist(String db, String collectionToCheck, String key, JSONArray valuesInJSON) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collectionToCheck);

        // create an mongoDD list/array
        BasicDBList valuesInList = new BasicDBList();

        // fill this array
        for (int i = 0; i < valuesInJSON.length(); i++) {

            BasicDBObject basicDBObject = new BasicDBObject();

            JSONObject currObject = valuesInJSON.getJSONObject(i);

            Iterator keys = currObject.keys();

            while (keys.hasNext()) {

                String currKey = keys.next().toString();

                basicDBObject.put(currKey, currObject.get(currKey).toString());

            }

            valuesInList.add(basicDBObject);

        }

        return mongoCollection.find(new Document(key, valuesInList)).iterator().hasNext();

    }


    /**
     * This method counts the keys of a document in a collection. The '_id' key is ignored
     * @param db is the name of the mongoDB
     * @param collectionToCheck is the name of the collection in the mongoDB
     * @return the number of keys
     */
    public int countCollectionDocuments(String db, String collectionToCheck) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collectionToCheck);

        if (mongoCollection.find().iterator().hasNext()) {

            // minus 1 because ignore the key '_id'
            return (mongoCollection.find().iterator().next().keySet().size() - 1);

        } else {

            return 0;

        }

    }

    /**
     * Checks if a collection contains an user as a value
     * @param db is the name of the mongoDB
     * @param collection is the name of the collection in the mongoDB
     * @param userToFind the username to find
     * @return true if the username exist as a value in the mongoDB
     */
    public boolean findUserByUsername(String db, String collection, String userToFind) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        Document doc = mongoCollection.find(new Document("username", userToFind)).first();

        try {

            return !doc.isEmpty();

        } catch (NullPointerException nullExp) {

            return false;

        }

    }

    /**
     * This method finds an ObjectId in the mongoDB
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param key is the key of an JSON object
     * @return an objectID in a String
     */
    public String findObjectID (String db, String collection, String key) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        Document doc = mongoCollection.find(new Document(key, new BasicDBObject("$exists", true))).first();

        return doc.get("_id").toString();

    }


    /**
     * Find an ObjectId in the mongoDB
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param key is the key of an JSON object
     * @param value is the corresponding value of an JSON object
     * @return an objectID in a String
     */
    public String findObjectID (String db, String collection, String key, String value) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        Document doc = mongoCollection.find(new Document(key, value)).first();

        return doc.get("_id").toString();

    }

    /**
     * Get the current identifier to find a document in the mongoDB
     */
    public BasicDBObject getId() {
        return this.dbIdentifier;
    }

    /**
     * Insert data (key - value) into mongoDB document
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param key is the key of an JSON object
     * @param value is the corresponding value of an JSON object
     * @return an message if this transaction was successful or not
     */
    public String insertDataToMongoDB (String db, String collection, String key, String value) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        // insert data to collection
        mongoCollection.insertOne(new Document(key, value));

        return documentExist(db, collection, key, value) ?
                "The document named " + key + " was insert in the collection." :
                "The document named " + key + " was not insert in the collection.";

    }

    /**
     * Insert data (key - data array) into mongoDB document
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param key is the key of an object
     * @param valuesInJSON a JSONArray with data to store in the mongoDB
     * @return an message if this transaction was successful or not
     */
    public String insertDataToMongoDB (String db, String collection, String key, JSONArray valuesInJSON) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        // create an mongoDB list/array
        BasicDBList valuesInList = new BasicDBList();

        // fill this array
        for (int i = 0; i < valuesInJSON.length(); i++) {

            if (valuesInJSON.get(i).getClass().equals(JSONObject.class)) {

                BasicDBObject basicDBObject = new BasicDBObject();

                JSONObject currObject = valuesInJSON.getJSONObject(i);

                Iterator keys = currObject.keys();

                while (keys.hasNext()) {

                    String currKey = keys.next().toString();

                    UrlValidator objectValidator = new UrlValidator();

                    // get a MDB url Encoder to encode the uri with utf-8
                    SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

                    boolean currKeyIsURI = false;
                    String localId = "";

                    if (objectValidator.isValid(soccomasURLEncoder.encodeUrl(currKey, "UTF-8"))) {
                        // special case if key is a uri

                        String nameSpace = ResourceFactory.createResource(currKey).getNameSpace();

                        localId = ResourceFactory.createResource(currKey).getLocalName();

                        basicDBObject.put("name_space_for_" + localId, nameSpace);

                        currKeyIsURI = true;

                    }

                    if (currObject.get(currKey) instanceof String) {

                        if (currKeyIsURI) {

                            basicDBObject.put(localId, currObject.get(currKey).toString());

                        } else {

                            basicDBObject.put(currKey, currObject.get(currKey).toString());

                        }



                    } else if (currObject.get(currKey) instanceof JSONArray) {

                        if (currKeyIsURI) {

                            JSONArray valuesInInnerJSON = currObject.getJSONArray(currKey);

                            BasicDBList valuesInInnerList = new BasicDBList();

                            for (int j = 0; j < valuesInInnerJSON.length(); j++) {

                                if (valuesInInnerJSON.get(j) instanceof JSONObject) {

                                    BasicDBObject basicInnerDBObject = new BasicDBObject();

                                    JSONObject currInnerObject = valuesInInnerJSON.getJSONObject(j);

                                    Iterator innerKeys = currInnerObject.keys();

                                    while (innerKeys.hasNext()) {

                                        String currInnerKey = innerKeys.next().toString();

                                        if (currInnerObject.get(currInnerKey) instanceof JSONObject) {

                                            BasicDBObject basicInnerInnerDBObject = new BasicDBObject();

                                            JSONObject currInnerInnerObject = currInnerObject.getJSONObject(currInnerKey);

                                            Iterator innerInnerKeys = currInnerInnerObject.keys();

                                            while (innerInnerKeys.hasNext()) {

                                                String currInnerInnerKey = innerKeys.next().toString();

                                                basicInnerInnerDBObject.put(currInnerInnerKey, currInnerInnerObject.get(currInnerInnerKey).toString());

                                            }

                                            basicInnerDBObject.put(currInnerKey, basicInnerInnerDBObject);

                                        } else {

                                            basicInnerDBObject.put(currInnerKey, currInnerObject.get(currInnerKey).toString());

                                        }

                                    }

                                    valuesInInnerList.add(basicInnerDBObject);

                                }

                            }

                            basicDBObject.put(localId, valuesInInnerList);

                        } else {

                            JSONArray valuesInInnerJSON = currObject.getJSONArray(currKey);

                            BasicDBList valuesInInnerList = new BasicDBList();

                            for (int j = 0; j < valuesInInnerJSON.length(); j++) {

                                if (valuesInInnerJSON.get(j) instanceof JSONObject) {

                                    BasicDBObject basicInnerDBObject = new BasicDBObject();

                                    JSONObject currInnerObject = valuesInInnerJSON.getJSONObject(j);

                                    Iterator innerKeys = currInnerObject.keys();

                                    while (innerKeys.hasNext()) {

                                        String currInnerKey = innerKeys.next().toString();

                                        if (currInnerObject.get(currInnerKey) instanceof JSONObject) {

                                            BasicDBObject basicInnerInnerDBObject = new BasicDBObject();

                                            JSONObject currInnerInnerObject = currInnerObject.getJSONObject(currInnerKey);

                                            Iterator innerInnerKeys = currInnerInnerObject.keys();

                                            while (innerInnerKeys.hasNext()) {

                                                String currInnerInnerKey = innerInnerKeys.next().toString();

                                                basicInnerInnerDBObject.put(currInnerInnerKey, currInnerInnerObject.get(currInnerInnerKey).toString());

                                            }

                                            basicInnerDBObject.put(currInnerKey, basicInnerInnerDBObject);

                                        } else {

                                            basicInnerDBObject.put(currInnerKey, currInnerObject.get(currInnerKey).toString());

                                        }

                                    }

                                    valuesInInnerList.add(basicInnerDBObject);

                                }

                            }

                            basicDBObject.put(currKey, valuesInInnerList);

                        }



                    }

                }

                valuesInList.add(basicDBObject);

            } else if (valuesInJSON.get(i).getClass().equals(String.class)) {

                valuesInList.add(valuesInJSON.get(i).toString());

            }

        }

        // one document exist in collection then use this else
        if (mongoCollection.find().iterator().hasNext()) {

            // get the first and only document of the database
            Document doc = mongoCollection.find().iterator().next();

            mongoCollection.updateOne(doc, new Document("$set", new Document(key, valuesInList)));

            return documentExist(db, collection, key, valuesInList) ?
                    "The document named " + key + " was insert in the collection." :
                    "The document named " + key + " was not insert in the collection.";

        } else {

            // insert data to collection
            mongoCollection.insertOne(new Document(key, valuesInList));

            return documentExist(db, collection, key, valuesInList) ?
                    "The document named " + key + " was insert in the collection." :
                    "The document named " + key + " was not insert in the collection.";

        }


    }

    /**
     * Write or update value of certain key of an object with certain objectId in mongoDB document
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param id is objectId of a mongoDB document
     * @param key is the key of an JSON object
     * @param value is the corresponding value of an JSON object
     * @return an message if this transaction was successful or not
     */
    public String insertDataToMongoDB (String db, String collection, String id, String key, String value) {

        // initialize the object id
        setId(id);

        return putDataToMongoDB(db, collection, key, value);

    }

    /**
     * Insert data (keys in data array - value) in mongoDB document (all keys will have the same value)
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param keys contains keys to save
     * @param value is the corresponding value of an JSON object
     * @return an message if this transaction was successful or not
     */
    public String insertDataToMongoDB (String db, String collection, JSONArray keys, String value) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        Document insertDoc = new Document();

        for (int i = 0; i < keys.length(); i++) {

            insertDoc.append(keys.get(i).toString(), value);

        }

        // insert data to collection
        mongoCollection.insertOne(insertDoc);

        for (int i = 0; i < keys.length(); i++) {

            if (!documentExist(db, collection, keys.get(i).toString(), value)) {

                return "The document named " + keys.get(i).toString() + " was not insert in the collection.";

            }

        }

        return  "All documents from the JSONArray were successfully insert in the collection.";

    }

    /**
     * This method pull the corresponding JSONObject from the mongoDB.
     * @param jsonToFindData contains JSON data to find a corresponding JSONObject from the mongoDB
     * @return an JSONObject for a specific input key
     */
    public JSONObject pullDataFromMongoDBWithLocalID(JSONObject jsonToFindData) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase("mdb-prototyp");

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(jsonToFindData.get("connectSID").toString());

        for (Document doc : mongoCollection.find()) {

            if (!(jsonToFindData.has("html_form"))) {

                System.out.println("There is no key named 'html_form' in the jsonToFindData");

            }

            // get a JSONObject from the mongoDB and then filter the correct inner JSONArray
            JSONArray jsonArray = new JSONObject(doc.toJson()).getJSONArray(jsonToFindData.get("html_form").toString());

            // find the correct JSONObject from the mongoDB
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObjectFromMongoDB = jsonArray.getJSONObject(i);

                // is the keyword from the mongoDB equals the input keyword return the JSONObject from the mongoDB
                if (jsonObjectFromMongoDB.get("localID").equals(jsonToFindData.get("localID").toString())) {

                    return jsonObjectFromMongoDB;

                }

            }

        }

        return null;

    }

    /**
     * This method pull the corresponding JSONObject of a partID from the mongoDB.
     * @param jsonToFindData contains JSON data to find a corresponding JSONObject from the mongoDB
     * @return an JSONObject for a specific input key
     */
    public JSONObject pullDataFromMongoDBWithPartID(JSONObject jsonToFindData) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase("mdb-prototyp");

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(jsonToFindData.get("connectSID").toString());

        for (Document doc : mongoCollection.find()) {

            if (!(jsonToFindData.has("html_form"))) {

                System.out.println("There is no key named 'html_form' in the jsonToFindData");

            }

            // get a JSONObject from the mongoDB and then filter the correct inner JSONArray
            JSONArray jsonArray = new JSONObject(doc.toJson()).getJSONArray(jsonToFindData.get("html_form").toString());

            // find the correct JSONObject from the mongoDB
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObjectFromMongoDB = jsonArray.getJSONObject(i);

                if (jsonToFindData.has("partID")) {

                    // is the keyword from the mongoDB equals the input keyword return the JSONObject from the mongoDB
                    if (jsonObjectFromMongoDB.get("localID").equals(jsonToFindData.get("partID").toString())) {

                        return jsonObjectFromMongoDB;

                    }

                } else {

                    System.out.println("WARN: There is no partID in Input!");

                }

            }

        }

        return null;

    }

    /**
     * Read data from a document in the mongoDB
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @return an entry in the mongoDB collection
     */
    public String pullDataFromMongoDB(String db, String collection) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        // get the first document of the database
        Document doc = mongoCollection.find(this.dbIdentifier).first();

        // convert document to String
        return doc.toJson();

    }

    /**
     * Read data from a document in the mongoDB
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param id is objectId of a mongoDB document
     * @return an entry in the mongoDB collection
     */
    public String pullDataFromMongoDB(String db, String collection, String id) {

        // initialize the object id
        setId(id);

        return pullDataFromMongoDB(db, collection);

    }

    /**
     * This method pull the corresponding JSONObject from the mongoDB.
     * @param jsonToFindData contains JSON data to find a corresponding JSONArray from the mongoDB
     * @return an JSONObject for a specific input key
     */
    public JSONObject pullJSONObjectFromMongoDB(JSONObject jsonToFindData) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase("mdb-prototyp");

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(jsonToFindData.get("html_form").toString());

        for (Document doc : mongoCollection.find()) {

            // get a JSONObject from the mongoDB and then filter the correct inner JSONArray

            return new JSONObject(doc.toJson());

        }

        return null;

    }

    /**
     * This method pull the corresponding JSONArray from the mongoDB.
     * @param jsonToFindData contains JSON data to find a corresponding JSONArray from the mongoDB
     * @return an JSONArray for a specific input key
     */
    public JSONArray pullListFromMongoDB(JSONObject jsonToFindData) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase("mdb-prototyp");

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(jsonToFindData.get("connectSID").toString());

        for (Document doc : mongoCollection.find()) {

            // get a JSONObject from the mongoDB and then filter the correct inner JSONArray

            return new JSONObject(doc.toJson()).getJSONArray(jsonToFindData.get("html_form").toString());

        }

        return null;

    }

    /**
     * Write or update data in a document in the mongoDB
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param key is the key of an JSON object
     * @param value is the corresponding value of an JSON object
     * @return an message if this transaction was successful or not
     */
    public String putDataToMongoDB (String db, String collection, String key, String value) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        // update the dataset
        UpdateResult result = mongoCollection.updateOne(this.dbIdentifier, new Document("$set", new Document(key, value)));

        if (result.getModifiedCount() > 0) {

            return "Database was successfully updated.";

        } else if (result.getModifiedCount() == 0) {

            return "Database is already up to date.";

        } else {

            return "Couldn't update database.";

        }


    }

    /**
     * Write or update data in a document in the mongoDB
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param key is the key of an JSON object
     * @param valuesInJSON a JSONArray with data to store in the mongoDB
     * @return an message if this transaction was successful or not
     */
    public String putDataToMongoDB (String db, String collection, String key, JSONArray valuesInJSON) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        for (Document doc : mongoCollection.find()) {

            // get a JSONObject from the mongoDB and then filter the correct inner JSONArray
            JSONArray mongoDBJSONArray = new JSONObject(doc.toJson()).getJSONArray(key);

            if (!mongoDBJSONArray.isNull(0)) {

                for (int i = 0; i < valuesInJSON.length(); i++) {

                    boolean newComponent = true;

                    for (int j = 0; j < mongoDBJSONArray.length(); j++) {

                        if (mongoDBJSONArray.getJSONObject(j).get("individualID").toString().equals(valuesInJSON.getJSONObject(i).get("individualID").toString())) {

                            newComponent = false;

                            mongoDBJSONArray.put(j, valuesInJSON.get(i));

                            // finish loops for better runtime
                            /*j = mongoDBJSONArray.length();
                            i = valuesInJSON.length();*/

                        }

                    }

                    if (newComponent) {

                        mongoDBJSONArray.put(valuesInJSON.get(i));

                    }

                }

                // create an mongoDB list/array
                BasicDBList valuesInList = new BasicDBList();

                // fill this array
                for (int i = 0; i < mongoDBJSONArray.length(); i++) {

                    if (mongoDBJSONArray.get(i).getClass().equals(JSONObject.class)) {

                        BasicDBObject basicDBObject = new BasicDBObject();

                        JSONObject currObject = mongoDBJSONArray.getJSONObject(i);

                        Iterator keys = currObject.keys();

                        while (keys.hasNext()) {

                            String currKey = keys.next().toString();

                            UrlValidator objectValidator = new UrlValidator();

                            // get a MDB url Encoder to encode the uri with utf-8
                            SOCCOMASURLEncoder soccomasURLEncoder = new SOCCOMASURLEncoder();

                            boolean currKeyIsURI = false;
                            String localId = "";

                            if (objectValidator.isValid(soccomasURLEncoder.encodeUrl(currKey, "UTF-8"))) {
                                // special case if key is a uri

                                String nameSpace = ResourceFactory.createResource(currKey).getNameSpace();

                                localId = ResourceFactory.createResource(currKey).getLocalName();

                                basicDBObject.put("name_space_for_" + localId, nameSpace);

                                currKeyIsURI = true;

                            }

                            if (currObject.get(currKey) instanceof String) {

                                if (currKeyIsURI) {

                                    basicDBObject.put(localId, currObject.get(currKey).toString());

                                } else {

                                    basicDBObject.put(currKey, currObject.get(currKey).toString());

                                }



                            } else if (currObject.get(currKey) instanceof JSONArray) {

                                if (currKeyIsURI) {

                                    JSONArray valuesInInnerJSON = currObject.getJSONArray(currKey);

                                    BasicDBList valuesInInnerList = new BasicDBList();

                                    for (int j = 0; j < valuesInInnerJSON.length(); j++) {

                                        if (valuesInInnerJSON.get(j) instanceof JSONObject) {

                                            BasicDBObject basicInnerDBObject = new BasicDBObject();

                                            JSONObject currInnerObject = valuesInInnerJSON.getJSONObject(j);

                                            Iterator innerKeys = currInnerObject.keys();

                                            while (innerKeys.hasNext()) {

                                                String currInnerKey = innerKeys.next().toString();

                                                basicInnerDBObject.put(currInnerKey, currInnerObject.get(currInnerKey).toString());

                                            }

                                            valuesInInnerList.add(basicInnerDBObject);

                                        }

                                    }

                                    basicDBObject.put(localId, valuesInInnerList);

                                } else {

                                    JSONArray valuesInInnerJSON = currObject.getJSONArray(currKey);

                                    BasicDBList valuesInInnerList = new BasicDBList();

                                    for (int j = 0; j < valuesInInnerJSON.length(); j++) {

                                        if (valuesInInnerJSON.get(j) instanceof JSONObject) {

                                            BasicDBObject basicInnerDBObject = new BasicDBObject();

                                            JSONObject currInnerObject = valuesInInnerJSON.getJSONObject(j);

                                            Iterator innerKeys = currInnerObject.keys();

                                            while (innerKeys.hasNext()) {

                                                String currInnerKey = innerKeys.next().toString();

                                                basicInnerDBObject.put(currInnerKey, currInnerObject.get(currInnerKey).toString());

                                            }

                                            valuesInInnerList.add(basicInnerDBObject);

                                        }

                                    }

                                    basicDBObject.put(currKey, valuesInInnerList);

                                }



                            }

                        }

                        valuesInList.add(basicDBObject);

                    } else if (mongoDBJSONArray.get(i).getClass().equals(String.class)) {

                        valuesInList.add(mongoDBJSONArray.get(i).toString());

                    }

                }

                // update the dataset
                UpdateResult result = mongoCollection.updateOne(doc, new Document("$set", new Document(key, valuesInList)));

                if (result.getModifiedCount() > 0) {

                    return "Database was successfully updated.";

                } else if (result.getModifiedCount() == 0) {

                    return "Database is already up to date.";

                } else {

                    return "Couldn't update database.";

                }

            }

        }

        return "Couldn't update database.";

    }

    /**
     * This method saves the input data until the transition 'create new mdb entry' is started or the transition is
     * aborted.
     * @param jsonInputObject contains the input for a later calculation
     */
    public void putJSONInputObjectInMongoDB(JSONObject jsonInputObject) {

        JSONArray jsonInputArray = new JSONArray();

        jsonInputArray.put(jsonInputObject);

        int count = countCollectionDocuments("mdb-prototyp", jsonInputObject.get("html_form").toString());

        if (count > 0) {

            System.out.println("Collection already exist");

            insertDataToMongoDB("mdb-prototyp", jsonInputObject.get("html_form").toString(), String.valueOf(count), jsonInputArray);

        } else {

            createCollection("mdb-prototyp", jsonInputObject.get("html_form").toString());

            insertDataToMongoDB("mdb-prototyp", jsonInputObject.get("html_form").toString(), "0", jsonInputArray);

        }

    }

    /**
     * This method deletes data in a document in the mongoDB
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param key is the key of an JSON object
     * @return an message if this transaction was successful or not
     */
    public String removeDataToMongoDB (String db, String collection, String key) {

        // get the database from the mongodb
        MongoDatabase database = this.mongoClient.getDatabase(db);

        // get the collection from the database
        MongoCollection<Document> mongoCollection = database.getCollection(collection);

        // update the dataset
        UpdateResult result = mongoCollection.updateOne(this.dbIdentifier, new Document("$unset", new Document(key, new BasicDBObject("$exists", true))));

        if (result.getModifiedCount() > 0) {

            return "Database was successfully updated.";

        } else if (result.getModifiedCount() == 0) {

            return "Database is already up to date.";

        } else {

            return "Couldn't update database.";

        }

    }


    /**
     * Write or update value of certain key of an object with certain objectId in mongoDB document
     * @param db is the name of a database in the mongoDB
     * @param collection is the name of a collection in the database
     * @param id is objectId of a mongoDB document
     * @param key is the key of an JSON object
     * @return an message if this transaction was successful or not
     */
    public String removeDataToMongoDB (String db, String collection, String id, String key) {

        // initialize the object id
        setId(id);

        return removeDataToMongoDB(db, collection, key);

    }

    /**
     * Set the identifier to find a document in the mongoDB
     * @param id is objectId of a mongoDB document
     */
    public void setId(String id) {
        this.dbIdentifier = new BasicDBObject("_id", new ObjectId(id));
    }

}