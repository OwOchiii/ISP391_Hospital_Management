package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import orochi.model.Specialization;

public interface SpecializationRepository extends JpaRepository<Specialization, Integer> {
}
