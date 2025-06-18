package orochi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.model.DoctorSupportTicket;
import orochi.repository.DoctorSupportTicketRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorSupportTicketService {

    @Autowired
    private DoctorSupportTicketRepository doctorSupportTicketRepository;

    public List<DoctorSupportTicket> getTicketsByDoctorId(Integer doctorId) {
        return doctorSupportTicketRepository.findByDoctorIdOrderBySubmissionDateDesc(doctorId);
    }

    public Optional<DoctorSupportTicket> getTicketById(Long id) {
        return doctorSupportTicketRepository.findById(id);
    }

    public DoctorSupportTicket saveTicket(DoctorSupportTicket ticket) {
        return doctorSupportTicketRepository.save(ticket);
    }

    public boolean isAuthorizedToView(DoctorSupportTicket ticket, Integer doctorId) {
        return ticket.getDoctorId().equals(doctorId);
    }
}
