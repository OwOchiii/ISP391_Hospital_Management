package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import orochi.model.DoctorSupportTicket;

import java.util.List;

@Repository
public interface DoctorSupportTicketRepository extends JpaRepository<DoctorSupportTicket, Long> {
    List<DoctorSupportTicket> findByDoctorIdOrderBySubmissionDateDesc(Integer doctorId);
}
