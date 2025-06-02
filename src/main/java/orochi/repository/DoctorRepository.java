package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Doctor;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {

    // Find a doctor by the email of the associated user
    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE u.email = :email")
    Optional<Doctor> findByUserEmail(@Param("email") String email);

    // Find a doctor by user ID
    Optional<Doctor> findByUserId(Integer userId);

    // Find a doctor by the full name of the associated user (case insensitive)
    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Optional<Doctor> findByUserFullNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT d FROM Doctor d JOIN d.specializations s WHERE s.specId = :specId")
    List<Doctor> findBySpecializationId(@Param("specId") Integer specId);
}

