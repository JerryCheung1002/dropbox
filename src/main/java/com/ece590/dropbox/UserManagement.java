package com.ece590.dropbox;

import com.amazonaws.util.json.JSONObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.UUID;

/**This class contains the operation on AWSS3 such as authentication, adding and deleting bucket for users, uploading
 * and downloading files.
 * Created by JerryCheung on 3/27/16.
 */
public class UserManagement {
    private static final String HOST = "54.210.164.65";
    private static final int PORT = 27017;
    private static final String EMAIL = "email";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DB_EMAIL_PASSWORD = "db_email_password";
    //private static final String DB_EMAIL_PASSWORD = "db_email_password_test";
    private static final String DB_META = "db_meta";
    //private static final String DB_META = "db_meta_test";
    private static final String COLLECTION_EMAIL_PASSWORD_PAIR = "collection_email_password_pair";
    private static final String COLLECTION_USAGE = "collection_usage";
    private static final String MSG = "message";
    private static final String SUCC = "success";
    private static final String FAIL = "fail";
    private static final String FIRSTNAME = "firstname";
    private static final String LASTNAME = "lastname";
    private static final String BUCKETNAME = "bucketname";
    private static final String CURUSAGE = "currentusage";
    private static final String LIMIT = "limit";

    private static final String DB_TEST = "db_test";
    private static final String COLLECTION_TEST = "collection_test";

  /*  public static void main(String[] args) {
        String firstname1 = "Jianyu1";
        String lastname1 = "Zhang";
        String password1 = "123456";
        String email1 = "foo1@example.com";
        String res = UserManagement.addNewUser(firstname1, lastname1, password1, email1);
        System.out.println(res + "");
        System.out.println("The bucket name is" + getBucket(email1));

        String firstname2 = "Jianyu2";
        String lastname2 = "Zhang";
        String password2 = "123456";
        String email2 = "foo2@example.com";
        JSONObject res2 = UserManagement.addNewUser(firstname2, lastname2, password2, email2);
        System.out.println(res2 + "");
        System.out.println("The bucket name is" + getBucket(email2));

        String firstname3 = "Jianyu3";
        String lastname3 = "Zhang";
        String password3 = "123456";
        String email3 = "foo3@example.com";
        JSONObject res3 = UserManagement.addNewUser(firstname3, lastname3, password3, email3);
        System.out.println(res3 + "");
        System.out.println("The bucket name is" + getBucket(email3));

        MongoClient mongoClient = new MongoClient(HOST, PORT);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_EMAIL_PASSWORD);
        MongoCollection collection = mongoDatabase.getCollection(COLLECTION_EMAIL_PASSWORD_PAIR);
        String tmp = "Zhang";
        Document queryObject = new Document(LASTNAME, tmp);
        FindIterable<Document> searchRes = collection.find(queryObject);
        for (Document doc : searchRes) {
            System.out.println("" + doc);
        }
    }       */

    public static void main(String[] args) {
        String res = UserManagement.addNewUser("Jianyu", "Zhang", "testemail", "123456");
        System.out.println(res + "");
    }
    /**
     * Just for test
     * @return
     */
    public static String testGreeting() {
        return "This is the test function in UserManagement class";
    }

    /**
     * Just for testing whether the tomcat server can connect to the database
     * @param firstname
     * @param lastname
     * @return
     */
    public static String addTestObj(String firstname, String lastname) {
        MongoClient mongoClient = new MongoClient(HOST, PORT);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_TEST);
        MongoCollection mongoCollection = mongoDatabase.getCollection(COLLECTION_TEST);
        Document doc = new Document("firstname", firstname).append("lastname", lastname);
        mongoCollection.insertOne(doc);
        return SUCC;
    }


    /**
     * authenticate user based on the provided email and password
     */
    public static JSONObject authenticate(String email, String password) {
        System.out.println("in authenticate(String, String)");
        JSONObject tuple = new JSONObject();
        JSONObject res = null;
        try {
            tuple.put(EMAIL, email);
            tuple.put(PASSWORD, password);
            res = authenticate(tuple);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * authenticate user based on the provided JSONObject
     */
    private static JSONObject authenticate(JSONObject tuple) throws Exception{
        JSONObject res = new JSONObject();
        String savedPassword = null;
        System.out.println("------in authenticate(JSONObject)");
        MongoClient mongoClient = new MongoClient(HOST, PORT);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_EMAIL_PASSWORD);
        MongoCollection collection = mongoDatabase.getCollection(COLLECTION_EMAIL_PASSWORD_PAIR);
        //BasicDBObject queryObject = new BasicDBObject();
        //queryObject.put(USERNAME, tuple.get(USERNAME));
        Document queryObject = new Document(EMAIL, tuple.get(EMAIL));
        FindIterable<Document> searchRes = collection.find(queryObject);
        for (Document document : searchRes) {
            savedPassword = document.getString(PASSWORD);
            System.out.println("Saved password = " + savedPassword);
        }
        if (tuple.getString(PASSWORD).equals(savedPassword)) {
            res.put(MSG, SUCC);
        } else {
            res.put(MSG, FAIL);
        }
        return res;
    }

    /**
     * add new user based on provided firstname, lastname, email
     */
    public static String addNewUser(String firstname, String lastname, String password, String email) {
        JSONObject res = null;
        String bucketname = lastname.toLowerCase() + UUID.randomUUID().toString();
        //BucketMaking bucketMaking = new BucketMaking();
        //bucketMaking.createNewBucket(lastname + UUID.randomUUID().toString());
        System.out.println("---------in addNewUser(String, String, String)");
        JSONObject userInfo = new JSONObject();
        try {
            userInfo.put(FIRSTNAME, firstname);
            userInfo.put(LASTNAME, lastname);
            userInfo.put(PASSWORD, password);
            userInfo.put(EMAIL, email);
            userInfo.put(BUCKETNAME, bucketname);
            res = addNewUser(userInfo);
            AwsOperation.createBucket(bucketname);
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception in addNewUser";
        }
        return res.toString();
    }

    /**
     * insert the new user info into the database
     * @param userInfo
     * @return JSONObject containing success or fail message
     * @throws Exception
     */
    private static JSONObject addNewUser(JSONObject userInfo) throws Exception {
        JSONObject res = new JSONObject();
        boolean isExisted = false;
        Document newUserDoc = new Document(FIRSTNAME, userInfo.getString(FIRSTNAME)).append(LASTNAME, userInfo.getString(LASTNAME))
                .append(PASSWORD, userInfo.getString(PASSWORD)).append(EMAIL, userInfo.getString(EMAIL))
                .append(BUCKETNAME, userInfo.getString(BUCKETNAME));
        MongoClient mongoClient = new MongoClient(HOST, PORT);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_EMAIL_PASSWORD);
        MongoCollection mongoCollection = mongoDatabase.getCollection(COLLECTION_EMAIL_PASSWORD_PAIR);
        Document queryObject = new Document(EMAIL, userInfo.get(EMAIL));
        FindIterable<Document> searchRes = mongoCollection.find(queryObject);
        for (Document document : searchRes) {
            if ((document.getString(EMAIL)).equals(userInfo.getString(EMAIL))) {
                isExisted = true;
            }
        }
        if (isExisted) {
            res.put(MSG, SUCC);
        } else {
            Double curUsage = new Double(0.0);
            Double limit = new Double(1024.0);
            mongoCollection.insertOne(newUserDoc);
            mongoDatabase = mongoClient.getDatabase(DB_META);
            mongoCollection = mongoDatabase.getCollection(COLLECTION_USAGE);
            Document newDoc = new Document(EMAIL, userInfo.getString(EMAIL)).append(CURUSAGE, curUsage).append(LIMIT, limit);
            mongoCollection.insertOne(newDoc);
            //send email to the user
            //AmazonSESSample aSES=new AmazonSESSample();
            //String response=aSES.sendRegisterMail(user.getString("firstname"),user.getString("email"),password);
            res.put(MSG, SUCC);
        }
        return res;
    }

    /**
     * get the name of the bucket assocaited with the given email
     */
    public static String getBucket(String email) {
        String res = null;
        System.out.println("---------in getBucket(String)");
        try {
            MongoClient mongoClient = new MongoClient(HOST, PORT);
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_EMAIL_PASSWORD);
            MongoCollection mongoCollection = mongoDatabase.getCollection(COLLECTION_EMAIL_PASSWORD_PAIR);
            Document queryObject = new Document(EMAIL, email);
            FindIterable<Document> searchRes = mongoCollection.find(queryObject);
            for (Document document : searchRes) {
                res = document.getString(BUCKETNAME);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }
}