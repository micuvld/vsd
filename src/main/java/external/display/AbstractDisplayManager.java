package external.display;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

@Slf4j
public abstract class AbstractDisplayManager{
    protected static final int WIDTH = 1280;
    protected static final int HEIGHT = 720;
    protected static final int FPS_CAP = 120;
    String title;

    public AbstractDisplayManager(String title) {
        this.title = title;
    }

    protected void createDisplay() {
        try {
            Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
            Display.create(new PixelFormat());
            Display.setTitle(title);
        } catch (LWJGLException e) {
            log.error("Error at creating the external.display!");
            e.printStackTrace();
        }

        GL11.glViewport(0, 0, WIDTH, HEIGHT);
    }

    protected abstract void cleanUp();

    protected void closeDisplay() {
        cleanUp();
        Display.destroy();
    }

}
