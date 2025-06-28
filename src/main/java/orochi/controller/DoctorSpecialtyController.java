package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orochi.dto.DoctorDTO;
import orochi.model.Appointment;
import orochi.model.Doctor;
import orochi.service.impl.DoctorServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorSpecialtyController {

    private final DoctorServiceImpl doctorService;

    @Autowired
    public DoctorSpecialtyController(DoctorServiceImpl doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/specialty/{specialtyId}")
    public ResponseEntity<?> getDoctorSpecialties(
            @PathVariable(value = "specialtyId") Integer specialtyId
    ) {
        List<Doctor> lstDoctor = doctorService.getBySpecialtyId(specialtyId);


        List<DoctorDTO> lstDoctorDTO = lstDoctor.stream().map(
                d  -> DoctorDTO.builder()
                        .doctorId(d.getDoctorId())
                        .bioDescription(d.getBioDescription())
                        .fullName(d.getUser().getFullName())
                        .build()
        ).toList();

        return ResponseEntity.ok().body(lstDoctorDTO);
    }

    @GetMapping("/appointment")
    public ResponseEntity<?> getBookedTimeSlots(
            @RequestParam("doctorId") Integer doctorId,
            @RequestParam("date") LocalDate date) {

        List<Appointment> appointments = doctorService.getBookedTimeSlots(doctorId, date);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        var data = appointments.stream()
                .map(appointment -> appointment.getDateTime().format(timeFormatter))
                .toList();

        return ResponseEntity.ok().body(data);
    }
}
