import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by JerryCheung on 4/6/16.
 */
public class TestFile {
    private static String bucketName = "zhangcf695887-b90f-4e2a-906b-09416d59f413";
    private static String key        = "1.jpg";

    public static void main(String[] args) {
        String res = "fail";
        try {
            res = TestFile.getFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(res + "");
    }

    public static String createFile() {
        String res = "File created successfully!";
        String filepath = "/tmp/" + bucketName;
        File f = new File(filepath);
        if (!f.exists()) {
            System.out.println("Path is not existed!");
            f.mkdirs();
        }
        String filename = key;
        File file = new File(f, filename);
        if (!file.exists()) {
            System.out.println("File is not existed!");
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    public static String getFile() throws Exception{
        //File file = new File(FILE_PATH);
        //ResponseBuilder response = Response.ok((Object) file);
        //response.header("Content-Disposition", "attachment; filename=\"test_text_file.txt\"");
        //return response.build();
        AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));
        InputStream objectData = object.getObjectContent();
        String filepath = "/tmp/" + bucketName;
        File f = new File(filepath);
        if (!f.exists()) {
            System.out.println("Dir is not existed!");
            f.mkdirs();
        }
        String filename = key;
        File file = new File(f, filename);
        if (!file.exists()) {
            System.out.println("File is not existed!");
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
        file.delete();    //delete the temporary file
        //Process the objectData stream. Make a copy of the object under the path "/tmp/bucketName"
        return "download success";
    }
}
