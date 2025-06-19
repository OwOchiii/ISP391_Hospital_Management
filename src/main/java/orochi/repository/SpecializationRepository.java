package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.Specialization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;


import java.util.List;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Integer> {
    List<Specialization> findAllByOrderBySpecNameAsc();

    @Query("SELECT s FROM Specialization s ORDER BY s.specName ASC")
    Page<Specialization> findAllSpecializations(Pageable pageable);
}
