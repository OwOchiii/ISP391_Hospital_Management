package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import orochi.model.Prescription;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {

    /**
     * Find prescriptions by appointment ID, ordered by prescription date (newest first)
     *
     * @param appointmentId The appointment ID
     * @return List of prescriptions for the appointment
     */
    List<Prescription> findByAppointmentIdOrderByPrescriptionDateDesc(Integer appointmentId);

    /**
     * Find prescriptions by patient ID, ordered by prescription date (newest first)
     *
     * @param patientId The patient ID
     * @return List of prescriptions for the patient
     */
    List<Prescription> findByPatientIdOrderByPrescriptionDateDesc(Integer patientId);

    /**
     * Find prescriptions by doctor ID, ordered by prescription date (newest first)
     *
     * @param doctorId The doctor ID
     * @return List of prescriptions written by the doctor
     */
    List<Prescription> findByDoctorIdOrderByPrescriptionDateDesc(Integer doctorId);

    /**
     * Find all prescriptions for a specific appointment
     *
     * @param appointmentId The appointment ID
     * @return List of prescriptions for the appointment
     */
    List<Prescription> findByAppointmentId(Integer appointmentId);

    /**
     * Find prescriptions for a patient created after a specific date
     *
     * @param patientId The patient ID
     * @param date      The date after which to find prescriptions
     * @return List of prescriptions for the patient after the given date
     */
    List<Prescription> findByPatientIdAndPrescriptionDateAfter(Integer patientId, LocalDateTime date);

    /**
     * Find paginated prescriptions by patient ID
     *
     * @param patientId The patient ID
     * @param pageable   Pagination information
     * @return Page of prescriptions for the patient
     */
    Page<Prescription> findByPatientId(Integer patientId, Pageable pageable);
}
