package external.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RawModel {
    private int vaoID;
    private int vertexCount;
}
