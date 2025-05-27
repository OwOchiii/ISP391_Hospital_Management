package com.hospital.service;

import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import com.hospital.model.Specialty;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    public List<Appointment> getAppointmentsByPatient(int patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Specialty> getAllSpecialties() {
        return specialtyRepository.findAll();
    }

    public List<Doctor> getDoctorsBySpecialty(int specialtyId) {
        return doctorRepository.findBySpecialtyId(specialtyId);
    }

    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public void saveAppointment(Appointment appointment) {
        appointmentRepository.save(appointment);
    }

    public Patient findPatientById(int id) {
        Optional<Patient> patient = patientRepository.findById(id);
        return patient.orElse(null);
    }

    public List<Doctor> searchDoctors(String query) {
        try {
            int specialtyId = Integer.parseInt(query);
            return doctorRepository.findBySpecialtyId(specialtyId);
        } catch (NumberFormatException e) {
            return doctorRepository.findAll().stream()
                    .filter(d -> d.getDoctorName().toLowerCase().contains(query.toLowerCase()))
                    .toList();
        }
    }

    // New method to fetch appointments by date
    public List<Appointment> getAppointmentsByDate(Date date) {
        return appointmentRepository.findByAppointmentDate(date);
    }
}