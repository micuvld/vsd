package external.display;

import com.jogamp.opengl.GL;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class SimpleDisplayManager extends AbstractDisplayManager implements Runnable{

    private LinkedBlockingQueue<ByteBuffer> frames;

    public SimpleDisplayManager(String title, LinkedBlockingQueue<ByteBuffer> frames) {
        super(title);
        this.frames = frames;
    }

    @Override
    public void run() {
        createDisplay();
        prepareDisplay();

        ByteBuffer newFrame = null;
        ByteBuffer currentFrame = null;
        try {
            currentFrame = frames.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while(!Display.isCloseRequested()) {
            newFrame = frames.poll();
            if (newFrame != null) {
                currentFrame = newFrame;
            }

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDrawPixels(WIDTH, HEIGHT, GL.GL_RGB, GL11.GL_UNSIGNED_BYTE,
                    currentFrame);
            Display.update();
        }

        closeDisplay();
    }

    private void prepareDisplay() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    protected void cleanUp() {
        //do nothing
    }
}
