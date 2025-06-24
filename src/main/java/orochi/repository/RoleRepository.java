// src/main/java/orochi/repository/RoleRepository.java
package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import orochi.model.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

}
