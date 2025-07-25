package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.DoctorSpecialization;
import orochi.model.DoctorSpecializationId;

import java.util.Optional;

public interface DoctorSpecializationRepository extends JpaRepository<DoctorSpecialization, DoctorSpecializationId> {

    @Query("SELECT ds FROM DoctorSpecialization ds WHERE ds.doctor.doctorId = :doctorId")
    Optional<DoctorSpecialization> findByDoctorId(@Param("doctorId") Integer doctorId);

    @Modifying
    @Query("DELETE FROM DoctorSpecialization ds WHERE ds.specialization.specId = :specId")
    void deleteBySpecId(@Param("specId") Integer specId);

    // Fix: Use explicit query instead of method name convention
    @Query("SELECT CASE WHEN COUNT(ds) > 0 THEN true ELSE false END FROM DoctorSpecialization ds WHERE ds.doctor.doctorId = :doctorId AND ds.specialization.specId = :specId")
    boolean existsByDoctorIdAndSpecId(@Param("doctorId") Integer doctorId, @Param("specId") Integer specId);

    @Modifying
    @Query("DELETE FROM DoctorSpecialization ds WHERE ds.doctor.doctorId = :doctorId AND ds.specialization.specId = :specId")
    void deleteByDoctorIdAndSpecId(@Param("doctorId") Integer doctorId, @Param("specId") Integer specId);
}