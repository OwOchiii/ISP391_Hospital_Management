package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.MedicalReport;

import java.util.List;

@Repository
public interface MedicalReportRepository extends JpaRepository<MedicalReport, Integer> {

    /**
     * Find all medical reports for a specific appointment
     * @param appointmentId The ID of the appointment
     * @return List of medical reports for the appointment
     */
    List<MedicalReport> findByAppointmentIdOrderByReportDateDesc(Integer appointmentId);

    /**
     * Find all medical reports created by a specific doctor
     * @param doctorId The ID of the doctor
     * @return List of medical reports created by the doctor
     */
    List<MedicalReport> findByDoctorIdOrderByReportDateDesc(Integer doctorId);
}
