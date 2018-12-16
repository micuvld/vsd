package external.shaders;

import org.lwjgl.util.vector.Matrix4f;

public class StaticShader extends ShaderProgram {
    private static final String VERTEX_FILE = "C:\\MyStuff\\Facultate\\VSD\\Proiect\\src\\main\\java\\external\\shaders\\vertexShader.txt";
    private static final String FRAGMENT_FILE = "C:\\MyStuff\\Facultate\\VSD\\Proiect\\src\\main\\java\\external\\shaders\\fragmentShader.txt";

    private int locationTransformationMatrix;
    private int locationProjectionMatrix;

    public StaticShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    @Override
    protected void getAllUniformLocations() {
        locationTransformationMatrix = super.getUniformLocation("transformationMatrix");
        locationProjectionMatrix = super.getUniformLocation("projectionMatrix");
    }

    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "textureCoords");
    }

    public void loadTransformationMatrix(Matrix4f matrix) {
        super.loadMatrix(locationTransformationMatrix, matrix);
    }

    public void loadProjectionMatrix(Matrix4f projection) {
        super.loadMatrix(locationProjectionMatrix, projection);
    }
}
