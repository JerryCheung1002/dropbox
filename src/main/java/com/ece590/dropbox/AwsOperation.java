package com.ece590.dropbox;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sun.jersey.core.header.FormDataContentDisposition;
import org.bson.Document;

import java.io.File;
import java.text.DecimalFormat;
/*
 * Created by JerryCheung on 3/27/16.
 */
public class AwsOperation {

    private static final String SUCC = "success";
    private static final String FAIL = "fail";
    private static final String LIMIT_EXCEED = "no_enough_space";

    private static final String HOST = "54.210.164.65";
    private static final int PORT = 27017;
    private static final String DB_EMAIL_PASSWORD = "db_email_password";
    private static final String DB_META = "db_meta";
    private static final String COLLECTION_EMAIL_PASSWORD_PAIR = "collection_email_password_pair";
    private static final String COLLECTION_USAGE = "collection_usage";
    private static final String COLLECTION_FILE_META = "collection_file_metadata";
    private static final String USERNAME = "username";
    private static final String FILENAME = "filename";
    private static final String BUCKETNAME = "bucketname";
    private static final String FILESIZE = "filesize";
    private static final String LASTMODIFIED = "lastmodified";
    private static final String OWNER = "owner";
    //private static final String DOWNLOADLINK = "download_link";
    private static final String EMAIL = "email";
    private static final String CURUSAGE = "currentusage";
    private static final String LIMIT = "limit";
    private static final String OBJECTS_ENTRIES = "objects_entries";



    public static void main(String[] args) {
        AmazonS3Client s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        String bucketname = "zhangcf695887-b90f-4e2a-906b-09416d59f413";
        String objectname = "ref.txt";
        String downloadlink = s3Client.getResourceUrl(bucketname, objectname);
        System.out.println(downloadlink + "");
    }
    /**
     * Create a new bucket with the given bucketname
     * @param bucketname
     */
    public static void createBucket(String bucketname) {
        System.out.println("-------in createBucket(String)");
        AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        s3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
        try {
            if (!(s3Client.doesBucketExist(bucketname))) {
                s3Client.createBucket(bucketname);
            }
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    /**
     * Add the file metadata to the database
     * @param meta
     * @return
     */
    public static String addFileMeta(JSONObject meta) {
        //Document newFileMetaDoc = new Document(USERNAME, meta.get(USERNAME)).append(FILENAME, meta.get(FILENAME)).append()
        MongoClient mongoClient = new MongoClient(HOST, PORT);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_META);
        MongoCollection mongoCollection = mongoDatabase.getCollection(COLLECTION_USAGE);
        String res = FAIL;
        try {
            /*update user useage info*/
            Document queryObject = new Document(EMAIL, meta.get(EMAIL));
            FindIterable<Document> searchRes = mongoCollection.find(queryObject);
            double curUsage = 0;
            double limit = 0;
            for (Document document : searchRes) {
                if ((document.getString(EMAIL)).equals(meta.getString(EMAIL))) {
                    curUsage = document.getDouble(CURUSAGE);
                    limit = document.getDouble(LIMIT);
                }
            }
            double newUsage = curUsage + meta.getDouble(FILESIZE);
            if (newUsage > limit) {
                res = LIMIT_EXCEED;
            } else {
                mongoCollection.updateOne(new Document(EMAIL, meta.get(EMAIL)), new Document("$set", new Document(CURUSAGE, new Double(newUsage))));
                            /*insert new file meta data into the collection*/
                mongoCollection = mongoDatabase.getCollection(COLLECTION_FILE_META);
                Document newFileMetaDoc = new Document(EMAIL, meta.get(EMAIL)).append(FILENAME, meta.get(FILENAME))
                        .append(BUCKETNAME, meta.get(BUCKETNAME)).append(FILESIZE, meta.get(FILESIZE)).append(LASTMODIFIED, meta.get(LASTMODIFIED))
                        .append(OWNER, meta.get(OWNER));//.append(DOWNLOADLINK, meta.get(DOWNLOADLINK));
                mongoCollection.insertOne(newFileMetaDoc);
                res = SUCC;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * return success or fail.
     * delete the metadata associated with the specified object from the database.
     * also update the user usage data in the database.
     * @param email
     * @param objectname
     * @return
     */
    public static String deleteFileMeta(String email, String objectname) {
        String res = FAIL;
        MongoClient mongoClient = new MongoClient(HOST, PORT);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_META);
        MongoCollection mongoCollection = mongoDatabase.getCollection(COLLECTION_FILE_META);
        try {
            Document queryObject = new Document(EMAIL, email).append(FILENAME, objectname);
            FindIterable<Document> searchRes = mongoCollection.find(queryObject);
            Double fileSz = new Double(0.0);
            for (Document document : searchRes) {
                System.out.println("******************file size is: " + document.getString(FILESIZE) + "*************");
                fileSz = Double.parseDouble(document.getString(FILESIZE));
            }
            /*delete the info from the collection*/
            mongoCollection.deleteOne(queryObject);
            mongoCollection = mongoDatabase.getCollection(COLLECTION_USAGE);
            queryObject = new Document(EMAIL, email);
            searchRes = mongoCollection.find(queryObject);
            double curUsage = 0.0;
            for (Document document : searchRes) {
                curUsage = document.getDouble(CURUSAGE);
            }
            double newUsage = curUsage - fileSz;
            mongoCollection.updateOne(new Document(EMAIL, email), new Document("$set", new Document(CURUSAGE, new Double(newUsage))));
            res = SUCC;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * return a jsonobject containing the objects and usage info of a given user.
     * @param email
     * @return
     */
    public static JSONObject getObjectsMetaData(String email) {
        MongoClient mongoClient = new MongoClient(HOST, PORT);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DB_META);
        MongoCollection mongoCollection = mongoDatabase.getCollection(COLLECTION_FILE_META);
        JSONArray jsonObjectsMeta = new JSONArray();
        JSONObject jsonRes = new JSONObject();
        try {
            /*get objects metadata from database*/
            Document queryObject = new Document(EMAIL, email);
            FindIterable<Document> searchRes = mongoCollection.find(queryObject);
            for (Document document : searchRes) {
                JSONObject jsonTemp = new JSONObject();
                jsonTemp.put(EMAIL, document.getString(EMAIL));
                jsonTemp.put(FILENAME, document.getString(FILENAME));
                jsonTemp.put(BUCKETNAME, document.getString(BUCKETNAME));
                jsonTemp.put(FILESIZE, document.get(FILESIZE));
                jsonTemp.put(LASTMODIFIED, document.get(LASTMODIFIED));
                jsonTemp.put(OWNER, document.getString(OWNER));
                //jsonTemp.put(DOWNLOADLINK, document.getString(DOWNLOADLINK));
                jsonObjectsMeta.put(jsonTemp);
            }
            jsonRes.put(OBJECTS_ENTRIES, jsonObjectsMeta);
            /*get user storage information from database*/
            mongoCollection = mongoDatabase.getCollection(COLLECTION_USAGE);
            searchRes = mongoCollection.find(queryObject);
            for (Document document : searchRes) {
                jsonRes.put(CURUSAGE, document.getDouble(CURUSAGE));
                jsonRes.put(LIMIT, document.getDouble(LIMIT));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonRes;
    }

    /**
     * upload the file to S3. Return success or fail to indicate whether upload successfully.
     * @param file
     * @param formDataContentDispositon
     * @param email
     * @return
     */
    public static String addObject(File file, FormDataContentDisposition formDataContentDispositon, String email) {
        System.out.println("------in addObject(File, FormDataContentDisposition, String");
        String res = FAIL;
        AmazonS3Client s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        s3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
        String bucketname = UserManagement.getBucket(email);
        try {
            PutObjectRequest putRequest = new PutObjectRequest(bucketname, formDataContentDispositon.getFileName(), file);
            putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            s3Client.putObject(putRequest);
            System.out.println("Finish uploading file to S3");
            ObjectMetadata objectMetadata = s3Client.getObjectMetadata(bucketname, formDataContentDispositon.getFileName());
            JSONObject jsonMeta = new JSONObject();
            DecimalFormat decimalFormat = new DecimalFormat("###.##");
            jsonMeta.put(EMAIL, email);
            jsonMeta.put(FILENAME, formDataContentDispositon.getFileName());
            jsonMeta.put(BUCKETNAME, bucketname);
            jsonMeta.put(FILESIZE, decimalFormat.format(((double)objectMetadata.getContentLength()) / (1024 * 1024)));
            jsonMeta.put(LASTMODIFIED, objectMetadata.getLastModified());
            jsonMeta.put(OWNER, email);
            //jsonMeta.put(DOWNLOADLINK, s3Client.getResourceUrl(bucketname, formDataContentDispositon.getFileName()));
            System.out.println("uploaded file metadata: " + jsonMeta);
            res = addFileMeta(jsonMeta);
            if (!res.equals(SUCC)) {
                System.out.println("No enough user space");
                s3Client.deleteObject(bucketname, formDataContentDispositon.getFileName());
            }
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * delete the specified object from the s3 bucket
     * @param email
     * @param objectname
     * @return succ or fail
     */
    public static String deleteObject(String email, String objectname) {
        System.out.println("------in deleteObject(String, String)");
        String res = FAIL;
        String tmp = deleteFileMeta(email, objectname);
        try {
            if (tmp.equals(SUCC)) {
                AmazonS3Client s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
                String bucketname = UserManagement.getBucket(email);
                s3Client.deleteObject(bucketname, objectname);
                res = SUCC;
            }
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * return the objects and usage info of a specified user
     * @param email
     * @return
     */
    public static String viewObjects(String email) {
        JSONObject jsonRes = getObjectsMetaData(email);
        return jsonRes.toString();
    }

    /**
     * return the download link of the specified user and object.
     * @param email
     * @param objectname
     * @return
     */
    public static String getLink(String email, String objectname) {
        String res = "";
        try {
            AmazonS3Client s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
            String bucketname = UserManagement.getBucket(email);
            res = s3Client.getResourceUrl(bucketname, objectname);
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }
}