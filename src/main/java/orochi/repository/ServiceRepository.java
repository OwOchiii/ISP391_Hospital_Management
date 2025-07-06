package orochi.repository;

import orochi.model.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ServiceRepository extends JpaRepository<MedicalService, Integer> {
    @Modifying
    @Query("DELETE FROM MedicalService s WHERE s.specialization.specId = :specId")
    void deleteBySpecId(@Param("specId") Integer specId);

    Page<MedicalService> findByServiceNameContainingIgnoreCase(String name, Pageable pageable);

    Page<MedicalService> findBySpecId(Integer specId, Pageable pageable);

    Page<MedicalService> findByServiceNameContainingIgnoreCaseAndSpecId(
            String name, Integer specId, Pageable pageable);
}