package com.hospital.repository;

import com.hospital.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findTop5ByOrderByIdDesc();

    @Query("SELECT s.name, COUNT(a) FROM Appointment a JOIN a.status s GROUP BY s.name")
    List<Object[]> countAppointmentsByStatus();
}