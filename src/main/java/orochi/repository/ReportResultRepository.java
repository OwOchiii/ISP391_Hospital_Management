package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.ReportResult;
import orochi.model.ReportResultId;

import java.util.List;

@Repository
public interface ReportResultRepository extends JpaRepository<ReportResult, ReportResultId> {

    /**
     * Find all result associations for a specific report
     * @param reportId The ID of the report
     * @return List of report result associations
     */
    List<ReportResult> findByReportId(Integer reportId);

    /**
     * Find all report associations for a specific result
     * @param resultId The ID of the result
     * @return List of report result associations
     */
    List<ReportResult> findByResultId(Integer resultId);

    /**
     * Delete all result associations for a specific report
     * @param reportId The ID of the report
     */
    void deleteByReportId(Integer reportId);
}
