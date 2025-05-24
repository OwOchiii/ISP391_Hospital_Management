package com.hospital.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Table(name = "Patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int patientId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "gender")
    private char gender;

    @Column(name = "birthdate")
    @NotNull(message = "Birthdate is required")
    private Date birthdate;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    // Constructors
    public Patient() {}
    public Patient(String fullName, char gender, Date birthdate, String phoneNumber, String email) {
        this.fullName = fullName;
        this.gender = gender;
        this.birthdate = birthdate;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    // Getters and Setters
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public char getGender() { return gender; }
    public void setGender(char gender) { this.gender = gender; }
    public Date getBirthdate() { return birthdate; }
    public void setBirthdate(Date birthdate) { this.birthdate = birthdate; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}