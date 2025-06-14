package orochi.service.impl;

import org.springframework.stereotype.Service;
import orochi.model.Doctor;
import orochi.repository.DoctorRepository;

import java.util.List;
import java.util.Optional;

@Service("doctorServiceImpl")
public class DoctorServiceImpl {

    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    // Get all doctors
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    // Get doctor by ID
    public Optional<Doctor> getDoctorById(Integer id) {
        return doctorRepository.findById(id);
    }



    // Get top-rated doctors

}
