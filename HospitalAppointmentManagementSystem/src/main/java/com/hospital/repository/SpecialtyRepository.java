package com.hospital.repository;

import com.hospital.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
}