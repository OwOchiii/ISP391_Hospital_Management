package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.model.MedicalOrder;
import orochi.model.MedicalResult;
import orochi.repository.MedicalOrderRepository;
import orochi.repository.MedicalResultRepository;
import orochi.service.FileStorageService;
import orochi.service.MedicalReportService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/medical-results")
public class MedicalResultController {

    private static final Logger logger = LoggerFactory.getLogger(MedicalResultController.class);

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private MedicalResultRepository medicalResultRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private MedicalReportService medicalReportService;

    @GetMapping("/order/{orderId}")
    public String getResultForm(@PathVariable Integer orderId, Model model) {
        try {
            logger.info("Getting result form for order ID: {}", orderId);

            Optional<MedicalOrder> orderOpt = medicalOrderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                logger.warn("Medical order not found for ID: {}", orderId);
                model.addAttribute("errorMessage", "Medical order not found");
                return "error";
            }

            MedicalOrder order = orderOpt.get();
            model.addAttribute("order", order);

            // Check if result already exists
            if (order.getResultId() != null) {
                Optional<MedicalResult> resultOpt = medicalResultRepository.findById(order.getResultId());
                resultOpt.ifPresent(result -> model.addAttribute("result", result));
            }

            return "medical/result-form";
        } catch (Exception e) {
            logger.error("Error getting result form for order ID: {}", orderId, e);
            model.addAttribute("errorMessage", "Failed to load result form: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/order/{orderId}/submit")
    public String submitResult(
            @PathVariable Integer orderId,
            @RequestParam Integer doctorId,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile resultFile,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        try {
            logger.info("Submitting result for order ID: {}", orderId);

            Optional<MedicalOrder> orderOpt = medicalOrderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                logger.warn("Medical order not found for ID: {}", orderId);
                redirectAttributes.addFlashAttribute("errorMessage", "Medical order not found");
                return "redirect:/medical-results";
            }

            MedicalOrder order = orderOpt.get();

            // Create or update result
            MedicalResult result;
            if (order.getResultId() != null) {
                Optional<MedicalResult> resultOpt = medicalResultRepository.findById(order.getResultId());
                if (resultOpt.isPresent()) {
                    result = resultOpt.get();
                    result.setDescription(description);
                    result.setStatus(status);
                    result.setResultDate(LocalDateTime.now());
                } else {
                    result = new MedicalResult();
                    result.setAppointmentId(order.getAppointmentId());
                    result.setDoctorId(doctorId);
                    result.setDescription(description);
                    result.setStatus(status);
                    result.setResultDate(LocalDateTime.now());
                }
            } else {
                result = new MedicalResult();
                result.setAppointmentId(order.getAppointmentId());
                result.setDoctorId(doctorId);
                result.setDescription(description);
                result.setStatus(status);
                result.setResultDate(LocalDateTime.now());
            }

            // Handle file upload if provided
            if (resultFile != null && !resultFile.isEmpty()) {
                String fileUrl = fileStorageService.storeFile(resultFile, "medical-results");
                result.setFileUrl(fileUrl);
            }

            // Save result
            medicalResultRepository.save(result);

            // Update order with result ID
            order.setResultId(result.getResultId());

            // Update order status if needed
            if ("COMPLETED".equals(status) && !"COMPLETED".equals(order.getStatus())) {
                order.setStatus("COMPLETED");
            }

            medicalOrderRepository.save(order);

            logger.info("Successfully submitted result for order ID: {}", orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Medical result saved successfully");

            // Redirect to the appropriate page based on context
            if (order.getAssignedToDeptId() != null) {
                return "redirect:/department/orders?departmentId=" + order.getAssignedToDeptId();
            } else {
                return "redirect:/doctor/appointments/" + order.getAppointmentId() + "?doctorId=" + doctorId;
            }

        } catch (Exception e) {
            logger.error("Error submitting result for order ID: {}", orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save result: " + e.getMessage());
            return "redirect:/medical-results/order/" + orderId;
        }
    }

    @GetMapping("/view/{resultId}")
    public String viewResult(@PathVariable Integer resultId, Model model) {
        try {
            logger.info("Viewing result ID: {}", resultId);

            Optional<MedicalResult> resultOpt = medicalResultRepository.findById(resultId);
            if (resultOpt.isEmpty()) {
                logger.warn("Medical result not found for ID: {}", resultId);
                model.addAttribute("errorMessage", "Medical result not found");
                return "error";
            }

            model.addAttribute("result", resultOpt.get());
            return "medical/view-result";
        } catch (Exception e) {
            logger.error("Error viewing result ID: {}", resultId, e);
            model.addAttribute("errorMessage", "Failed to load result: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/appointment/{appointmentId}")
    public String getResultsByAppointment(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId,
            Model model) {

        try {
            logger.info("Getting results for appointment ID: {}", appointmentId);

            List<MedicalResult> results = medicalResultRepository.findByAppointmentIdOrderByResultDateDesc(appointmentId);
            model.addAttribute("results", results);
            model.addAttribute("appointmentId", appointmentId);
            model.addAttribute("doctorId", doctorId);

            return "medical/appointment-results";
        } catch (Exception e) {
            logger.error("Error getting results for appointment ID: {}", appointmentId, e);
            model.addAttribute("errorMessage", "Failed to load results: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/generate-report/{appointmentId}")
    public String generateMedicalReport(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId,
            Model model) {

        try {
            logger.info("Generating medical report for appointment ID: {}", appointmentId);

            // Get all results for this appointment
            List<MedicalResult> results = medicalResultRepository.findByAppointmentIdOrderByResultDateDesc(appointmentId);

            // Get all orders for this appointment
            List<MedicalOrder> orders = medicalOrderRepository.findByAppointmentIdOrderByOrderDate(appointmentId);

            model.addAttribute("results", results);
            model.addAttribute("orders", orders);
            model.addAttribute("appointmentId", appointmentId);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("generatedDate", LocalDateTime.now());

            return "medical/generate-report";
        } catch (Exception e) {
            logger.error("Error generating report for appointment ID: {}", appointmentId, e);
            model.addAttribute("errorMessage", "Failed to generate report: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/generate-pdf/{appointmentId}")
    public ResponseEntity<?> generatePdfReport(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId,
            @RequestParam(required = false) String additionalNotes) {

        try {
            logger.info("Generating PDF report for appointment ID: {}", appointmentId);

            // Call the service to generate the PDF report
            Map<String, Object> result = medicalReportService.generatePdfReport(appointmentId, doctorId, additionalNotes);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error generating PDF report for appointment ID: {}", appointmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to generate PDF report: " + e.getMessage()
                    ));
        }
    }
}
