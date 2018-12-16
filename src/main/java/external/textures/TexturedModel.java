package external.textures;

import external.models.RawModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TexturedModel {
    private final RawModel rawModel;
    private final ModelTexture modelTexture;
}
