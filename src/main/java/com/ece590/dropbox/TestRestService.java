package com.ece590.dropbox;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
/**
 * Created by JerryCheung on 4/2/16.
 */

@Path("/test")
public class TestRestService {

    private final static String FILE_PATH = "/tmp/test.txt";
    private static String bucketName = "zhangcf695887-b90f-4e2a-906b-09416d59f413";
    private static String key        = "1.jpg";

    @GET
    @Path("/greeting/{param}")
    public String getMsg(@PathParam("param") String msg) {
        String output = "Jersey say : " + msg;
        return output;
    }

    @GET
    @Path("/download")
    public Response getFile() throws IOException {
        //File file = new File(FILE_PATH);
        //ResponseBuilder response = Response.ok((Object) file);
        //response.header("Content-Disposition", "attachment; filename=\"test_text_file.txt\"");
        //return response.build();
        //System.out.println("------getFile()");
        AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));
        InputStream objectData = object.getObjectContent();
        String filepath = "/tmp/" + bucketName;
        File f = new File(filepath);
        if (!f.exists()) {
            f.mkdirs();
        }
        String filename = key;
        File file = new File(f, filename);
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
}
