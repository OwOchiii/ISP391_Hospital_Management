package orochi.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ReportResults")
@Data
@IdClass(ReportResultId.class)
public class ReportResult {

    @Id
    @Column(name = "ReportID")
    private Integer reportId;

    @Id
    @Column(name = "ResultID")
    private Integer resultId;

    @ManyToOne
    @JoinColumn(name = "ReportID", insertable = false, updatable = false)
    private MedicalReport report;

    @ManyToOne
    @JoinColumn(name = "ResultID", insertable = false, updatable = false)
    private MedicalResult result;
}
