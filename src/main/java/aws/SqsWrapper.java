package aws;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import utils.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqsWrapper {
    private static final int WAIT_TIME = 0;
    private String queuePrefix;
    private AmazonSQS sqsClient;

    public SqsWrapper(String queuePrefix) {
        this.queuePrefix = queuePrefix;
        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(CredentialsProvider.getProvider())
                .withRegion(Config.get(AppProperty.AWS_REGION))
                .build();
    }

    public List<Message> getMessages(int numMessages) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queuePrefix)
                .withWaitTimeSeconds(WAIT_TIME)
                .withVisibilityTimeout(5)
                .withMaxNumberOfMessages(numMessages)
                .withMessageAttributeNames(MessageAttribute.TYPE.name());

        return sqsClient.receiveMessage(receiveMessageRequest).getMessages();
    }

    public List<Message> getMessages(int numMessages, int waitTime) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queuePrefix)
                .withWaitTimeSeconds(waitTime)
                .withVisibilityTimeout(5)
                .withMaxNumberOfMessages(numMessages)
                .withMessageAttributeNames(MessageAttribute.TYPE.name());

        return sqsClient.receiveMessage(receiveMessageRequest).getMessages();
    }

    public void deleteMessage(Message message) {
        sqsClient.deleteMessage(new DeleteMessageRequest()
                .withQueueUrl(queuePrefix)
                .withReceiptHandle(message.getReceiptHandle()));
    }

    public void createQueue() {
        sqsClient.createQueue(queuePrefix);
    }

    public void sendMessage(MessageType type, String body) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put(MessageAttribute.TYPE.name(), new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(type.name()));

        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queuePrefix)
                .withMessageAttributes(attributes)
                .withMessageBody(body);
        sqsClient.sendMessage(sendMessageRequest);
    }

    public void deleteOtherQueues() {
        sqsClient.listQueues().getQueueUrls().stream()
                .filter(queue -> !queuePrefix.equals(queue))
                .forEach(sqsClient::deleteQueue);
    }
}
