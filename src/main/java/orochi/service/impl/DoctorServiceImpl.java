package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.repository.AppointmentRepository;
import orochi.repository.DoctorRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /**
     * Get doctors by specialty ID and return as List<Map<String, Object>>
     * This method is used by the API controller for the frontend
     */
    public List<Map<String, Object>> getDoctorsBySpecialtyId(Integer specialtyId) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyId(specialtyId);

        return doctors.stream()
                .map(doctor -> {
                    Map<String, Object> doctorData = new HashMap<>();
                    doctorData.put("doctorId", doctor.getDoctorId());
                    doctorData.put("fullName", doctor.getUser().getFullName());
                    doctorData.put("specialtyName", doctor.getSpecializations().isEmpty() ?
                        "General Practice" :
                        doctor.getSpecializations().get(0).getSpecName());
                    doctorData.put("bioDescription", doctor.getBioDescription());
                    return doctorData;
                })
                .collect(Collectors.toList());
    }

    public List<Appointment> getBookedTimeSlots(int doctorId, LocalDate date) {
        // This method should return a list of booked time slots for a specific doctor on a given date.

        return appointmentRepository.findByDoctorIdAndDate(
                doctorId, date);
    }
}
