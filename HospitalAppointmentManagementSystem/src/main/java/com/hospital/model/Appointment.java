package com.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Time;
import java.util.Date;

@Entity
@Table(name = "Appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int appointmentId;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = true)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Column(name = "appointment_date")
    @NotNull(message = "Appointment date is required")
    private Date appointmentDate;

    @Column(name = "appointment_time")
    @NotNull(message = "Appointment time is required")
    private Time appointmentTime;

    @Column(name = "reason")
    @NotNull(message = "Reason is required")
    private String reason;

    @Column(name = "status")
    private String status;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "approved_date")
    private Date approvedDate;

    // Constructors
    public Appointment() {}
    public Appointment(Patient patient, Specialty specialty, Date appointmentDate, Time appointmentTime, String reason) {
        this.patient = patient;
        this.specialty = specialty;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.reason = reason;
        this.status = "Pending";
        this.createdDate = new Date();
    }

    // Getters and Setters
    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public Specialty getSpecialty() { return specialty; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
    public Date getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(Date appointmentDate) { this.appointmentDate = appointmentDate; }
    public Time getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(Time appointmentTime) { this.appointmentTime = appointmentTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    public Date getApprovedDate() { return approvedDate; }
    public void setApprovedDate(Date approvedDate) { this.approvedDate = approvedDate; }

    // Helper methods for form binding
    public Integer getSpecialtyId() {
        return specialty != null ? specialty.getSpecialtyId() : null;
    }
    public void setSpecialtyId(Integer specialtyId) {
        if (specialtyId == null) {
            this.specialty = null;
        } else {
            this.specialty = new Specialty();
            this.specialty.setSpecialtyId(specialtyId);
        }
    }
    public Integer getDoctorId() {
        return doctor != null ? doctor.getDoctorId() : null;
    }
    public void setDoctorId(Integer doctorId) {
        if (doctorId == null) {
            this.doctor = null;
        } else {
            this.doctor = new Doctor();
            this.doctor.setDoctorId(doctorId);
        }
    }
}