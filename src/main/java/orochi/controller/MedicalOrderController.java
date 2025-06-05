package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import orochi.model.MedicalOrder;
import orochi.model.Doctor;
import orochi.model.Department;
import orochi.repository.MedicalOrderRepository;
import orochi.repository.DoctorRepository;
import orochi.repository.DepartmentRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/doctor/medical-orders")
public class MedicalOrderController {

    private static final Logger logger = LoggerFactory.getLogger(MedicalOrderController.class);

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @GetMapping("")
    public String getAllMedicalOrders(
            @RequestParam Integer doctorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        try {
            logger.info("Fetching medical orders for doctor ID: {} with filters - status: {}, type: {}, date: {}",
                    doctorId, status, type, date);

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid doctor ID: " + doctorId));

            // Get outgoing orders (orders created by this doctor)
            List<MedicalOrder> outgoingOrders;
            if (status != null && !status.isEmpty()) {
                outgoingOrders = medicalOrderRepository.findByDoctorIdAndStatus(doctorId, status);
            } else {
                outgoingOrders = medicalOrderRepository.findByDoctorId(doctorId);
            }

            // Get incoming orders (orders assigned to departments where this doctor is head)
            List<Department> headedDepartments = doctor.getDepartmentsLed();
            List<MedicalOrder> incomingOrders = null;

            if (headedDepartments != null && !headedDepartments.isEmpty()) {
                // This would need an additional repository method to get orders for multiple departments
                // For now, we'll get orders for the first department this doctor heads
                Department department = headedDepartments.get(0);
                incomingOrders = department.getMedicalOrders();

                // Filter by status if specified
                if (status != null && !status.isEmpty() && incomingOrders != null) {
                    incomingOrders.removeIf(order -> !order.getStatus().equals(status));
                }
            }

            model.addAttribute("doctorId", doctorId);
            model.addAttribute("doctorName", doctor.getUser().getFullName());
            model.addAttribute("outgoingOrders", outgoingOrders);
            model.addAttribute("incomingOrders", incomingOrders);
            model.addAttribute("selectedStatus", status);

            logger.debug("Retrieved {} outgoing and {} incoming medical orders for doctor ID: {}",
                    outgoingOrders.size(),
                    incomingOrders != null ? incomingOrders.size() : 0,
                    doctorId);

            return "doctor/medical-orders";
        } catch (Exception e) {
            logger.error("Error fetching medical orders for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve medical orders: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/{id}")
    public String getMedicalOrderDetails(
            @PathVariable Integer id,
            @RequestParam Integer doctorId,
            Model model) {
        try {
            logger.info("Fetching details for medical order ID: {} for doctor ID: {}", id, doctorId);

            MedicalOrder order = medicalOrderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid medical order ID: " + id));

            model.addAttribute("medicalOrder", order);
            model.addAttribute("doctorId", doctorId);

            logger.debug("Retrieved medical order details for order ID: {} and doctor ID: {}", id, doctorId);
            return "doctor/medical-order-details";
        } catch (Exception e) {
            logger.error("Error fetching medical order details for ID: {} and doctor ID: {}", id, doctorId, e);
            model.addAttribute("errorMessage", "Failed to retrieve medical order details: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/new")
    public String showCreateOrderForm(
            @RequestParam Integer doctorId,
            @RequestParam(required = false) Integer appointmentId,
            Model model) {
        try {
            logger.info("Displaying create order form for doctor ID: {} and appointment ID: {}", doctorId, appointmentId);

            MedicalOrder newOrder = new MedicalOrder();
            newOrder.setDoctorId(doctorId);
            newOrder.setOrderDate(new Date(System.currentTimeMillis()));
            newOrder.setStatus("Pending");

            if (appointmentId != null) {
                newOrder.setAppointmentId(appointmentId);
            }

            // Get list of departments for the dropdown
            List<Department> departments = departmentRepository.findAll();

            model.addAttribute("medicalOrder", newOrder);
            model.addAttribute("departments", departments);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("appointmentId", appointmentId);

            return "doctor/medical-order-form";
        } catch (Exception e) {
            logger.error("Error displaying create order form for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to display create order form: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/create")
    public String createMedicalOrder(
            @ModelAttribute MedicalOrder medicalOrder,
            @RequestParam Integer doctorId,
            Model model) {
        try {
            logger.info("Creating new medical order for doctor ID: {}", doctorId);

            // Set current date if not provided
            if (medicalOrder.getOrderDate() == null) {
                medicalOrder.setOrderDate(new Date(System.currentTimeMillis()));
            }

            // Set status to Pending if not provided
            if (medicalOrder.getStatus() == null || medicalOrder.getStatus().isEmpty()) {
                medicalOrder.setStatus("Pending");
            }

            // Save the medical order
            medicalOrder = medicalOrderRepository.save(medicalOrder);

            logger.debug("Successfully created medical order with ID: {} for doctor ID: {}",
                    medicalOrder.getOrderId(), doctorId);

            return "redirect:/doctor/medical-orders?doctorId=" + doctorId + "&success=created";
        } catch (Exception e) {
            logger.error("Error creating medical order for doctor ID: {}", doctorId, e);
            model.addAttribute("errorMessage", "Failed to create medical order: " + e.getMessage());
            model.addAttribute("medicalOrder", medicalOrder);
            model.addAttribute("doctorId", doctorId);
            return "doctor/medical-order-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditOrderForm(
            @PathVariable Integer id,
            @RequestParam Integer doctorId,
            Model model) {
        try {
            logger.info("Displaying edit form for medical order ID: {} and doctor ID: {}", id, doctorId);

            MedicalOrder order = medicalOrderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid medical order ID: " + id));

            // Check if this doctor has permission to edit this order
            if (!order.getDoctorId().equals(doctorId)) {
                logger.warn("Doctor {} attempted to edit order {} that belongs to another doctor", doctorId, id);
                model.addAttribute("errorMessage", "You don't have permission to edit this order");
                return "error";
            }

            // Get list of departments for the dropdown
            List<Department> departments = departmentRepository.findAll();

            model.addAttribute("medicalOrder", order);
            model.addAttribute("departments", departments);
            model.addAttribute("doctorId", doctorId);

            return "doctor/medical-order-form";
        } catch (Exception e) {
            logger.error("Error displaying edit form for medical order ID: {} and doctor ID: {}", id, doctorId, e);
            model.addAttribute("errorMessage", "Failed to display edit form: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{id}/update")
    public String updateMedicalOrder(
            @PathVariable Integer id,
            @ModelAttribute MedicalOrder medicalOrder,
            @RequestParam Integer doctorId,
            Model model) {
        try {
            logger.info("Updating medical order ID: {} for doctor ID: {}", id, doctorId);

            // Verify the order exists
            MedicalOrder existingOrder = medicalOrderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid medical order ID: " + id));

            // Check if this doctor has permission to update this order
            if (!existingOrder.getDoctorId().equals(doctorId)) {
                logger.warn("Doctor {} attempted to update order {} that belongs to another doctor", doctorId, id);
                model.addAttribute("errorMessage", "You don't have permission to update this order");
                return "error";
            }

            // Ensure ID matches the path variable
            medicalOrder.setOrderId(id);

            // Save the updated order
            medicalOrderRepository.save(medicalOrder);

            logger.debug("Successfully updated medical order ID: {} for doctor ID: {}", id, doctorId);

            return "redirect:/doctor/medical-orders?doctorId=" + doctorId + "&success=updated";
        } catch (Exception e) {
            logger.error("Error updating medical order ID: {} for doctor ID: {}", id, doctorId, e);
            model.addAttribute("errorMessage", "Failed to update medical order: " + e.getMessage());
            model.addAttribute("medicalOrder", medicalOrder);
            model.addAttribute("doctorId", doctorId);
            return "doctor/medical-order-form";
        }
    }

    @PostMapping("/{id}/update-status")
    public String updateOrderStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestParam Integer doctorId,
            Model model) {
        try {
            logger.info("Updating status to '{}' for medical order ID: {} by doctor ID: {}", status, id, doctorId);

            MedicalOrder order = medicalOrderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid medical order ID: " + id));

            // Check if status is valid
            if (!List.of("Pending", "InProgress", "Completed", "Cancelled").contains(status)) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }

            order.setStatus(status);
            medicalOrderRepository.save(order);

            logger.debug("Successfully updated status to '{}' for medical order ID: {}", status, id);

            return "redirect:/doctor/medical-orders?doctorId=" + doctorId + "&success=statusUpdated";
        } catch (Exception e) {
            logger.error("Error updating status for medical order ID: {} and doctor ID: {}", id, doctorId, e);
            model.addAttribute("errorMessage", "Failed to update order status: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/search")
    public String searchMedicalOrders(
            @RequestParam Integer doctorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        try {
            logger.info("Searching medical orders with keyword '{}' for doctor ID: {}", keyword, doctorId);

            // This would need a custom repository method for searching
            // For now, we'll just return all orders for this doctor
            List<MedicalOrder> orders = medicalOrderRepository.findByDoctorId(doctorId);

            model.addAttribute("medicalOrders", orders);
            model.addAttribute("doctorId", doctorId);
            model.addAttribute("keyword", keyword);
            model.addAttribute("title", "Search Results: " + keyword);

            logger.debug("Retrieved {} medical orders for search term '{}' and doctor ID: {}",
                    orders.size(), keyword, doctorId);

            return "doctor/medical-orders";
        } catch (Exception e) {
            logger.error("Error searching medical orders with keyword '{}' for doctor ID: {}", keyword, doctorId, e);
            model.addAttribute("errorMessage", "Failed to search medical orders: " + e.getMessage());
            return "error";
        }
    }
}