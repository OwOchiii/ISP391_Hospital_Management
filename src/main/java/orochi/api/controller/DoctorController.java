package orochi.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import orochi.dto.DoctorDTO;
import orochi.model.Doctor;
import orochi.repository.DoctorRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController(value = "apiDoctorController")
@RequestMapping("/api")
public class DoctorController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorController.class);

    @Autowired
    private DoctorRepository doctorRepository;

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorDTO>> getDoctors(@RequestParam(required = false) Integer specId) {
        try {
            logger.info("Fetching doctors for specId: {}", specId);
            List<Doctor> doctors = specId != null ? doctorRepository.findBySpecializationId(specId) : doctorRepository.findAll();
            List<DoctorDTO> doctorDTOs = doctors.stream()
                    .map(doctor -> new DoctorDTO(
                            doctor.getDoctorId(),
                            doctor.getUser() != null ? doctor.getUser().getFullName() : "Unknown",
                            doctor.getSpecializations().stream().findFirst().map(spec -> spec.getSpecId()).orElse(null)
                    ))
                    .collect(Collectors.toList());
            logger.info("Returning {} doctors", doctorDTOs.size());
            return ResponseEntity.ok(doctorDTOs);
        } catch (Exception e) {
            logger.error("Error fetching doctors for specId: {}", specId, e);
            return ResponseEntity.status(500).body(List.of());
        }
    }
}