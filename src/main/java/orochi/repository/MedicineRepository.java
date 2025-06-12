package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.Medicine;
import orochi.model.Prescription;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Integer> {

    List<Medicine> findByPrescriptionId(Integer prescriptionId);

    @Modifying
    @Transactional
    void deleteByPrescriptionId(Integer prescriptionId);

    @Modifying
    @Query(value = "DELETE FROM Medicine WHERE PrescriptionID = :prescriptionId", nativeQuery = true)
    @Transactional
    int deleteMedicinesByPrescriptionId(@Param("prescriptionId") Integer prescriptionId);

    void deleteByPrescription(Prescription prescription);
}

