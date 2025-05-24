package com.hospital.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Specialties")
public class Specialty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer specialtyId; // Changed to Integer

    @Column(name = "specialty_name")
    private String specialtyName;

    // Getters and Setters
    public Integer getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(Integer specialtyId) { this.specialtyId = specialtyId; }
    public String getSpecialtyName() { return specialtyName; }
    public void setSpecialtyName(String specialtyName) { this.specialtyName = specialtyName; }
}