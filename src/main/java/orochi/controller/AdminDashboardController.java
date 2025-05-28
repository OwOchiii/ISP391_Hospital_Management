package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import orochi.service.AppointmentMetricService;
import orochi.service.DoctorService;
import orochi.service.MedicalOrderService;
import orochi.service.PrescriptionService;
import orochi.service.RevenueService;
import orochi.service.RoomService;
import orochi.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserService userService;
    private final AppointmentMetricService appointmentService;
    private final RevenueService revenueService;
    private final DoctorService doctorService;
    private final RoomService roomService;
    private final PrescriptionService prescriptionService;
    private final MedicalOrderService medicalOrderService;

    @Autowired
    public AdminDashboardController(
            UserService userService,
            AppointmentMetricService appointmentService,
            RevenueService revenueService,
            DoctorService doctorService,
            RoomService roomService,
            PrescriptionService prescriptionService,
            MedicalOrderService medicalOrderService) {
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.revenueService = revenueService;
        this.doctorService = doctorService;
        this.roomService = roomService;
        this.prescriptionService = prescriptionService;
        this.medicalOrderService = medicalOrderService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, @RequestParam(required = false) Integer adminId) {
        // Placeholder for admin ID and name - in a real implementation,
        // you would get these from the authenticated user session
        if (adminId == null) {
            adminId = 1; // Default admin ID if not provided
        }
        String adminName = "Admin User";

        // Basic info
        model.addAttribute("adminId", adminId);
        model.addAttribute("adminName", adminName);

        // Statistics
        model.addAttribute("totalUsers", userService.getTotalUsers());
        model.addAttribute("totalAppointments", appointmentService.getTotalAppointments());
        model.addAttribute("totalRevenue", revenueService.getMonthlyRevenue());
        model.addAttribute("totalDoctors", doctorService.getActiveDoctors());
        model.addAttribute("totalRooms", roomService.getTotalRooms());
        model.addAttribute("totalPrescriptions", prescriptionService.getMonthlyCount());

        // Growth metrics
        model.addAttribute("userGrowth", userService.getGrowthPercentage());
        model.addAttribute("appointmentGrowth", appointmentService.getGrowthPercentage());
        model.addAttribute("revenueGrowth", revenueService.getGrowthPercentage());
        model.addAttribute("doctorGrowth", 5); // PLACEHOLDER: 5% growth for doctors

        // Management summaries
        model.addAttribute("activeUsers", userService.getGuestUsers());
        model.addAttribute("newUsersToday", userService.getNewUsersToday());
        model.addAttribute("todayAppointments", appointmentService.getTodayAppointments());
        model.addAttribute("pendingAppointments", appointmentService.getPendingAppointments());
        model.addAttribute("onlineDoctors", doctorService.getOnlineDoctors());
        model.addAttribute("busyDoctors", 15); // PLACEHOLDER: 15 busy doctors
        model.addAttribute("availableRooms", roomService.getAvailableRooms());
        model.addAttribute("occupiedRooms", 6); // PLACEHOLDER: 6 occupied rooms
        model.addAttribute("pendingOrders", medicalOrderService.getPendingOrders());
        model.addAttribute("completedOrders", 156); // PLACEHOLDER: 156 completed orders

        // Additional revenue statistics
        model.addAttribute("monthlyRevenue", revenueService.getMonthlyRevenue());
        model.addAttribute("yearlyRevenue", 485670.0); // PLACEHOLDER: $485,670 yearly revenue

        return "admin/dashboard";
    }
}
