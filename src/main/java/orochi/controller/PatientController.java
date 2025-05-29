package orochi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/patient")
public class PatientController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "patient/dashboard";
    }

    @GetMapping("/search-doctor")
    public String searchDoctor() {
        return "patient/search-doctor";
    }

    @GetMapping("/book-appointment")
    public String bookAppointment() {
        return "patient/book-appointment";
    }

    @GetMapping("/appointment-list")
    public String appointmentList() {
        return "patient/appointment-list";
    }

    @GetMapping("/profile")
    public String profile() {
        return "patient/profile";
    }

    @GetMapping("/customer-support")
    public String customerSupport() {
        return "patient/customer-support";
    }

    @GetMapping("/feedback")
    public String feedback() {
        return "patient/feedback";
    }
}