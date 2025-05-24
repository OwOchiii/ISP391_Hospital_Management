package com.hospital.repository;

import com.hospital.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {
    @Query("SELECT d FROM Doctor d WHERE d.specialty.specialtyId = :specialtyId")
    List<Doctor> findBySpecialtyId(@Param("specialtyId") int specialtyId);
}