package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import orochi.model.Users;

public interface UserRepository extends JpaRepository<Users, Integer> {
    // Basic CRUD operations provided by JpaRepository
}