package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import orochi.model.Specialization;
import orochi.repository.SpecializationRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SpecializationController {

    @Autowired
    private SpecializationRepository specializationRepository;

    @GetMapping("/specializations")
    public ResponseEntity<List<Specialization>> getSpecializations() {
        List<Specialization> specializations = specializationRepository.findAll();
        return ResponseEntity.ok(specializations);
    }
}