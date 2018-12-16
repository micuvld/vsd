package units;

import aws.S3Wrapper;
import aws.SqsWrapper;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import external.display.DisplayManager;
import external.models.Entity;
import external.models.Loader;
import external.render.OBJLoader;
import external.textures.ModelTexture;
import external.textures.TexturedModel;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.util.vector.Vector3f;
import processing.FrameObject;
import processing.InitialObjectParameters;
import processing.ObjectProperties;
import utils.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Slf4j
public class Slave implements ProcessingUnit {
    private static final String QUEUE_PREFIX = Config.get(AppProperty.QUEUE_PREFIX);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String INPUT_BUCKET = Config.get(AppProperty.INPUT_OBJECT_BUCKET);
    private static final String PROCESSED_BUCKET = Config.get(AppProperty.PROCESSED_OBJECT_BUCKET);
    private static final String TEXTURES_BUCKET = Config.get(AppProperty.TEXTURES_BUCKET);
    private String slaveId;
    private SlaveMetadata slaveMetadata;
    private SqsWrapper handshakeSqsWrapper;
    private SqsWrapper slaveSqsWrapper;
    private List<ObjectProperties> assignedObjects = null;
    private S3Wrapper s3Wrapper;
    private Loader loader = new Loader();
    private ConcurrentLinkedQueue<Entity> entities;
    private DisplayManager displayManager;

    public Slave() {
        slaveId = generateSlaveId();
        this.slaveMetadata = SlaveMetadata.builder()
                .uuid(slaveId)
                .queue(generateSlaveQueue(slaveId))
                .build();
        this.handshakeSqsWrapper = new SqsWrapper(QUEUE_PREFIX);
        this.slaveSqsWrapper = new SqsWrapper(slaveMetadata.getQueue());
        this.s3Wrapper = new S3Wrapper();
        this.entities = new ConcurrentLinkedQueue<>();
        this.displayManager = new DisplayManager("Slave#" + slaveId, entities);
    }

    public static void main(String[] args) {
        Slave slave = new Slave();
        slave.run();
    }

    @Override
    public void run() {
        log.info("Slave has started");
        log.info("Creating own queue: " + slaveMetadata.getQueue());
        //create own queue
        slaveSqsWrapper.createQueue();
        sendHandshake();
        waitForObjectAssignement();
        readAssignedObjects();

        FrameObject currentFrame;
        currentFrame = displayManager.getFrame();
        sendFrameToMaster(currentFrame);

        while (true) {
            List<Command> commands = waitForCommands();

            for (Command command : commands) {
                processCommand(command);
            }

            currentFrame = displayManager.getFrame();
            sendFrameToMaster(currentFrame);
        }
    }

    private void processCommand(Command command) {
        log.info("Proccessing command {}", command.name());
        switch (command) {
            case MOVE_UP:
                entities.forEach(entity -> entity.increasePosition(0, 0.5f, 0));
                break;
            case MOVE_DOWN:
                entities.forEach(entity -> entity.increasePosition(0, -0.5f, 0));
                break;
            case MOVE_LEFT:
                entities.forEach(entity -> entity.increasePosition(-0.5f, 0, 0));
                break;
            case MOVE_RIGHT:
                entities.forEach(entity -> entity.increasePosition(0.5f, 0, 0));
                break;
            case ROTATE_LEFT:
                entities.forEach(entity -> entity.increaseRotation(0, 0, 10));
                break;
            case ROTATE_RIGHT:
                entities.forEach(entity -> entity.increaseRotation(0, 0, -10));
                break;
        }
    }

    private List<Command> waitForCommands() {
        List<Command> commands = new ArrayList<>();

        while (commands.isEmpty()) {
            List<Message> messages = slaveSqsWrapper.getMessages(1, 1);
            messages.forEach(message -> {
                if (MessageType.COMMAND_REQUEST.name()
                        .equals(message.getMessageAttributes().get(MessageAttribute.TYPE.name()).getStringValue())) {
                    commands.add(Command.valueOf(message.getBody()));
                }

                slaveSqsWrapper.deleteMessage(message);
            });
        }

        return commands;
    }

    private void sendFrameToMaster(FrameObject frameObject) {
        try {
            String frameObjectName = "frame#" + slaveId;
            log.info("Sending frame {} to master.", frameObjectName);
            byte[] bytes = Utils.compress(objectMapper.writeValueAsString(frameObject));
            s3Wrapper.uploadBytes(PROCESSED_BUCKET, frameObjectName, bytes);
            slaveSqsWrapper.sendMessage(MessageType.NOTIFY_UPLOAD, frameObjectName);
        } catch (IOException e) {
            log.error("Failed to convert frame to json!");
            e.printStackTrace();
        }
    }

    private byte[] readAssignedObjects() {
        ConcurrentLinkedQueue<ObjAndTexture> objAndTextures = new ConcurrentLinkedQueue<>();
        assignedObjects.parallelStream().forEach(object -> {
            try {
                log.info("Reading the assigned object bytes for {}", object.getS3ObjKey());
                String objFile = Utils.decompress(s3Wrapper.getObjectBytes(INPUT_BUCKET, object.getS3ObjKey()));
                byte[] textureFile = s3Wrapper.getObjectBytes(TEXTURES_BUCKET, object.getS3TextureKey());
                objAndTextures.add(new ObjAndTexture(objFile, textureFile, object.getParameters()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        objAndTextures.forEach(element -> {
            entities.add(generateEntity(element.getParameters(), element.getObjContent(), element.getTexture()));
        });
        return null;
    }

    private void waitForObjectAssignement() {
        while (assignedObjects == null) {
            List<Message> objectMessages = slaveSqsWrapper.getMessages(1, 1);
            objectMessages.forEach(message -> {
                if (message.getMessageAttributes().get(MessageAttribute.TYPE.name()).getStringValue()
                        .equals(MessageType.OBJECT_SETUP.name())) {
                    try {
                        assignedObjects = objectMapper.readValue(
                                message.getBody(), new TypeReference<List<ObjectProperties>>() {});
                        log.info("I have been assigned the following objects: {}", assignedObjects.stream()
                                .map(ObjectProperties::getS3ObjKey)
                                .collect(Collectors.joining(" ")));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    slaveSqsWrapper.deleteMessage(message);
                }
            });
        }
    }

    private void sendHandshake() {
        log.info("Sending handshake message");
        try {
            handshakeSqsWrapper.sendMessage(MessageType.HANDSHAKE_SLAVE, objectMapper.writeValueAsString(slaveMetadata));
            log.info("Successuflly Sent handshake message");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private Entity generateEntity(InitialObjectParameters parameters, String objFile, byte[] textureFile) {
        return Entity.builder()
                .model(TexturedModel.builder()
                        .rawModel(OBJLoader.loadObjModel(objFile, loader))
                        .modelTexture(new ModelTexture(loader.loadTexture(textureFile)))
                        .build())
                .position(new Vector3f(parameters.getTranslateX(), parameters.getTranslateY(), parameters.getTranslateZ()))
                .rotX(parameters.getRotateX())
                .rotY(parameters.getRotateY())
                .rotZ(parameters.getRotateZ())
                .scale(parameters.getScale())
                .build();
    }

    private static String generateSlaveId() {
        return UUID.randomUUID().toString().replace("-", "_");
    }

    private String generateSlaveQueue(String slaveId) {
        return "slave_" + slaveId;
    }
}
