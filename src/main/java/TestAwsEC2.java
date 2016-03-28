/**
 * Created by JerryCheung on 3/27/16.
 */
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TestAwsEC2 {
    public static void main(String[] args) throws Exception {
        //Load the Properties File with AWS Credentials
        //Properties properties = new Properties();
        //properties.load(RunEC2test.class.getResourceAsStream("/AwsCredentials.properties"));
        //BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
        //Create an Amazon EC2 Client
        AmazonEC2Client ec2 = new AmazonEC2Client(new ProfileCredentialsProvider());
        //Create Instance Request
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
        //Configure Instance Request
        runInstancesRequest.withImageId("ami-fce3c696")
                .withInstanceType("m3.medium")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("15319demo")             //make sure the Key name and the security group have already existed in the account
                .withSecurityGroups("default");

        //Launch Instance
        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
        //Return the Object Reference of the Instance just Launched
        Instance instance=runInstancesResult.getReservation().getInstances().get(0);

        //Add a tag to the Instance
        CreateTagsRequest createTagsRequest = new CreateTagsRequest();
        createTagsRequest.withResources(instance.getInstanceId())
                .withTags(new Tag("Name", "ClassDemo"));

        //Print InstanceID
        System.out.println("Just launched an Instance with ID: " + instance.getInstanceId());

        //Prompt User
        System.out.printf("\nTerminate Instance? (y/n): ");

        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        String userResponse = bufferRead.readLine();

        if (userResponse.toLowerCase().equals("y")) {
            //Terminate Instance
            TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
            List<String> instances = new ArrayList<String>();
            instances.add(instance.getInstanceId());
            terminateInstancesRequest.setInstanceIds(instances);
            ec2.terminateInstances(terminateInstancesRequest);
        }
    }
}