package orochi.service;

import orochi.model.MedicalResult;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface MedicalResultService {

    /**
     * Get all medical results for a specific order
     *
     * @param orderId The ID of the medical order
     * @return List of medical results associated with the order
     */
    List<MedicalResult> getResultsForOrder(Long orderId);

    /**
     * Get a specific medical result by ID
     *
     * @param resultId The ID of the medical result
     * @return The medical result, or null if not found
     */
    MedicalResult getResultById(Integer resultId);

    /**
     * Get all medical results for a specific appointment
     *
     * @param appointmentId The ID of the appointment
     * @return List of medical results associated with the appointment
     */
    List<MedicalResult> getResultsForAppointment(Integer appointmentId);

    /**
     * Get all medical results for a specific doctor
     *
     * @param doctorId The ID of the doctor
     * @return List of medical results created by the doctor
     */
    List<MedicalResult> getResultsByDoctor(Integer doctorId);

    /**
     * Update an existing medical result
     *
     * @param resultId The ID of the medical result to update
     * @param description The updated description
     * @param status The updated status
     * @param resultFile The updated file (or null if no new file)
     * @return The updated medical result
     */
    MedicalResult updateMedicalResult(Integer resultId, String description, String status, MultipartFile resultFile);

    /**
     * Delete a medical result
     *
     * @param resultId The ID of the medical result to delete
     * @return true if successful, false otherwise
     */
    boolean deleteMedicalResult(Integer resultId);
}
