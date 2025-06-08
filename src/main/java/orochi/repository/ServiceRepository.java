package orochi.repository;

import orochi.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {
    // Không cần thêm query tùy chỉnh nếu chỉ cần CRUD cơ bản
}