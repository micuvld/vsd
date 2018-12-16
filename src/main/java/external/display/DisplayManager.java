package external.display;

import external.models.Entity;
import external.render.Renderer;
import external.shaders.StaticShader;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import processing.FrameObject;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.opengl.GL11.*;

@Slf4j
public class DisplayManager extends AbstractDisplayManager{
    private Renderer renderer;
    private StaticShader staticShader;
    private ConcurrentLinkedQueue<Entity> entities;
    private byte[] depthArray = new byte[WIDTH * HEIGHT * 4];
    private byte[] pixelArray = new byte[WIDTH * HEIGHT * 3];

    public DisplayManager(String title, ConcurrentLinkedQueue<Entity> concurrentLinkedQueue) {
        super(title);
        createDisplay();
        entities = concurrentLinkedQueue;
    }

    public FrameObject getFrame() {
        prepareToRender();

        if (!Display.isCloseRequested()) {
            renderer.prepare();
            staticShader.start();
            entities.forEach(entity -> renderer.render(entity, staticShader));
            FrameObject frameToReturn = renderFrame();
            staticShader.stop();
            updateDisplay();
            return frameToReturn;
        } else {
            cleanUp();
            closeDisplay();

            return null;
        }
    }

    private void prepareToRender() {
        staticShader = new StaticShader();
        renderer = new Renderer(staticShader);

    }

    private FrameObject renderFrame() {
        ByteBuffer depthBuffer = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 3);

        GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL_DEPTH_COMPONENT,
                GL_FLOAT, depthBuffer);
        GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL_RGB,
                GL_UNSIGNED_BYTE, pixelBuffer);


        depthBuffer.get(depthArray);
        pixelBuffer.get(pixelArray);

        return FrameObject.builder()
                .depthBuffer(depthArray)
                .pixelBuffer(pixelArray)
                .build();
    }

    private void updateDisplay() {
        Display.sync(FPS_CAP);
        Display.update();
    }

    protected void cleanUp() {
        staticShader.cleanUp();
    }
}