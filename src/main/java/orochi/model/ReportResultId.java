package orochi.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class ReportResultId implements Serializable {
    private Integer reportId;
    private Integer resultId;

    public ReportResultId(Integer reportId, Integer resultId) {
        this.reportId = reportId;
        this.resultId = resultId;
    }
}
