package utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import processing.InitialObjectParameters;

@Data
@Builder
@AllArgsConstructor
public class ObjAndTexture {
    private String objContent;
    private byte[] texture;
    private InitialObjectParameters parameters;
}
