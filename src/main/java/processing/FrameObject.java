package processing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FrameObject {
    private byte[] pixelBuffer;
    private byte[] depthBuffer;
}
