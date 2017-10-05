package com.ibs.lambda.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.iot.client.AWSIotMqttClient;

import java.io.File;
import java.net.URL;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.ibs.lambda.demo.NonBlockingPublishListener;
import com.ibs.lambda.demo.TestTopicListener;

import com.ibs.services.iot.client.sample.sampleUtil.CommandArguments;
import com.ibs.services.iot.client.sample.sampleUtil.SampleUtil;
import com.ibs.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;


public class LambdaFunctionHandler implements RequestHandler<String, String> {
	
	 private static final String TestTopic = "helloWorldTopic";
	    private static final AWSIotQos TestTopicQos = AWSIotQos.QOS0;

	    private static AWSIotMqttClient awsIotClient;
	   

	    public static void setClient(AWSIotMqttClient client) {
	        awsIotClient = client;
	    }

	    //Blocking Publisher
	    public static class BlockingPublisher implements Runnable {
	        private final AWSIotMqttClient awsIotClient;
	        private static String payload = null;
	        
	        public BlockingPublisher(AWSIotMqttClient awsIotClient,String payload) {
	            this.awsIotClient = awsIotClient;
	            this.payload = payload;
	        }

	        @Override
	        public void run() {
	            //long counter = 1;
	                //while (counter == 2) {
	                //String payload = "<----------------GOOD DAY from LAMBDA JAVA BlockingPublisher PUBLISHER-----------------> ";
	                try {
	                    awsIotClient.publish(TestTopic, payload);
	                } catch (AWSIotException e) {
	                    System.out.println(System.currentTimeMillis() + ": publish failed for " + payload);
	                }
	                System.out.println(System.currentTimeMillis() + ": >>> " + payload);

	                /*try {
	                    Thread.sleep(1000);
	                } catch (InterruptedException e) {
	                    System.out.println(System.currentTimeMillis() + ": BlockingPublisher was interrupted");
	                    return;
	                }
	            }*/
	        }
	    }
       //Non Blocking
	    public static class NonBlockingPublisher implements Runnable {
	        private final AWSIotMqttClient awsIotClient;
	        private static String payload = null;
	        public NonBlockingPublisher(AWSIotMqttClient awsIotClient, String payload) {
	            this.awsIotClient = awsIotClient;
	            this.payload = payload;
	        }

	        @Override
	        public void run() {
	            long counter = 1;

	                // while (counter ==2) {
	                //String payload = "------GOOD DAY from LAMBDA JAVA NON BlockingPublisher PUBLISHER ------- " ;
	                AWSIotMessage message = new NonBlockingPublishListener(TestTopic, TestTopicQos, payload);
	                try {
	                    awsIotClient.publish(message);
	                } catch (AWSIotException e) {
	                    System.out.println(System.currentTimeMillis() + ": publish failed for " + payload);
	                }

	               /* try {
	                    Thread.sleep(1000);
	                } catch (InterruptedException e) {
	                    System.out.println(System.currentTimeMillis() + ": NonBlockingPublisher was interrupted");
	                    return;
	                }
	            }*/
	        }
	    }
	    
	    
    @Override
    public String handleRequest(String payload, Context context) { 
    	String  response_payload = "RESPONSE FROM AWS LAMBDA: " + payload ;
    	String[] args = {};
    	  CommandArguments arguments = CommandArguments.parse(args);
          initClient(arguments);
    	try {
			awsIotClient.connect();
		} catch (AWSIotException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//AWSIotTopic topic = new TestTopicListener(TestTopic, TestTopicQos);
        // awsIotClient.subscribe(topic, true);

         Thread blockingPublishThread = new Thread(new BlockingPublisher(awsIotClient, payload));
         Thread nonBlockingPublishThread = new Thread(new NonBlockingPublisher(awsIotClient, payload));

         blockingPublishThread.start();
         nonBlockingPublishThread.start();

         try {
			blockingPublishThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         try {
			nonBlockingPublishThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
        
        
        return response_payload;
    }
    
    private static void initClient(CommandArguments arguments) {
    	String clientEndpoint = arguments.getNotNull("clientEndpoint", SampleUtil.getConfig("clientEndpoint"));
        String clientId = arguments.getNotNull("clientId", SampleUtil.getConfig("clientId"));
        String certificateFile = arguments.get("certificateFile", SampleUtil.getConfig("certificateFile"));
        String privateKeyFile = arguments.get("privateKeyFile", SampleUtil.getConfig("privateKeyFile"));            
               
         if (awsIotClient == null && certificateFile != null && privateKeyFile != null) {        	
         /*String algorithm = arguments.get("keyAlgorithm", SampleUtil.getConfig("keyAlgorithm"));
            KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, algorithm);            
            Sring algorithm = arguments.get("keyAlgorithm", SampleUtil.getConfig("keyAlgorithm"));*/
            KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
            awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
        }

        if (awsIotClient == null) {        	
            String awsAccessKeyId = arguments.get("awsAccessKeyId", SampleUtil.getConfig("awsAccessKeyId"));
            String awsSecretAccessKey = arguments.get("awsSecretAccessKey", SampleUtil.getConfig("awsSecretAccessKey"));
            String sessionToken = arguments.get("sessionToken", SampleUtil.getConfig("sessionToken"));

            if (awsAccessKeyId != null && awsSecretAccessKey != null) {
            	System.out.println("if (awsAccessKeyId != null && awsSecretAccessKey != null)");
                awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey,
                        sessionToken);
            }
        }

        if (awsIotClient == null) {
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }
    }
    /*private static void initClient() {
        String clientEndpoint = SampleUtil.getConfig("clientEndpoint");
        String clientId = SampleUtil.getConfig("clientId");

        String certificateFile =  SampleUtil.getConfig("certificateFile");
        String privateKeyFile =  SampleUtil.getConfig("privateKeyFile");
        if (awsIotClient == null && certificateFile != null && privateKeyFile != null) {
        	System.out.println("====>if (awsIotClient == null && certificateFile != null && privateKeyFile != null)");
            String algorithm = SampleUtil.getConfig("keyAlgorithm");

            KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, algorithm);

            awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
        }

        if (awsIotClient == null) {
        	System.out.println("=======>f (awsIotClient == null)");
            String awsAccessKeyId = SampleUtil.getConfig("awsAccessKeyId");
            String awsSecretAccessKey =  SampleUtil.getConfig("awsSecretAccessKey");
            String sessionToken = SampleUtil.getConfig("sessionToken");

            if (awsAccessKeyId != null && awsSecretAccessKey != null) {
            	System.out.println("==========>if (awsAccessKeyId != null && awsSecretAccessKey != null)");
                awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey,
                        sessionToken);
            }
        }

        if (awsIotClient == null) {
        	
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }
    }*/
    
    

}
