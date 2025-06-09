package orochi.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.extern.slf4j.Slf4j;
import orochi.model.*;
import orochi.repository.*;
import orochi.config.CustomUserDetails;
import orochi.service.FeedbackService;
import orochi.service.PatientService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/patient")
public class PatientDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(PatientDashboardController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientContactRepository patientContactRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            Integer patientId = getCurrentPatientId();

            if (patientId == null) {
                logger.error("No patientId found for authenticated user");
                model.addAttribute("errorMessage", "Patient ID is required. Please contact support.");
                return "error";
            }

            Optional<Patient> patientOpt = patientService.getPatientById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }

            Patient patient = patientOpt.get();
            model.addAttribute("patientName", patient.getUser().getFullName());

            // Get upcoming appointments
            List<Appointment> upcomingAppointmentsList = patientService.getUpcomingAppointments(patientId);
            int upcomingAppointments = upcomingAppointmentsList != null ? upcomingAppointmentsList.size() : 0;
            model.addAttribute("upcomingAppointments", upcomingAppointments);

            // Get total appointments
            List<Appointment> allAppointments = patientService.getAllAppointments(patientId);
            int totalAppointments = allAppointments != null ? allAppointments.size() : 0;
            model.addAttribute("totalAppointments", totalAppointments);

            // Get active prescriptions
            List<Prescription> activePrescriptions = patientService.getActivePrescriptions(patientId);
            int activePrescriptionsCount = activePrescriptions != null ? activePrescriptions.size() : 0;
            model.addAttribute("activePrescriptions", activePrescriptionsCount);

            // Get last visit date
            String lastVisit = "N/A";
            if (allAppointments != null && !allAppointments.isEmpty()) {
                for (Appointment appointment : allAppointments) {
                    if (appointment.getDateTime().isBefore(LocalDateTime.now())) {
                        lastVisit = formatLastVisitDate(appointment.getDateTime());
                        break;
                    }
                }
            }
            model.addAttribute("lastVisit", lastVisit);

            logger.info("Dashboard loaded successfully for patient ID: {}", patientId);
            return "patient/dashboard";
        } catch (Exception e) {
            logger.error("Error loading dashboard", e);
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/search-doctor")
    public String searchDoctor(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId != null) {
                model.addAttribute("patientId", patientId);
            }
            return "patient/search-doctor";
        } catch (Exception e) {
            logger.error("Error loading search doctor page", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }


    @GetMapping("/appointment-list")
    public String appointmentList(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            // Get patient info
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                model.addAttribute("patientName", patient.getUser().getFullName());
            }

            // Get all appointments for the patient
            List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByDateTimeDesc(patientId);

            // Count appointments by status
            long scheduledCount = appointments.stream()
                    .filter(a -> "Scheduled".equals(a.getStatus()))
                    .count();

            long pendingCount = appointments.stream()
                    .filter(a -> "Pending".equals(a.getStatus()))
                    .count();

            long completedCount = appointments.stream()
                    .filter(a -> "Completed".equals(a.getStatus()))
                    .count();

            long cancelledCount = appointments.stream()
                    .filter(a -> "Cancel".equals(a.getStatus()))
                    .count();

            // Add attributes to model
            model.addAttribute("appointments", appointments);
            model.addAttribute("patientId", patientId);
            model.addAttribute("totalAppointments", appointments.size());
            model.addAttribute("scheduledAppointments", scheduledCount);
            model.addAttribute("pendingAppointments", pendingCount);
            model.addAttribute("completedAppointments", completedCount);
            model.addAttribute("cancelledAppointments", cancelledCount);

            return "patient/appointment-list";
        } catch (Exception e) {
            logger.error("Error loading appointment list", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/appointment-list/{id}/cancel")
    public String showCancelAppointmentPage(@PathVariable("id") Integer appointmentId,
                                            @RequestParam("patientId") Integer patientId,
                                            Model model) {
        try {
            Integer currentPatientId = getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                logger.error("Unauthorized access attempt for patient ID: {}", patientId);
                model.addAttribute("errorMessage", "Unauthorized access or invalid patient ID");
                return "error";
            }

            // Get patient info
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }
            Patient patient = patientOpt.get();
            model.addAttribute("patientName", patient.getUser().getFullName());
            model.addAttribute("patientId", patientId);

            // Get appointment info
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (!appointmentOpt.isPresent()) {
                logger.error("Appointment not found for ID: {}", appointmentId);
                model.addAttribute("errorMessage", "Appointment not found");
                return "error";
            }
            Appointment appointment = appointmentOpt.get();

            // Verify appointment belongs to the patient
            if (!appointment.getPatient().getPatientId().equals(patientId)) {
                logger.error("Appointment ID: {} does not belong to patient ID: {}", appointmentId, patientId);
                model.addAttribute("errorMessage", "You are not authorized to cancel this appointment");
                return "error";
            }

            // Check if appointment is cancellable
            if (!"Scheduled".equals(appointment.getStatus())) {
                logger.warn("Attempt to cancel non-scheduled appointment ID: {}", appointmentId);
                model.addAttribute("errorMessage", "Only scheduled appointments can be cancelled");
                return "error";
            }

            model.addAttribute("appointment", appointment);
            model.addAttribute("previousDescription", appointment.getDescription() != null ? appointment.getDescription() : "No description provided");

            logger.info("Cancel appointment page loaded for appointment ID: {}, patient ID: {}", appointmentId, patientId);
            return "patient/cancel-appointment";
        } catch (Exception e) {
            logger.error("Error loading cancel appointment page for appointment ID: {}", appointmentId, e);
            model.addAttribute("errorMessage", "An error occurred while loading the cancellation page: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/appointment-list/{id}/cancel")
    public String confirmCancelAppointment(@PathVariable("id") Integer appointmentId,
                                           @RequestParam("patientId") Integer patientId,
                                           @RequestParam("description") String cancelReason,
                                           RedirectAttributes redirectAttributes) {
        try {
            Integer currentPatientId = getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                logger.error("Unauthorized access attempt for patient ID: {}", patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access or invalid patient ID");
                return "redirect:/patient/appointment-list";
            }

            // Get appointment info
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (!appointmentOpt.isPresent()) {
                logger.error("Appointment not found for ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found");
                return "redirect:/patient/appointment-list";
            }
            Appointment appointment = appointmentOpt.get();

            // Verify appointment belongs to the patient
            if (!appointment.getPatient().getPatientId().equals(patientId)) {
                logger.error("Appointment ID: {} does not belong to patient ID: {}", appointmentId, patientId);
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to cancel this appointment");
                return "redirect:/patient/appointment-list";
            }

            // Check if appointment is cancellable
            if (!"Scheduled".equals(appointment.getStatus())) {
                logger.warn("Attempt to cancel non-scheduled appointment ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Only scheduled appointments can be cancelled");
                return "redirect:/patient/appointment-list";
            }

            // Trim and normalize cancellation reason
            String trimmedReason = cancelReason.trim().replaceAll("\\s+", " ");
            if (trimmedReason.isEmpty()) {
                logger.warn("No cancellation reason provided for appointment ID: {}", appointmentId);
                redirectAttributes.addFlashAttribute("errorMessage", "Please provide a reason for cancellation");
                return "redirect:/patient/appointment-list/" + appointmentId + "/cancel?patientId=" + patientId;
            }
            if (trimmedReason.length() > 250) {
                trimmedReason = trimmedReason.substring(0, 250);
            }

            // Update appointment status and description
            appointment.setStatus("Cancel");
            appointment.setDescription(trimmedReason);
            appointmentRepository.save(appointment);

            logger.info("Appointment ID {} cancelled successfully by patient ID: {}", appointmentId, patientId);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment cancelled successfully");
            return "redirect:/patient/appointment-list";
        } catch (Exception e) {
            logger.error("Error cancelling appointment ID: {}", appointmentId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel appointment: " + e.getMessage());
            return "redirect:/patient/appointment-list";
        }
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                logger.error("Patient not found for ID: {}", patientId);
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }
            Patient patient = patientOpt.get();
            model.addAttribute("patient", patient);
            model.addAttribute("patientName", patient.getUser().getFullName());
            model.addAttribute("patientId", patientId);

            return "patient/profile";
        } catch (Exception e) {
            logger.error("Error loading profile", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/update-profile")
    public String showUpdateProfileForm(Model model) {
        Integer patientId = getCurrentPatientId();
        if (patientId == null) {
            model.addAttribute("errorMessage", "Patient ID is required");
            return "error";
        }

        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (!patientOpt.isPresent()) {
            model.addAttribute("errorMessage", "Patient not found");
            return "error";
        }

        Patient patient = patientOpt.get();
        Users user = patient.getUser();
        String streetAddress = "";
        if (patient.getContacts() != null && !patient.getContacts().isEmpty()) {
            PatientContact contact = patient.getContacts().get(0);
            streetAddress = contact.getStreetAddress();
        }

        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhoneNumber(user.getPhoneNumber());
        form.setDateOfBirth(patient.getDateOfBirth());
        form.setGender(patient.getGender());
        form.setDescription(patient.getDescription());
        form.setStreetAddress(streetAddress);

        model.addAttribute("profileForm", form);
        model.addAttribute("patientId", patientId);
        model.addAttribute("patientName", user.getFullName());

        return "patient/update-profile";
    }

    @PostMapping("/update-profile")
    @Transactional
    public String updateProfile(@ModelAttribute("profileForm") @Valid ProfileUpdateForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes, Model model) {
        System.out.println("Processing update-profile with form: " + form);

        Integer patientId = getCurrentPatientId();
        if (patientId == null) {
            System.out.println("Patient ID is null");
            redirectAttributes.addFlashAttribute("errorMessage", "Patient ID is required");
            return "redirect:/patient/profile";
        }

        if (bindingResult.hasErrors()) {
            System.out.println("Validation errors found: " + bindingResult.getAllErrors());
            return "patient/update-profile";
        }

        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (!patientOpt.isPresent()) {
            System.out.println("Patient not found for ID: " + patientId);
            redirectAttributes.addFlashAttribute("errorMessage", "Patient not found");
            return "redirect:/patient/profile";
        }

        Patient patient = patientOpt.get();
        Users user = patient.getUser();
        System.out.println("Found patient ID: " + patient.getPatientId() + ", user ID: " + user.getUserId());

        String newEmail = form.getEmail();
        String newPhoneNumber = form.getPhoneNumber();

        if (!newEmail.equals(user.getEmail())) {
            Optional<Users> emailCheck = userRepository.findByEmail(newEmail);
            if (emailCheck.isPresent() && !emailCheck.get().getUserId().equals(user.getUserId())) {
                System.out.println("Email already in use: " + newEmail);
                bindingResult.addError(new FieldError("profileForm", "email", "This email is already in use."));
            }
        }

        if (!newPhoneNumber.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumberAndUserIdNot(newPhoneNumber, user.getUserId())) {
            System.out.println("Phone number already in use: " + newPhoneNumber);
            bindingResult.addError(new FieldError("profileForm", "phoneNumber", "This phone number is already in use."));
        }

        if (bindingResult.hasErrors()) {
            System.out.println("Post-validation errors found: " + bindingResult.getAllErrors());
            return "patient/update-profile";
        }

        try {
            // Update Users entity
            user.setFullName(form.getFullName());
            user.setEmail(newEmail);
            user.setPhoneNumber(newPhoneNumber);
            System.out.println("Saving user ID: " + user.getUserId() + ", email: " + user.getEmail());
            userRepository.save(user);

            // Update Patient entity
            patient.setDateOfBirth(form.getDateOfBirth());
            patient.setGender(form.getGender());
            patient.setDescription(form.getDescription());
            System.out.println("Saving patient ID: " + patient.getPatientId());
            patientRepository.save(patient);

            // Update or create PatientContact
            Optional<PatientContact> contactOpt = patientContactRepository.findFirstByPatientId(patientId);
            if (contactOpt.isPresent()) {
                PatientContact contact = contactOpt.get();
                contact.setStreetAddress(form.getStreetAddress());
                contact.setCity("OK");
                contact.setCountry("OK");
                System.out.println("Saving existing contact ID: " + contact.getContactId());
                patientContactRepository.save(contact);
            } else {
                PatientContact newContact = new PatientContact();
                newContact.setPatientId(patientId);
                newContact.setAddressType("Primary");
                newContact.setStreetAddress(form.getStreetAddress());
                newContact.setCity("OK");
                newContact.setCountry("OK");
                System.out.println("Saving new contact for patient ID: " + patientId);
                patientContactRepository.save(newContact);
            }

            // Move success message after all saves
            System.out.println("Profile updated successfully for patient ID: " + patientId);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
            return "redirect:/patient/profile";
        } catch (Exception e) {
            System.out.println("Error updating profile for patient ID: " + patientId + ", error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
            return "patient/update-profile";
        }
    }

    @GetMapping("/customer-support")
    public String customerSupport(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId != null) {
                model.addAttribute("patientId", patientId);
            }
            return "patient/customer-support";
        } catch (Exception e) {
            logger.error("Error loading customer support page", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/feedback")
    public String feedback(Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            // Get patient info
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            Patient patient = patientOpt.orElseThrow(() -> new Exception("Patient not found"));
            model.addAttribute("userId", patient.getUser().getUserId());
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");

            return "patient/feedback";
        } catch (Exception e) {
            logger.error("Error loading feedback page", e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/feedback")
    public String submitFeedback(@RequestParam("description") String description, Model model) {
        try {
            Integer patientId = getCurrentPatientId();
            if (patientId == null) {
                model.addAttribute("errorMessage", "Patient ID is required");
                return "error";
            }

            // Get userId from patient
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (!patientOpt.isPresent()) {
                model.addAttribute("errorMessage", "Patient not found");
                return "error";
            }
            Integer userId = patientOpt.get().getUser().getUserId();

            // Trim and normalize spaces, limit to 250 characters
            String trimmedDescription = description.trim().replaceAll("\\s+", " ");
            if (trimmedDescription.length() > 250) {
                trimmedDescription = trimmedDescription.substring(0, 250);
            }

            Feedback feedback = new Feedback();
            feedback.setUserId(userId);
            feedback.setDescription(trimmedDescription);

            feedbackService.saveFeedback(feedback);
            model.addAttribute("successMessage", "Feedback submitted successfully");
            model.addAttribute("userId", userId);
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", patientOpt.get().getUser().getFullName());
            return "redirect:/patient/feedback";
        } catch (Exception e) {
            logger.error("Error submitting feedback", e);
            model.addAttribute("errorMessage", "Failed to submit feedback: " + e.getMessage());
            model.addAttribute("description", description); // Preserve user input
            return "error";
        }
    }

    @GetMapping("/my-feedback")
    public String myFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {
        try{
            Integer patientId = getCurrentPatientId();
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            Patient patient = patientOpt.orElseThrow(() -> new Exception("Patient not found"));
            Integer userId = patient.getUser().getUserId();

            // Get patient info
            model.addAttribute("userId", userId);
            model.addAttribute("patientId", patientId);
            model.addAttribute("patientName", patient.getUser() != null ? patient.getUser().getFullName() : "Patient");

            Pageable pageable = PageRequest.of(page, size);
            Page<Feedback> feedbackPage;

            // Trim date strings to handle whitespace
            String fromDateTrimmed = (fromDate != null) ? fromDate.trim() : null;
            String toDateTrimmed = (toDate != null) ? toDate.trim() : null;

            if (fromDateTrimmed != null && !fromDateTrimmed.isEmpty() && toDateTrimmed != null && !toDateTrimmed.isEmpty()) {
                LocalDateTime start = LocalDateTime.parse(fromDateTrimmed + "T00:00:00");
                LocalDateTime end = LocalDateTime.parse(toDateTrimmed + "T23:59:59");
                feedbackPage = feedbackService.getFeedbackByUserIdAndDateRange(userId, start, end, pageable);
            } else if (fromDateTrimmed != null && !fromDateTrimmed.isEmpty()) {
                LocalDateTime start = LocalDateTime.parse(fromDateTrimmed + "T00:00:00");
                feedbackPage = feedbackService.getFeedbackByUserIdAndDateRange(userId, start, LocalDateTime.now(), pageable);
            } else if (toDateTrimmed != null && !toDateTrimmed.isEmpty()) {
                LocalDateTime end = LocalDateTime.parse(toDateTrimmed + "T23:59:59");
                feedbackPage = feedbackService.getFeedbackByUserIdAndDateRange(userId, LocalDateTime.of(1900, 1, 1, 0, 0), end, pageable);
            } else {
                feedbackPage = feedbackService.getFeedbackByUserId(userId, pageable);
            }

            model.addAttribute("feedbacks", feedbackPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", feedbackPage.getTotalPages());
            model.addAttribute("size", size);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            return "patient/my-feedback";
        }
        catch(Exception e){
            logger.error("Error fetching feedback", e);
            model.addAttribute("errorMessage", "Failed to fetch feedback: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/delete-feedback")
    public String deleteFeedback(@RequestParam("feedbackId") Integer feedbackId) {
        feedbackService.deleteFeedback(feedbackId);
        return "redirect:/patient/my-feedback";
    }

    private Integer getCurrentPatientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getPatientId();
        }
        return null;
    }

    private String formatLastVisitDate(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate());

        if (daysDifference == 0) {
            return "Today";
        } else if (daysDifference == 1) {
            return "Yesterday";
        } else if (daysDifference < 7) {
            return daysDifference + " days ago";
        } else if (daysDifference < 30) {
            long weeks = daysDifference / 7;
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            return dateTime.format(formatter);
        }
    }
}