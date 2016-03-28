/**
 * Created by JerryCheung on 3/27/16.
 */
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class TestMongoDB{
    public static void main( String args[] ){
        try{
            // 连接到 mongodb 服务
            MongoClient mongoClient = new MongoClient( "localhost" , 27017 );

            // 连接到数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase("mycol");
            System.out.println("Connect to database successfully");
            mongoClient.close();

        }catch(Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }
}
