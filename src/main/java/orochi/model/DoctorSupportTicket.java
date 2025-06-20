package orochi.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_support_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class DoctorSupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Integer doctorId;

    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType;

    @Column(name = "other_request_type", length = 100)
    private String otherRequestType;

    @Column(name = "ticket_title", nullable = false, length = 200)
    private String requestTitle;

    @Column(name = "affected_module", nullable = false, length = 50)
    private String affectedModule;

    @Column(name = "other_module", length = 100)
    private String otherModule;

    @Column(name = "priority_level", nullable = false, length = 20)
    private String priorityLevel;

    @Column(name = "description", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String description;

    @Column(name = "justification", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String justification;

    @Column(name = "attachment_path", length = 255)
    private String attachmentPath;

    @Column(name = "follow_up_needed", nullable = false)
    private Boolean followUpNeeded = false;

    @Column(name = "preferred_contact_method", length = 20)
    private String preferredContactMethod;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "preferred_contact_times", length = 100)
    private String preferredContactTimes;

    @Column(name = "additional_contact_info", columnDefinition = "VARCHAR(MAX)")
    private String additionalContactInfo;

    @Column(name = "ticket_status", nullable = false, length = 20)
    private String ticketStatus = "PENDING";

    @Column(name = "submission_date", nullable = false)
    private LocalDateTime submissionDate = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "doctor_id", insertable = false, updatable = false)
    private Doctor doctor;
}
