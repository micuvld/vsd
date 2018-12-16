package processing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectProperties {
    private InitialObjectParameters parameters;
    private String s3ObjKey;
    private String s3TextureKey;
}
