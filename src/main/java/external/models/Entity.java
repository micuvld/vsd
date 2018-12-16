package external.models;

import external.textures.TexturedModel;
import lombok.Builder;
import lombok.Data;
import org.lwjgl.util.vector.Vector3f;

@Data
@Builder
public class Entity {
    private TexturedModel model;
    private Vector3f position;
    private float rotX, rotY, rotZ;
    private float scale;

    public void increasePosition(float dx, float dy, float dz) {
        this.position.x+=dx;
        this.position.y+=dy;
        this.position.z+=dz;
    }

    public void increaseRotation(float dx, float dy, float dz) {
        this.rotX+=dx;
        this.rotY+=dy;
        this.rotZ+=dz;
    }
}
