package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orochi.model.Receptionist;

import java.util.Optional;

@Repository
public interface ReceptionistEntityRepository extends JpaRepository<Receptionist, Integer> {

    /**
     * Find a Receptionist by UserId
     * @param userId The User ID to search for
     * @return Optional containing the Receptionist if found
     */
    Optional<Receptionist> findByUserId(Integer userId);

    /**
     * Check if a Receptionist exists for a given UserId
     * @param userId The User ID to check
     * @return true if exists, false otherwise
     */
    boolean existsByUserId(Integer userId);

    /**
     * Delete a Receptionist by UserId
     * @param userId The User ID
     */
    void deleteByUserId(Integer userId);
}
