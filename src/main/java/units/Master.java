package units;

import aws.S3Wrapper;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import external.display.SimpleDisplayManager;
import lombok.extern.slf4j.Slf4j;
import processing.FrameObject;
import processing.InitialObjectParameters;
import processing.ObjectProperties;
import utils.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Slf4j
public class Master implements ProcessingUnit {
    private static final String TEXTURE_PREFIX = "_texture.png";
    private static final String QUEUE_PREFIX = Config.get(AppProperty.QUEUE_PREFIX);
    private static final String PROCESSED_BUCKET = Config.get(AppProperty.PROCESSED_OBJECT_BUCKET);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, SlaveMetadata> slaves;
    private S3Wrapper s3Wrapper;
    private SimpleDisplayManager simpleDisplayManager;
    private int numSlaves = 2;
    private LinkedBlockingQueue<ByteBuffer> pixelFrames;

    public Master() {
        s3Wrapper = new S3Wrapper();
        slaves = new ConcurrentHashMap<>();
        pixelFrames = new LinkedBlockingQueue<>();
        simpleDisplayManager = new SimpleDisplayManager("Master", pixelFrames);
    }

    public void run() {
        inputNumSlaves();
        cleanup();
        List<ObjectProperties> objectsProperties = readObjectsLocations();
        waitForSlavesToJoin(numSlaves);
        assignObjectsToSlaves(objectsProperties);

        new Thread(simpleDisplayManager).start();
        while (true) {
            Map<String, String> slaveReplies = waitForSlavesReplies();
            ConcurrentLinkedQueue<FrameObject> frames = fetchFrames(slaveReplies);
            ByteBuffer pixelsToBeDisplayed = combineFrames(frames);
            pixelFrames.offer(pixelsToBeDisplayed);
            Command command = CommandListener.getCommand();
            sendCommandToSlaves(command);
        }
    }

    private void inputNumSlaves() {
        System.out.println("Input number of slaves to wait for:");
        Scanner scanner = new Scanner(System.in);
        numSlaves = scanner.nextInt();
    }

    private ByteBuffer combineFrames(ConcurrentLinkedQueue<FrameObject> frames) {
        log.info("Combining the frames");
        if (frames.size() == 0) {
            return null;
        }

        if (frames.size() == 1) {
            return Utils.byteArrayToByteBuffer(frames.poll().getPixelBuffer());
        }

        FrameObject firstFrame = frames.poll();
        FrameObject secondFrame = frames.poll();
        FrameObject resultFrame = mergeTwoFrames(firstFrame, secondFrame);

        FrameObject nextFrame;
        while ((nextFrame = frames.poll()) != null) {
            resultFrame = mergeTwoFrames(resultFrame, nextFrame);
        }

        return Utils.byteArrayToByteBuffer(resultFrame.getPixelBuffer());
    }

    private FrameObject mergeTwoFrames(FrameObject frame1, FrameObject frame2) {
        byte[] depthBuffer1 = frame1.getDepthBuffer();
        byte[] pixelsBuffer1 = frame1. getPixelBuffer();
        byte[] depthBuffer2 = frame2.getDepthBuffer();
        byte[] pixelBuffer2 = frame2.getPixelBuffer();
        byte[] resultDepthBuffer = new byte[frame2.getDepthBuffer().length];
        byte[] resultPixelBuffer = new byte[frame1.getPixelBuffer().length];

        int pixelIndex = 0;
        for (int i = 0; i < frame1.getDepthBuffer().length; i += 4) {
            if (depthBuffer1[i + 2] > depthBuffer2[i + 2]) {
                resultPixelBuffer[pixelIndex] = pixelsBuffer1[pixelIndex];
                resultPixelBuffer[pixelIndex + 1] = pixelsBuffer1[pixelIndex + 1];
                resultPixelBuffer[pixelIndex + 2] = pixelsBuffer1[pixelIndex + 2];
                resultDepthBuffer[i] = depthBuffer1[i + 2];
            } else {
                resultPixelBuffer[pixelIndex] = pixelBuffer2[pixelIndex];
                resultPixelBuffer[pixelIndex + 1] = pixelBuffer2[pixelIndex + 1];
                resultPixelBuffer[pixelIndex + 2] = pixelBuffer2[pixelIndex + 2];
                resultDepthBuffer[i] = depthBuffer2[i + 2];
            }
            pixelIndex += 3;
        }

        return FrameObject.builder()
                .pixelBuffer(resultPixelBuffer)
                .depthBuffer(resultDepthBuffer)
                .build();
    }

    private ConcurrentLinkedQueue<FrameObject> fetchFrames(Map<String, String> slaveReplies) {
        ConcurrentLinkedQueue<FrameObject> frameObjects = new ConcurrentLinkedQueue<>();
        log.info("Fetching the frames from S3");

        slaveReplies.values().parallelStream().forEach(objectS3Key -> {
            try {
                byte[] frameBytes = s3Wrapper.getObjectBytes(PROCESSED_BUCKET, objectS3Key);
                frameObjects.add(objectMapper.readValue(Utils.decompress(frameBytes), FrameObject.class));
            } catch (IOException e) {
                log.error("Failed to parse json received from slave, from location: {}", objectS3Key);
                e.printStackTrace();
            }
        });

        return frameObjects;
    }

    private void sendCommandToSlaves(Command command) {
        log.info("Sending command {} to slaves.", command.name());
        slaves.values().parallelStream().forEach(slave -> {
            slave.getSqsWrapper().sendMessage(MessageType.COMMAND_REQUEST, command.name());
        });

    }

    private List<ObjectProperties> readObjectsLocations() {
        List<String> objFilesLocations = s3Wrapper.getKeysOfBucket(Config.get(AppProperty.INPUT_OBJECT_BUCKET));
        log.info("Found {} objects in S3.", objFilesLocations.size());
        return objFilesLocations.stream()
                .map(objLocation -> ObjectProperties.builder()
                        .s3ObjKey(objLocation)
                        .s3TextureKey(objLocation.split("\\.")[0] + TEXTURE_PREFIX)
                        .build())
                .collect(Collectors.toList());
    }

    private void waitForSlavesToJoin(int numSlaves) {
        ConnectionListener connectionListener = new ConnectionListener(slaves, QUEUE_PREFIX, numSlaves);
        new Thread(connectionListener).start();

        while (slaves.size() < numSlaves) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void assignObjectsToSlaves(List<ObjectProperties> objectsProperties) {
        log.info("Assigning objects to slaves");
        int numImagesPerNode = objectsProperties.size() / slaves.size();
        int numLeftoverImages = objectsProperties.size() % slaves.size();

        Iterator<ObjectProperties> objectSetupIterator = objectsProperties.iterator();
        Iterator<SlaveMetadata> slavesIterator = slaves.values().iterator();
        int slaveIndex = 0;
        while (slavesIterator.hasNext()) {
            SlaveMetadata slave = slavesIterator.next();
            try {
                int numImagesToAssign = slavesIterator.hasNext() ? numImagesPerNode : numImagesPerNode + numLeftoverImages;
                List<ObjectProperties> objectsForSlave = generateListOfObjectsForSlave(objectSetupIterator, numImagesToAssign, slaveIndex++);
                slave.getSqsWrapper().sendMessage(
                        MessageType.OBJECT_SETUP, objectMapper.writeValueAsString(objectsForSlave));

                log.info("Assigning objects {} to slave {} ",
                        objectsForSlave.stream()
                                .map(ObjectProperties::getS3ObjKey)
                                .collect(Collectors.joining(" ")),
                        slave.getUuid());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    private List<ObjectProperties> generateListOfObjectsForSlave(
            Iterator<ObjectProperties> iterator, int numImages, int slaveIndex) {
        List<ObjectProperties> objectProperties = new ArrayList<>();

        for (int i = 0; i < numImages; ++i) {
            if (iterator.hasNext()) {
                ObjectProperties currentObjectProperties = iterator.next();
                int objectIndex = slaveIndex + i;
                currentObjectProperties.setParameters(InitialObjectParameters.builder()
                        .translateX(i * 0.5f)
                        .translateY(i * 0.5f)
                        .translateZ(-10 + (i + slaveIndex * 10) * 0.1f)
                        .rotateY(20 * objectIndex)
                        .scale(1)
                        .build());
                objectProperties.add(currentObjectProperties);
            } else {
                log.warn("Attempted to log more images than there were left.");
            }
        }

        return objectProperties;
    }

    private Map<String, String> waitForSlavesReplies() {
        ConcurrentHashMap<String, String> slaveReplies = new ConcurrentHashMap<>();
        while (slaveReplies.size() < slaves.size()) {
            slaves.values().parallelStream().forEach(slave -> {
                List<Message> messageList = slave.getSqsWrapper().getMessages(1,1);
                if (!messageList.isEmpty()) {
                    log.info("Received reply from {}", slave.getUuid());
                    Message message = messageList.get(0);
                    String messageType = message.getMessageAttributes().get(MessageAttribute.TYPE.name()).getStringValue();
                    if (messageType.equals(MessageType.NOTIFY_UPLOAD.name())) {
                        slaveReplies.putIfAbsent(slave.getUuid(), message.getBody());
                    }

                    slave.getSqsWrapper().deleteMessage(message);
                }
            });
        }

        log.info("Received all the replies from slaves.");
        return slaveReplies;
    }

    private void cleanup() {
        s3Wrapper.clearBucket(Config.get(AppProperty.PROCESSED_OBJECT_BUCKET));
//        sqsWrapper.deleteOtherQueues();
    }

    public static void main(String[] args) {
        Master master = new Master();
        master.run();
    }
}
