package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.DoctorEducation;

import java.util.List;

@Repository
public interface DoctorEducationRepository extends JpaRepository<DoctorEducation, Integer> {

    /**
     * Find all education records for a specific doctor
     *
     * @param doctorId The doctor's ID
     * @return List of education records
     */
    List<DoctorEducation> findByDoctorId(Integer doctorId);

    /**
     * Delete all education records for a specific doctor
     *
     * @param doctorId The doctor's ID
     */
    void deleteByDoctorId(Integer doctorId);
}
