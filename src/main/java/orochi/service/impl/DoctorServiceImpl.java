package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.repository.AppointmentRepository;
import orochi.repository.DoctorRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service("doctorServiceImpl")
public class DoctorServiceImpl {

    private final DoctorRepository doctorRepository;

    private final AppointmentRepository appointmentRepository;

    @Autowired
    public DoctorServiceImpl(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // Get all doctors
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    // Get doctor by ID
    public Optional<Doctor> getDoctorById(Integer id) {
        return doctorRepository.findById(id);
    }

    public List<Doctor> getBySpecialtyId(int specialtyId) {
        return doctorRepository.findBySpecialtyId(specialtyId);
    }

    public List<Appointment> getBookedTimeSlots(int doctorId, LocalDate date) {
        // This method should return a list of booked time slots for a specific doctor on a given date.

        return appointmentRepository.findByDoctorIdAndDate(
                doctorId, date);
    }
}
