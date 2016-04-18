package com.ece590.dropbox;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.json.JSONObject;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**Class indicates the architecture of dropobox
 * Created by JerryCheung on 4/2/16.
 */
@Path("/dropbox")
public class DropboxService {

    private static final String SUCC = "success";
    private static final String FAIL = "fail";
    private static final String LIMIT_EXCEED = "no_enough_space";

    @GET
    @Path("/hello/{param}")
    public Response getMsg(@PathParam("param") String msg) {
        String output = "Jersey say : " + msg;
        return Response.status(200).entity(output).build();
    }

    @GET
    @Path("/test")
    public String test() {
        return UserManagement.testGreeting();
    }

    @GET
    @Path("/test_add/{first}/{last}")
    public String testAddObj(@PathParam("first") String firstname, @PathParam("last") String lastname) {
        return UserManagement.addTestObj(firstname, lastname);
    }

    @GET
    @Path("/test_create_bucket")
    public String testCreateBucket() {
        String res = "Bucket Create Successfully";
        String bucketName = "jerrycheungece590cloudcomputing";
        AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());
        //System.out.println("Finish loading----");
        s3client.setRegion(Region.getRegion(Regions.US_EAST_1));

        try {
            if(!(s3client.doesBucketExist(bucketName)))
            {
                // Note that CreateBucketRequest does not specify region. So bucket is
                // created in the region specified in the client.
                s3client.createBucket(new CreateBucketRequest(
                        bucketName));
            }
            // Get location.
            String bucketLocation = s3client.getBucketLocation(new GetBucketLocationRequest(bucketName));
            System.out.println("bucket location = " + bucketLocation);

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
        return res;
    }

    @GET
    @Path("/authentication/{email}/{password}")
    public String authenticateUser(@PathParam("email") String email, @PathParam("password") String password) {
        System.out.println("------authenticateUser()");
        JSONObject res = UserManagement.authenticate(email, password);
        return res.toString();
    }


    //@GET   used for testing by formatting the url directly in a browser
    @POST
    @Path("/registeration/{firstname}/{lastname}/{email}/{password}")
    public String addNewUser(@PathParam("firstname") String firstname, @PathParam("lastname") String lastname, @PathParam("email") String email, @PathParam("password") String password) {
        System.out.println("------addNewUser()");
        //String res = firstname + lastname + email + password;
        String res = UserManagement.addNewUser(firstname, lastname, password, email);
        return res;
    }

    @GET
    @Path("/view/{email}")
    public String viewFiles(@PathParam("email") String email) {
        System.out.println("------viewFiles()");
        JSONObject res = AwsOperation.getObjectsMetaData(email);
        return res.toString();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile (@FormDataParam("file") File file, @FormDataParam("file") FormDataContentDisposition formDataContentDisposition,
                            @QueryParam("email") String email) {
        System.out.println("------uploadFile()");
        String res = AwsOperation.addObject(file, formDataContentDisposition, email);
        try {
            if (res.equals(FAIL)) {
                URI redirection = new URI("../upload_fail.html");
                throw new WebApplicationException(Response.temporaryRedirect(redirection).build());
            }
            else if (res.equals(LIMIT_EXCEED)) {
                URI redirection = new URI("../dropbox_main_page.html");
                throw new WebApplicationException(Response.temporaryRedirect(redirection).build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String output = "File uploaded successfully";
        return Response.status(200).entity(output).build();
    }

    @GET
    @Path("/download/{email}/{objectname}")      //http://127.0.0.1:8080/rest/dropbox/download/{email}/{objectname}
    public Response downloadFile(@PathParam("email") String email, @PathParam("objectname") String objectname) throws IOException {
        String bucketName = UserManagement.getBucket(email);
        //String bucketName = "zhangcf695887-b90f-4e2a-906b-09416d59f413";
        AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, objectname));
        InputStream objectData = object.getObjectContent();
        String filepath = "/tmp/" + bucketName;
        File f = new File(filepath);
        if (!f.exists()) {
            f.mkdirs();
        }
        File file = new File(f, objectname);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file);
        int ch = 0;
        try {
            while ((ch = objectData.read()) != -1) {
                fos.write(ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fos.close();
            objectData.close();
        }
        Response.ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition", "attachment; filename=\"1.jpg\"");
        return response.build();
    }

    @GET
    @Path("/getlink/{email}/{objectname}")
    public String getFileLink(@PathParam("email") String email, @PathParam("objectname") String objectname) {
        System.out.println("------downloadFile()");
        String res = AwsOperation.getLink(email, objectname);
        return res;
    }

    @DELETE
    @Path("/deletion/{email}/{objectname}")
    public String deleteFile(@PathParam("email") String email, @PathParam("objectname") String objectname) {
        System.out.println("------deleteFile()");
        AwsOperation.deleteObject(email, objectname);
        return "success";
    }
}
