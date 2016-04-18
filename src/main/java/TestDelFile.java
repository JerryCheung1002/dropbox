import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Created by JerryCheung on 4/10/16.
 */
public class TestDelFile {
    private final static String bucketname = "zhangcf695887-b90f-4e2a-906b-09416d59f413";
    private final static String objectname = "1.jpg";

    public static void main(String[] args) {
        AmazonS3Client s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        s3Client.deleteObject(bucketname, objectname);
    }
}
