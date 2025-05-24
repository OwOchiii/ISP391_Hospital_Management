package com.hospital.repository;

import com.hospital.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    @Query("SELECT a FROM Appointment a WHERE a.patient.patientId = :patientId")
    List<Appointment> findByPatientId(@Param("patientId") int patientId);
}