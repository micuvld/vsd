package units;

import aws.SqsWrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SlaveMetadata {
    private String uuid;
    private String queue;
    private SqsWrapper sqsWrapper;
}
