package orochi.dto;

import lombok.Getter;
import lombok.Setter;
import orochi.model.Doctor;
import orochi.model.Specialization;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class AppointmentDTO {
    private Long id;
    private String fullName;
    private String patientID;
    private String gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String email;
    private String addressType;
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String specialtyId;
    private String specialtyName;
    private String doctorId;
    private String doctorFullName;
    private Doctor doctor;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String room;
    private String appointmentStatus;
    private String reasonForVisit;
    private String paymentStatus;
    private List<Specialization> specializations;
}