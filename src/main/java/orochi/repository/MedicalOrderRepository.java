package orochi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.MedicalOrder;

import java.util.List;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Integer> {

    // Find orders by appointment ID
    List<MedicalOrder> findByAppointmentIdOrderByOrderDate(Integer appointmentId);

    // Paginated version
    Page<MedicalOrder> findByAppointmentId(Integer appointmentId, Pageable pageable);

    // Find orders created by a specific doctor with a particular status
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId AND m.status = :status ORDER BY m.orderDate")
    List<MedicalOrder> findByDoctorIdAndStatus(@Param("doctorId") Integer doctorId, @Param("status") String status);

    // Paginated version
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId AND m.status = :status")
    Page<MedicalOrder> findByDoctorIdAndStatus(@Param("doctorId") Integer doctorId, @Param("status") String status, Pageable pageable);

    // Find all orders for a specific doctor
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId ORDER BY m.orderDate")
    List<MedicalOrder> findByDoctorId(@Param("doctorId") Integer doctorId);

    // Paginated version
    @Query("SELECT m FROM MedicalOrder m WHERE m.doctorId = :doctorId")
    Page<MedicalOrder> findByDoctorId(@Param("doctorId") Integer doctorId, Pageable pageable);

    List<MedicalOrder> findByAppointmentPatientIdOrderByOrderDateDesc(Integer patientId);


}
