package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import orochi.model.MedicineInventory;

import java.util.List;

public interface MedicineInventoryRepository extends JpaRepository<MedicineInventory, Integer> {

    List<MedicineInventory> findByCurrentStockGreaterThan(Integer minStock);

    List<MedicineInventory> findByMedicineNameContainingIgnoreCase(String name);
}