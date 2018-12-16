package utils;

import aws.SqsWrapper;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import units.SlaveMetadata;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConnectionListener implements Runnable {
    private static final int LISTEN_DELAY = 0;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, SlaveMetadata> slaves;
    private SqsWrapper sqsWrapper;
    private int numSlaves;

    public ConnectionListener(Map<String, SlaveMetadata> slaves, String queuePrefix, int numSlaves) {
        this.slaves = slaves;
        this.sqsWrapper = new SqsWrapper(queuePrefix);
        this.numSlaves = numSlaves;
    }

    @Override
    public void run() {
        log.info("Connection listener started listening for clients.");
        while (slaves.size() < numSlaves) {
            List<Message> sqsMessages = sqsWrapper.getMessages(numSlaves, 1);
            processMessage(sqsMessages);

            try {
                Thread.sleep(LISTEN_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("All clients connected. Ready for processing phase");
    }

    private void processMessage(List<Message> sqsMessages) {
        sqsMessages.forEach(message -> {
            String messageType = message.getMessageAttributes().get(MessageAttribute.TYPE.name()).getStringValue();

            if (messageType == null) {
                log.warn("Received message with no message type! Message body: {}", message.getBody());
            } else {
                if (messageType.equals(MessageType.HANDSHAKE_SLAVE.name())) {
                    try {
                        SlaveMetadata slave = objectMapper.readValue(message.getBody(), SlaveMetadata.class);
                        slave.setSqsWrapper(new SqsWrapper(slave.getQueue()));
                        log.info("New client found: {}", slave.getUuid());
                        slaves.putIfAbsent(slave.getUuid(), slave);
                        log.info("Client registered. Number of clients: {}", slaves.size());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            sqsWrapper.deleteMessage(message);
        });
    }
}
