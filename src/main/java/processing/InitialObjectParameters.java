package processing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialObjectParameters {
    private float translateX;
    private float translateY;
    private float translateZ;
    private int rotateX;
    private int rotateY;
    private int rotateZ;
    private int scale;
}
