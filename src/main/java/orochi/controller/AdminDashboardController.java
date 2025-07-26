package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import java.time.LocalDate;

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
            @Qualifier("userServiceImpl") UserService userService,
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

        // Calculate date ranges for revenue calculations
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfYear = today.withDayOfYear(1);

        // Basic info
        model.addAttribute("adminId", adminId);
        model.addAttribute("adminName", adminName);

        // Statistics - using calculated values instead of hardcoded ones
        model.addAttribute("totalUsers", userService.getTotalUsers());
        model.addAttribute("totalAppointments", appointmentService.getTotalAppointments());
        model.addAttribute("totalDoctors", doctorService.getActiveDoctors());
        model.addAttribute("totalRooms", roomService.getTotalRooms());
        model.addAttribute("totalPrescriptions", prescriptionService.getMonthlyCount());

        // Revenue calculations - using actual calculated values
        Double monthlyRevenue = revenueService.getTotalRevenue(startOfMonth, today);
        Double yearlyRevenue = revenueService.getTotalRevenue(startOfYear, today);
        model.addAttribute("totalRevenue", monthlyRevenue != null ? monthlyRevenue : 0.0);
        model.addAttribute("monthlyRevenue", monthlyRevenue != null ? monthlyRevenue : 0.0);
        model.addAttribute("yearlyRevenue", yearlyRevenue != null ? yearlyRevenue : 0.0);

        // Management summaries - removing growth percentages as requested
        model.addAttribute("activeUsers", userService.getGuestUsers());
        model.addAttribute("newUsersToday", userService.getNewUsersToday());
        model.addAttribute("todayAppointments", appointmentService.getTodayAppointments());
        model.addAttribute("pendingAppointments", appointmentService.getPendingAppointments());
        model.addAttribute("onlineDoctors", doctorService.getOnlineDoctors());
        model.addAttribute("availableRooms", roomService.getAvailableRooms());
        model.addAttribute("pendingOrders", medicalOrderService.getPendingOrders());

        return "admin/dashboard";
    }
}
