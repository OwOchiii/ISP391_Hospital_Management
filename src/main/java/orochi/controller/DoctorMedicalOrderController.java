package orochi.controller;

import orochi.model.MedicalOrder;
import orochi.model.MedicalResult;
import orochi.service.MedicalOrderService;
import orochi.service.MedicalResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/doctor/medical-orders")
public class DoctorMedicalOrderController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorMedicalOrderController.class);

    @Autowired
    private MedicalOrderService medicalOrderService;

    @Autowired
    private MedicalResultService medicalResultService;

    @GetMapping("/{orderId}")
    public String viewMedicalOrderDetails(@PathVariable Long orderId,
                                          @RequestParam Long doctorId,
                                          Model model) {
        try {
            logger.debug("Fetching medical order details for order ID: {}", orderId);
            long startTime = System.currentTimeMillis();

            MedicalOrder order = medicalOrderService.getMedicalOrderById(orderId);

            long fetchTime = System.currentTimeMillis() - startTime;
            logger.info("Medical order fetch time: {}ms", fetchTime);

            if (order == null) {
                model.addAttribute("errorMessage", "Medical order not found.");
                return "error";
            }

            // Add medical order details to the model
            model.addAttribute("order", order);
            model.addAttribute("doctorId", doctorId);

            // Fetch and add medical results
            List<MedicalResult> results = medicalResultService.getResultsForOrder(orderId);
            model.addAttribute("results", results);

            logger.info("Fetched {} medical results for order ID: {}",
                    results != null ? results.size() : 0, orderId);

            // Add appointment information if available
            if (order.getAppointment() != null) {
                model.addAttribute("appointment", order.getAppointment());
                model.addAttribute("appointmentId", order.getAppointment().getAppointmentId());
                model.addAttribute("patientName", order.getAppointment().getPatient().getFullName());
                model.addAttribute("doctorName", order.getAppointment().getDoctor().getUser().getFullName());
            } else {
                // Handle case when no appointment is associated
                model.addAttribute("appointment", null);
                model.addAttribute("appointmentId", null);
                model.addAttribute("patientName", "N/A");
                model.addAttribute("doctorName", "N/A");
            }

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Total processing time for medical order details: {}ms", totalTime);

            return "doctor/medical-order-details";

        } catch (Exception e) {
            logger.error("Error fetching medical order details for order ID: {}", orderId, e);
            model.addAttribute("errorMessage", "Failed to retrieve medical order details: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{orderId}/update-status")
    public String updateOrderStatus(@PathVariable Long orderId,
                                   @RequestParam String status,
                                   @RequestParam Long doctorId,
                                   @RequestParam(required = false) Long appointmentId,
                                   RedirectAttributes redirectAttributes) {
        try {
            medicalOrderService.updateOrderStatus(orderId, status);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Medical order status updated successfully to " + status);

            logger.debug("Updated medical order status for order ID: {} to {}", orderId, status);

            // If appointmentId is provided, redirect back to appointment details
            if (appointmentId != null) {
                return "redirect:/doctor/appointments/" + appointmentId + "?doctorId=" + doctorId + "&tab=medical";
            }

            // Otherwise, redirect back to the order details
            return "redirect:/doctor/medical-orders/" + orderId + "?doctorId=" + doctorId;

        } catch (Exception e) {
            logger.error("Error updating medical order status for order ID: {}", orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to update medical order status: " + e.getMessage());

            // If appointmentId is provided, redirect back to appointment details
            if (appointmentId != null) {
                return "redirect:/doctor/appointments/" + appointmentId + "?doctorId=" + doctorId + "&tab=medical";
            }

            // Otherwise, redirect back to the order details
            return "redirect:/doctor/medical-orders/" + orderId + "?doctorId=" + doctorId;
        }
    }

    @PostMapping("/update-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateOrderStatusAjax(
            @RequestParam Long orderId,
            @RequestParam String status,
            @RequestParam Long doctorId) {

        Map<String, Object> response = new HashMap<>();

        try {
            logger.debug("AJAX request to update medical order status for order ID: {} to {}", orderId, status);
            long startTime = System.currentTimeMillis();

            medicalOrderService.updateOrderStatus(orderId, status);

            long updateTime = System.currentTimeMillis() - startTime;
            logger.info("Time to update order status via AJAX: {}ms", updateTime);

            response.put("success", true);
            response.put("message", "Medical order status updated successfully to " + status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating medical order status via AJAX for order ID: {}", orderId, e);

            response.put("success", false);
            response.put("message", "Failed to update medical order status: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{orderId}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteOrder(
            @PathVariable Long orderId,
            @RequestParam Long doctorId) {

        Map<String, Object> response = new HashMap<>();

        try {
            logger.debug("Request to delete medical order ID: {}", orderId);
            // Call service method to delete the order
            boolean deleted = medicalOrderService.deleteOrder(orderId);

            if (deleted) {
                response.put("success", true);
                response.put("message", "Medical order deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Medical order not found or could not be deleted");
                return ResponseEntity.status(404).body(response);
            }

        } catch (Exception e) {
            logger.error("Error deleting medical order ID: {}", orderId, e);

            response.put("success", false);
            response.put("message", "Failed to delete medical order: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{orderId}/add-result")
    public String addMedicalResult(
            @PathVariable Long orderId,
            @RequestParam Long doctorId,
            @RequestParam String description,
            @RequestParam String status,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile resultFile,
            RedirectAttributes redirectAttributes) {

        try {
            logger.debug("Adding medical result for order ID: {}", orderId);

            // Call service method to add medical result
            // You'll need to implement this in your MedicalOrderService
            medicalOrderService.addMedicalResult(orderId, description, status, resultFile);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Medical result added successfully");

            return "redirect:/doctor/medical-orders/" + orderId + "?doctorId=" + doctorId;

        } catch (Exception e) {
            logger.error("Error adding medical result for order ID: {}", orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to add medical result: " + e.getMessage());

            return "redirect:/doctor/medical-orders/" + orderId + "?doctorId=" + doctorId;
        }
    }

    @PostMapping("/{orderId}/edit-result")
    public String editMedicalResult(
            @PathVariable Long orderId,
            @RequestParam Long doctorId,
            @RequestParam Integer resultId,
            @RequestParam String description,
            @RequestParam String status,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile resultFile,
            RedirectAttributes redirectAttributes) {

        try {
            logger.debug("Editing medical result ID: {} for order ID: {}", resultId, orderId);

            // Call service method to update medical result
            MedicalResult updatedResult = medicalResultService.updateMedicalResult(
                    resultId, description, status, resultFile);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Medical result updated successfully");

            return "redirect:/doctor/medical-orders/" + orderId + "?doctorId=" + doctorId;

        } catch (Exception e) {
            logger.error("Error editing medical result ID: {} for order ID: {}", resultId, orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to update medical result: " + e.getMessage());

            return "redirect:/doctor/medical-orders/" + orderId + "?doctorId=" + doctorId;
        }
    }

    @PostMapping("/{orderId}/delete-result")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteMedicalResult(
            @PathVariable Long orderId,
            @RequestParam Integer resultId) {

        Map<String, Object> response = new HashMap<>();

        try {
            logger.debug("Deleting medical result ID: {} for order ID: {}", resultId, orderId);

            boolean deleted = medicalResultService.deleteMedicalResult(resultId);

            if (deleted) {
                response.put("success", true);
                response.put("message", "Medical result deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Medical result not found or could not be deleted");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error deleting medical result ID: {} for order ID: {}", resultId, orderId, e);

            response.put("success", false);
            response.put("message", "Failed to delete medical result: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{orderId}/result/{resultId}")
    @ResponseBody
    public ResponseEntity<?> getMedicalResult(
            @PathVariable Long orderId,
            @PathVariable Integer resultId) {

        try {
            logger.debug("Fetching medical result ID: {} for order ID: {}", resultId, orderId);

            MedicalResult result = medicalResultService.getResultById(resultId);

            if (result != null) {
                // Use the DTO to avoid circular reference issues
                orochi.dto.MedicalResultDTO resultDTO = new orochi.dto.MedicalResultDTO(result);
                return ResponseEntity.ok(resultDTO);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error fetching medical result ID: {} for order ID: {}", resultId, orderId, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
