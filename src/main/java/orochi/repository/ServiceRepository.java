package orochi.repository;

import orochi.model.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<MedicalService, Integer> {
    @Modifying
    @Query("DELETE FROM MedicalService s WHERE s.specialization.specId = :specId")
    void deleteBySpecId(@Param("specId") Integer specId);
}