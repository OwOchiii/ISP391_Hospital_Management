package orochi.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import orochi.dto.MedicalResultSummaryDTO;
import orochi.dto.MedicalResultDetailDTO;
import orochi.model.MedicalResult;
import orochi.repository.MedicalResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MedicalResultFacade {

    private final MedicalResultRepository repo;

    public MedicalResultFacade(MedicalResultRepository repo) {
        this.repo = repo;
    }

    /**
     * Trả về trang các bản tóm tắt kết quả,
     * hỗ trợ filter theo appointmentId, status, doctorName, patientName, resultDate từ–đến
     */
    public Page<MedicalResultSummaryDTO> listSummaries(
            Integer appointmentId,
            String status,
            String doctorName,
            String patientName,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    ) {
        Specification<MedicalResult> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (appointmentId != null) {
                predicates.add(cb.equal(root.get("appointmentId"), appointmentId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
            }
            if (doctorName != null && !doctorName.isBlank()) {
                Join<?,?> docUser = root.join("doctor").join("user");
                predicates.add(cb.like(cb.lower(docUser.get("fullName")), "%" + doctorName.toLowerCase() + "%"));
            }
            if (patientName != null && !patientName.isBlank()) {
                Join<?,?> appt = root.join("appointment");
                Join<?,?> patUser = appt.join("patient").join("user");
                predicates.add(cb.like(cb.lower(patUser.get("fullName")), "%" + patientName.toLowerCase() + "%"));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("resultDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("resultDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(spec, pageable)
                .map(r -> {
                    var dto = new MedicalResultSummaryDTO();

                    // --- map các trường cốt lõi ---
                    dto.setResultId(r.getResultId());
                    dto.setResultDate(r.getResultDate());
                    dto.setStatus(r.getStatus());
                    dto.setFileUrl(r.getFileUrl());
                    dto.setAppointmentDateTime(r.getAppointment().getDateTime());
                    dto.setPatientName(r.getAppointment().getPatient().getFullName());
                    dto.setDoctorName(r.getDoctor().getUser().getFullName());

                    // --- map các trường mở rộng ---
                    if (r.getAppointment().getRoom() != null) {
                        dto.setAppointmentRoom(r.getAppointment().getRoom().getRoomName());
                    }
                    dto.setAppointmentEmail(r.getAppointment().getEmail());
                    dto.setAppointmentPhone(r.getAppointment().getPhoneNumber());

                    return dto;
                });
    }

    /**
     * Lấy detail đầy đủ của một MedicalResult (bổ sung email, phone, room…)
     */
    public MedicalResultDetailDTO getDetail(Integer id) {
        var r = repo.findByIdWithAllDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy result " + id));

        var dto = new MedicalResultDetailDTO();

        // --- map MedicalResult ---
        dto.setResultId(r.getResultId());
        dto.setResultDate(r.getResultDate());
        dto.setDescription(r.getDescription());
        dto.setStatus(r.getStatus());
        dto.setFileUrl(r.getFileUrl());

        // --- map Appointment ---
        var appt = r.getAppointment();
        dto.setAppointmentId(appt.getAppointmentId());
        dto.setAppointmentDateTime(appt.getDateTime());
        dto.setAppointmentStatus(appt.getStatus());
        dto.setAppointmentDescription(appt.getDescription());
        dto.setAppointmentEmail(appt.getEmail());
        dto.setAppointmentPhone(appt.getPhoneNumber());
        dto.setRoomName(appt.getRoom() != null ? appt.getRoom().getRoomName() : null);

        // --- map Patient ---
        var pat = appt.getPatient();
        dto.setPatientId(pat.getPatientId());
        dto.setPatientName(pat.getFullName());
        dto.setPatientGender(pat.getGender());
        dto.setPatientDob(pat.getDateOfBirth());
        // ← MỚI: email của bệnh nhân
        dto.setPatientEmail(pat.getUser().getEmail());

        // --- map Doctor ---
        var doc = r.getDoctor();
        dto.setDoctorId(doc.getDoctorId());
        dto.setDoctorName(doc.getUser().getFullName());
        dto.setDoctorBio(doc.getBioDescription());
        // ← MỚI: thông tin liên lạc bác sĩ (ví dụ email)
        dto.setDoctorContact(doc.getUser().getEmail());

        // --- map Orders ---
        List<MedicalResultDetailDTO.OrderInfo> orders = r.getOrders().stream()
                .map(o -> {
                    var oi = new MedicalResultDetailDTO.OrderInfo();
                    oi.setOrderId(o.getOrderId());
                    oi.setOrderType(o.getOrderType());
                    oi.setOrderStatus(o.getStatus());
                    oi.setOrderDate(o.getOrderDate().toLocalDate());
                    oi.setAssignedDept(o.getAssignedToDepartment() != null
                            ? o.getAssignedToDepartment().getDeptName()
                            : null);
                    return oi;
                })
                .toList();
        dto.setOrders(orders);

        return dto;
    }
}
