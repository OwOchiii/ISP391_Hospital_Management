package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import orochi.model.Users;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer> {
    // Basic CRUD operations provided by JpaRepository
    Optional<Users> findByEmail(String email);
}

