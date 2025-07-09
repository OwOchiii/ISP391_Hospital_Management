package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.Specialization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Integer> {
    List<Specialization> findAllByOrderBySpecNameAsc();

    @Query("SELECT s FROM Specialization s")
    Page<Specialization> findAllSpecializations(Pageable pageable);

    Page<Specialization> findBySpecNameContainingIgnoreCaseAndSymptomContainingIgnoreCase(
            String specName, String symptom, Pageable pageable);

    @Query("SELECT DISTINCT s.symptom FROM Specialization s WHERE s.symptom IS NOT NULL")
    List<String> findDistinctSymptoms();
}