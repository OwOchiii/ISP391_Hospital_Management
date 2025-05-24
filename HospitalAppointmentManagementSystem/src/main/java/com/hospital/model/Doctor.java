package com.hospital.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer doctorId; // Changed to Integer

    @Column(name = "doctor_name")
    private String doctorName;

    @ManyToOne
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    // Getters and Setters
    public Integer getDoctorId() { return doctorId; }
    public void setDoctorId(Integer doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public Specialty getSpecialty() { return specialty; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
}