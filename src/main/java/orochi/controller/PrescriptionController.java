package orochi.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import orochi.model.Medicine;
import orochi.model.Prescription;
import orochi.repository.MedicineRepository;
import orochi.repository.PrescriptionRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/doctor/prescriptions")
public class PrescriptionController {

    private static final Logger logger = LoggerFactory.getLogger(PrescriptionController.class);

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/create")
    @ResponseBody
    @Transactional
    public Map<String, Object> createPrescription(
            @RequestParam Integer appointmentId,
            @RequestParam Integer patientId,
            @RequestParam Integer doctorId,
            @RequestParam(required = false) String notes) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Create and save the prescription first
            Prescription prescription = new Prescription();
            prescription.setAppointmentId(appointmentId);
            prescription.setPatientId(patientId);
            prescription.setDoctorId(doctorId);
            prescription.setNotes(notes);
            prescription.setPrescriptionDate(LocalDateTime.now());

            // Save the prescription to get an ID before adding medications
            prescription = prescriptionRepository.save(prescription);

            response.put("success", true);
            response.put("message", "Prescription created successfully");
            response.put("prescriptionId", prescription.getPrescriptionId());

        } catch (Exception e) {
            logger.error("Error creating prescription", e);
            response.put("success", false);
            response.put("message", "Failed to create prescription: " + e.getMessage());
        }

        return response;
    }

    @PostMapping("/update-notes")
    @ResponseBody
    public ResponseEntity<?> updatePrescriptionNotes(
            @RequestParam Integer prescriptionId,
            @RequestParam String notes) {

        try {
            logger.info("Updating notes for prescription ID: {}", prescriptionId);

            Optional<Prescription> prescriptionOpt = prescriptionRepository.findById(prescriptionId);
            if (prescriptionOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Prescription not found"));
            }

            Prescription prescription = prescriptionOpt.get();
            prescription.setNotes(notes);
            prescriptionRepository.save(prescription);

            logger.info("Successfully updated notes for prescription ID: {}", prescriptionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Prescription notes updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating prescription notes", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update prescription notes: " + e.getMessage()));
        }
    }

    @PostMapping("/delete/{prescriptionId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deletePrescription(@PathVariable Integer prescriptionId) {
        try {
            logger.info("Attempting to delete prescription with ID: {}", prescriptionId);

            // Check if prescription exists
            boolean exists = prescriptionRepository.existsById(prescriptionId);
            if (!exists) {
                logger.warn("Prescription with ID {} not found", prescriptionId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Prescription not found"
                ));
            }

            // First approach: Delete related medicines using repository
            try {
                logger.info("Deleting medicines for prescription ID: {}", prescriptionId);
                int deletedMedicines = medicineRepository.deleteMedicinesByPrescriptionId(prescriptionId);
                logger.info("Deleted {} medicines for prescription ID: {}", deletedMedicines, prescriptionId);

                // Then delete the prescription
                prescriptionRepository.deleteById(prescriptionId);
                logger.info("Successfully deleted prescription with ID: {}", prescriptionId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Prescription and associated medicines deleted successfully"
                ));
            } catch (Exception e) {
                // Log the exception but don't rethrow it
                logger.error("Error during prescription deletion using repository methods", e);

                // Use second approach with JDBC if the first fails
                try {
                    logger.info("Falling back to direct JDBC to delete medicines for prescription ID: {}", prescriptionId);
                    Session session = entityManager.unwrap(Session.class);

                    // Execute native SQL to delete medicines first
                    int deletedMedicines = session.createNativeMutationQuery("DELETE FROM Medicine WHERE PrescriptionID = :prescriptionId")
                            .setParameter("prescriptionId", prescriptionId)
                            .executeUpdate();
                    logger.info("Deleted {} medicines for prescription ID: {}", deletedMedicines, prescriptionId);

                    // Execute native SQL to delete the prescription
                    int deletedPrescriptions = session.createNativeMutationQuery("DELETE FROM Prescription WHERE PrescriptionID = :prescriptionId")
                            .setParameter("prescriptionId", prescriptionId)
                            .executeUpdate();
                    logger.info("Deleted {} prescriptions with ID: {}", deletedPrescriptions, prescriptionId);

                    if (deletedPrescriptions == 0) {
                        throw new IllegalStateException("Failed to delete prescription");
                    }

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Prescription and associated medicines deleted successfully using JDBC"
                    ));
                } catch (Exception jdbcEx) {
                    // Log the exception and throw a new one to properly roll back the transaction
                    logger.error("Error during prescription deletion using JDBC", jdbcEx);
                    throw new RuntimeException("Failed to delete prescription: " + jdbcEx.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Unhandled error during prescription deletion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to delete prescription: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/medicine/add")
    @ResponseBody
    @Transactional
    public Map<String, Object> addMedicineToPresciption(
            @RequestParam Integer prescriptionId,
            @RequestParam Integer inventoryId,
            @RequestParam String dosage,
            @RequestParam String frequency,
            @RequestParam String duration,
            @RequestParam(required = false) String instructions) {

        Map<String, Object> response = new HashMap<>();

        try {
            // First fetch the prescription to ensure it exists
            Optional<Prescription> prescriptionOpt = prescriptionRepository.findById(prescriptionId);

            if (prescriptionOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Prescription not found");
                return response;
            }

            Prescription prescription = prescriptionOpt.get();

            // Create new medicine entity
            Medicine medicine = new Medicine();
            medicine.setPrescription(prescription); // This will also set the prescriptionId
            medicine.setInventoryId(inventoryId);
            medicine.setDosage(dosage);
            medicine.setFrequency(frequency);
            medicine.setDuration(duration);
            medicine.setInstructions(instructions);

            // Save the medicine
            medicineRepository.save(medicine);

            response.put("success", true);
            response.put("message", "Medication added successfully");

        } catch (Exception e) {
            logger.error("Error adding medicine to prescription", e);
            response.put("success", false);
            response.put("message", "Failed to add medication: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/medicine/{medicineId}")
    @ResponseBody
    public ResponseEntity<?> getMedicineDetails(@PathVariable Integer medicineId) {
        try {
            logger.info("Fetching details for medicine ID: {}", medicineId);

            Optional<Medicine> medicineOpt = medicineRepository.findById(medicineId);
            if (medicineOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Medicine not found"));
            }

            Medicine medicine = medicineOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("medicine", medicine);

            logger.info("Successfully fetched details for medicine ID: {}", medicineId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching medicine details", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch medicine details: " + e.getMessage()));
        }
    }

    @PostMapping("/medicine/update/{medicineId}")
    @ResponseBody
    public ResponseEntity<?> updateMedicine(
            @PathVariable Integer medicineId,
            @RequestParam Integer prescriptionId,
            @RequestParam Integer inventoryId,
            @RequestParam String dosage,
            @RequestParam String frequency,
            @RequestParam String duration,
            @RequestParam(required = false) String instructions) {

        try {
            logger.info("Updating medicine ID: {}", medicineId);

            Optional<Medicine> medicineOpt = medicineRepository.findById(medicineId);
            if (medicineOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Medicine not found"));
            }

            Medicine medicine = medicineOpt.get();
            medicine.setInventoryId(inventoryId);
            medicine.setDosage(dosage);
            medicine.setFrequency(frequency);
            medicine.setDuration(duration);
            medicine.setInstructions(instructions);

            medicineRepository.save(medicine);

            logger.info("Successfully updated medicine ID: {}", medicineId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Medication updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating medicine", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update medication: " + e.getMessage()));
        }
    }



    @PostMapping("/medicine/delete/{medicineId}")
    @ResponseBody
    public ResponseEntity<?> deleteMedicine(@PathVariable Integer medicineId) {
        try {
            logger.info("Deleting medicine ID: {}", medicineId);

            if (!medicineRepository.existsById(medicineId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Medicine not found"));
            }

            medicineRepository.deleteById(medicineId);

            logger.info("Successfully deleted medicine ID: {}", medicineId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Medication removed successfully"));
        } catch (Exception e) {
            logger.error("Error deleting medicine", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to remove medication: " + e.getMessage()));
        }
    }
}
