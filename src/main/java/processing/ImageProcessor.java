//package processing;
//
//import org.lwjgl.LWJGLException;
//import org.lwjgl.opengl.Display;
//import org.lwjgl.opengl.DisplayMode;
//import utils.AppProperty;
//import utils.Config;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.nio.ByteBuffer;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.lwjgl.opengl.GL11.*;
//import static org.lwjgl.opengl.GL11.GL_LEQUAL;
//
//public class ImageProcessor {
//    private static final OBJLoader OBJ_LOADER = new OBJLoader();
//    private Map<String, Obj> objModels = new HashMap<>();
//    private static final int IMAGE_WIDTH = Config.getInt(AppProperty.DISPLAY_WIDTH);
//    private static final int IMAGE_HEIGHT = Config.getInt(AppProperty.DISPLAY_HEIGHT);
//    private static final DisplayMode DISPLAY_MODE = new DisplayMode(IMAGE_WIDTH, IMAGE_HEIGHT);
//
//    public void init(List<String> imageLocations) throws FileNotFoundException {
//        initGL();
//
//        imageLocations.forEach(location -> {
//            File imageFile = new File(location);
//            try {
//                objModels.put(location, OBJ_LOADER.loadModel(imageFile));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        });
//    }
//
//    private void initGL() {
//        try {
//            Display.setDisplayMode(DISPLAY_MODE);
//            Display.setTitle("Window");
//            Display.create();
//        } catch (LWJGLException e) {
//            e.printStackTrace();
//        }
//
//        glClearDepth(1.0f); // clear depth buffer
//        glEnable(GL_DEPTH_TEST); // Enables depth testing
//        glDepthFunc(GL_LEQUAL); // sets the type of test to use for depth
//    }
//
//    public void createFrameBuffersForObjects() {
//        objModels.entrySet().forEach(entry -> {
//            prepareFrame();
//            BuffersUtils.createFrameBuffer();
//            BuffersUtils.createDepthBufferAttachement(IMAGE_WIDTH, IMAGE_HEIGHT);
//            OBJ_LOADER.render(entry.getValue());
//
//            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(IMAGE_WIDTH * IMAGE_HEIGHT * 3);
//            glReadPixels(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, GL_RGB, GL_UNSIGNED_BYTE, byteBuffer);
//        });
//    }
//
//    private void prepareFrame() {
//        glClearDepth(1.0f); // clear depth buffer
//        glEnable(GL_DEPTH_TEST); // Enables depth testing
//        glDepthFunc(GL_LEQUAL); // sets the type of test to use for depth
//        glMatrixMode(GL_PROJECTION); // sets the matrix mode to project
//        glLoadIdentity();
//        glOrtho(-IMAGE_WIDTH/2, IMAGE_WIDTH/2 , IMAGE_HEIGHT/2, -IMAGE_HEIGHT/2, -1, 100);
//        glMatrixMode(GL_MODELVIEW);
//        glViewport(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
//    }
//}
