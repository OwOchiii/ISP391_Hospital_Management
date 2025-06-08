package orochi.repository;

import orochi.model.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<MedicalService, Integer> {
    // Không cần thêm query tùy chỉnh nếu chỉ cần CRUD cơ bản
}