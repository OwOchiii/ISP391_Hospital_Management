package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import orochi.config.CustomUserDetails;
import orochi.model.DoctorSupportTicket;
import orochi.repository.DoctorRepository;
import orochi.service.DoctorSupportTicketService;
import orochi.service.FileStorageService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/doctor/support-request")
public class DoctorSupportRequestController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorSupportRequestController.class);

    @Autowired
    private DoctorSupportTicketService supportTicketService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("")
    public String showSupportRequestForm(Model model, Authentication authentication) {
        Integer doctorId = getDoctorIdFromAuthentication(authentication);
        if (doctorId == null) {
            return "redirect:/login";
        }

        List<DoctorSupportTicket> requests = supportTicketService.getTicketsByDoctorId(doctorId);
        model.addAttribute("requests", requests);
        return "doctor/support-request";
    }

    @PostMapping("/submit")
    public String submitSupportRequest(@ModelAttribute DoctorSupportTicket supportTicket,
                                      @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        Integer doctorId = getDoctorIdFromAuthentication(authentication);
        if (doctorId == null) {
            return "redirect:/login";
        }

        String requestTitle = supportTicket.getRequestTitle();

        supportTicket.setDoctorId(doctorId);
        supportTicket.setSubmissionDate(LocalDateTime.now());
        supportTicket.setTicketStatus("PENDING");
        supportTicket.setRequestTitle(requestTitle);


        // Handle file upload if provided
        if (attachmentFile != null && !attachmentFile.isEmpty()) {
            try {
                String fileUrl = fileStorageService.storeFile(attachmentFile, "support-tickets");
                supportTicket.setAttachmentPath(fileUrl);
                logger.info("File uploaded successfully: {}", fileUrl);
            } catch (IOException e) {
                logger.error("Failed to upload attachment file", e);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Support request was saved but file upload failed: " + e.getMessage());
            }
        }

        DoctorSupportTicket savedTicket = supportTicketService.saveTicket(supportTicket);

        redirectAttributes.addFlashAttribute("successMessage",
                "Your support request has been submitted successfully with ID: " + savedTicket.getId());

        return "redirect:/doctor/support-request";
    }

    @GetMapping("/view/{id}")
    public String viewSupportRequest(@PathVariable Long id,
                                     Model model,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        Integer doctorId = getDoctorIdFromAuthentication(authentication);
        if (doctorId == null) {
            return "redirect:/login";
        }

        Optional<DoctorSupportTicket> ticketOpt = supportTicketService.getTicketById(id);

        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Support request not found.");
            return "redirect:/doctor/support-request";
        }

        DoctorSupportTicket ticket = ticketOpt.get();

        // Security check: ensure the doctor can only view their own tickets
        if (!supportTicketService.isAuthorizedToView(ticket, doctorId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to view this support request.");
            return "redirect:/doctor/support-request";
        }

        model.addAttribute("ticket", ticket);

        // Add a note about contact permissions
        boolean contactPermissionGranted = ticket.getFollowUpNeeded() != null && ticket.getFollowUpNeeded();
        model.addAttribute("contactPermissionGranted", contactPermissionGranted);
        model.addAttribute("contactNote", "Note: You can only view the status of your ticket. "
                + "You will not receive any contact from support staff unless you have enabled the 'Request follow-up' option.");

        return "doctor/view-support-request";
    }

    @GetMapping("/download-attachment/{id}")
    public String downloadAttachment(@PathVariable Long id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        Integer doctorId = getDoctorIdFromAuthentication(authentication);
        if (doctorId == null) {
            return "redirect:/login";
        }

        Optional<DoctorSupportTicket> ticketOpt = supportTicketService.getTicketById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Support request not found.");
            return "redirect:/doctor/support-request";
        }

        DoctorSupportTicket ticket = ticketOpt.get();

        // Security check: ensure the doctor can only access their own tickets
        if (!supportTicketService.isAuthorizedToView(ticket, doctorId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to download this attachment.");
            return "redirect:/doctor/support-request";
        }

        if (ticket.getAttachmentPath() == null || ticket.getAttachmentPath().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No attachment found for this support request.");
            return "redirect:/doctor/support-request/view/" + id;
        }

        // Extract filename from the path
        String fileName = ticket.getAttachmentPath();
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }

        // Redirect to the file download endpoint
        return "redirect:/download/support-tickets/" + fileName;
    }

    private Integer getDoctorIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getDoctorId();
        }
        return null;
    }
}
