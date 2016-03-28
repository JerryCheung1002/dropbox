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
    public static final String HOST = "localhost";
    public static final int PORT = 27017;
    public static final String EMAIL = "email";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DB_EMAIL_PASSWORD = "db_email_password";
    public static final String DB_META = "db_meta";
    public static final String COLLECTION_EMAIL_PASSWORD_PAIR = "collection_email_password_pair";
    public static final String COLLECTION_USAGE = "collection_usage";
    public static final String MSG = "message";
    public static final String SUCC = "success";
    public static final String FAIL = "fail";
    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";
    public static final String BUCKETNAME = "bucketname";
    public static final String CURUSAGE = "currentusage";
    public static final String LIMIT = "limit";

    public static void main(String[] args) {

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
    public static JSONObject addNewUser(String firstname, String lastname, String password, String email) {
        JSONObject res = null;
        String bucketname = lastname + UUID.randomUUID().toString();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
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
            res.put(MSG, FAIL);
        }
        return res;
    }
}