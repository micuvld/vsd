package units;

import external.models.Entity;
import external.models.Loader;
import external.models.RawModel;
import external.render.OBJLoader;
import external.render.Renderer;
import external.shaders.StaticShader;
import external.textures.ModelTexture;
import external.textures.TexturedModel;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector3f;
import utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SingleThreadedDisplayManager implements Runnable{
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS_CAP = 120;

    private Loader loader;
    private Renderer renderer;
    private StaticShader staticShader;

    public static void main(String[] argv) {
        SingleThreadedDisplayManager singleThreadedDisplayManager = new SingleThreadedDisplayManager();
        new Thread(singleThreadedDisplayManager).start();
    }

    @Override
    public void run() {
        createDisplay();
        prepareToRender();

        List<Entity> entities = new ArrayList<>();

        for (int i = 0; i < 50; ++i) {
            byte[] text = null;
            try {
                text = Files.readAllBytes(Paths.get("C:\\MyStuff\\Facultate\\VSD\\Other assts\\forS3\\obj\\ship" + i + ".obj"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            RawModel model = null;
            try {
                model = OBJLoader.loadObjModel(Utils.decompress(text), loader);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ModelTexture modelTexture = new ModelTexture(loader.loadTexture("C:\\MyStuff\\Facultate\\VSD\\Other assts\\forS3\\textures\\ship" + i + "_texture.png"));
            TexturedModel textureModel = new TexturedModel(model, modelTexture);
            entities.add(Entity.builder()
                    .model(textureModel)
                    .position(new Vector3f(i, i/5, -20))
                    .rotX(0)
                    .rotY(0)
                    .rotZ(0)
                    .scale(1)
                    .build());
        }

        while(!Display.isCloseRequested()) {
            //game logic
            //render
            entities.forEach(entity -> entity.increaseRotation(0, 0.1f, 0));
            renderer.prepare();
            staticShader.start();
            entities.forEach(entity -> renderer.render(entity, staticShader));
            staticShader.stop();
            updateDisplay();
        }

        cleanUp();
        closeDisplay();
    }

    public void prepareToRender() {
        loader = new Loader();
        staticShader = new StaticShader();
        renderer = new Renderer(staticShader);

    }

    private void createDisplay() {
        ContextAttribs attribs = new ContextAttribs(3,2);
        attribs.withForwardCompatible(true);
        attribs.withProfileCore(true);

        try {
            Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
            Display.create(new PixelFormat(), attribs);
            Display.setTitle("VSD-Project");
        } catch (LWJGLException e) {
            log.error("Error at creating the external.display!");
            e.printStackTrace();
        }

        GL11.glViewport(0, 0, WIDTH, HEIGHT);
    }

    private void updateDisplay() {
        Display.sync(FPS_CAP);
        Display.update();
    }

    private void closeDisplay() {
        cleanUp();
        Display.destroy();
    }

    private void cleanUp() {
        loader.cleanUp();
        staticShader.cleanUp();
    }
}