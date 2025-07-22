// src/main/java/orochi/controller/AdminDoctorController.java
package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;

import org.springframework.security.crypto.password.PasswordEncoder;
import orochi.model.Doctor;
import orochi.model.DoctorForm;
import orochi.service.DoctorService;
import orochi.service.FileStorageService;
import orochi.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import orochi.model.Users;
import orochi.service.UserService;
import orochi.util.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin/doctors")
@RequiredArgsConstructor
public class AdminDoctorController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDoctorController.class);

    private final DoctorService doctorService;
    private final FileStorageService fileStorageService;

    @Autowired
    @Qualifier("userServiceImpl")
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.default.doctor.password}")
    private String defaultPassword;

    @GetMapping
    public String list(
            @RequestParam int adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model
    ) {
        // Lấy trang dữ liệu từ service
        Page<Doctor> pageData = doctorService.searchDoctors(search, statusFilter, page, size);

        List<Doctor> doctorsWithEdu = pageData.getContent().stream()
                .map(d -> doctorService.getDoctorByIdWithEducation(d.getDoctorId()))
                .toList();
        // Cập nhật các thuộc tính trong model
        model.addAttribute("doctors", doctorsWithEdu);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("adminId", adminId);
        model.addAttribute("doctorForm", new DoctorForm());
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);

        return "admin/doctor/list";
    }


    @GetMapping("/{id}/details")
    public String doctorDetails(@PathVariable int id, Model model) {
        Doctor doctor = doctorService.getDoctorByIdWithEducation(id);
        model.addAttribute("doctor", doctor);
        return "admin/doctor/detail";  // Trang chi tiết bác sĩ
    }



    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id,
                           @RequestParam int adminId,
                           Model model) {
        model.addAttribute("doctorForm", doctorService.loadForm(id));
        model.addAttribute("adminId", adminId);
        return "admin/doctor/edit";
    }

    @GetMapping("/add")
    public String addForm(@RequestParam int adminId, Model model) {
        model.addAttribute("doctorForm", new DoctorForm());
        model.addAttribute("adminId", adminId);
        return "admin/doctor/add";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable int id,
                         @RequestParam int adminId,
                         @ModelAttribute Doctor form) {
        doctorService.saveDoctor(form);
        return "redirect:/admin/doctors?adminId=" + adminId;
    }


    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("doctorForm") DoctorForm doctorForm,
            BindingResult bindingResult,
            @RequestParam int adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "croppedImageData", required = false) String croppedImageData,
            @RequestParam(value = "degrees[]", required = false) List<String> degrees,
            @RequestParam(value = "institutions[]", required = false) List<String> institutions,
            @RequestParam(value = "graduations[]", required = false) List<Integer> graduations,
            @RequestParam(value = "educationDescriptions[]", required = false) List<String> educationDescriptions,
            @RequestParam(value = "certificateImages[]", required = false) MultipartFile[] certificateImages,
            Model model,
            RedirectAttributes flash
    ) {
        if (bindingResult.hasErrors()) {
            // load lại dữ liệu danh sách khi có lỗi
            Page<Doctor> pageData = doctorService.searchDoctors(search, statusFilter, page, size);
            model.addAttribute("doctors",      pageData.getContent());
            model.addAttribute("currentPage",  page);
            model.addAttribute("totalPages",   pageData.getTotalPages());
            model.addAttribute("pageSize",     size);
            model.addAttribute("search",       search);
            model.addAttribute("statusFilter", statusFilter);
            model.addAttribute("roles",        roleService.getAllRoles());
            model.addAttribute("adminId",      adminId);
            return "admin/doctor/list";
        }

        boolean isNew = (doctorForm.getDoctorId() == null);

        // Process profile image if provided
        try {
            // If we have cropped image data, use that instead of the original file
            if (croppedImageData != null && !croppedImageData.isEmpty()) {
                logger.info("Processing cropped image data for doctor");
                // Generate filename based on doctor info
                String filename = "doctor_profile_" + (doctorForm.getDoctorId() != null ?
                                  doctorForm.getDoctorId() : "new");

                // Convert Base64 image to MultipartFile
                MultipartFile croppedImageFile = ImageUtils.base64ToMultipartFile(
                        croppedImageData, filename);

                // Store the file and get the URL
                String imageUrl = fileStorageService.storeFile(croppedImageFile, "doctor-profiles");

                // Set the image URL in the doctor form
                doctorForm.setImageUrl(imageUrl);
                logger.info("Saved profile image for doctor: {}", imageUrl);
            } else if (profileImage != null && !profileImage.isEmpty()) {
                // If we only have the original file (no cropping), just store it
                logger.info("Processing original profile image for doctor");
                String imageUrl = fileStorageService.storeFile(profileImage, "doctor-profiles");
                doctorForm.setImageUrl(imageUrl);
                logger.info("Saved profile image for doctor: {}", imageUrl);
            }

            // Process education data and certificate images
            if (degrees != null && !degrees.isEmpty()) {
                // Transfer the education data to the doctorForm
                doctorForm.setDegrees(degrees);
                doctorForm.setInstitutions(institutions);
                doctorForm.setGraduations(graduations);
                doctorForm.setEducationDescriptions(educationDescriptions);

                // Process certificate images
                List<String> certificateImageUrls = new ArrayList<>();

                if (certificateImages != null) {
                    for (int i = 0; i < certificateImages.length; i++) {
                        MultipartFile certImage = certificateImages[i];
                        if (certImage != null && !certImage.isEmpty()) {
                            // Store certificate image
                            String certImageUrl = fileStorageService.storeFile(certImage, "doctor-certificates");
                            certificateImageUrls.add(certImageUrl);
                            logger.info("Saved certificate image: {}", certImageUrl);
                        } else {
                            // No image for this education entry
                            certificateImageUrls.add(null);
                        }
                    }
                }

                doctorForm.setCertificateImageUrls(certificateImageUrls);
            }
        } catch (IOException e) {
            logger.error("Error processing doctor images", e);
            flash.addFlashAttribute("errorMessage", "Error uploading images: " + e.getMessage());
        }

        // Gọi service để lưu (service sẽ gán luôn passwordHash nếu isNew)
        doctorService.saveFromForm(doctorForm);

        if (isNew) {
            // đẩy plaintext password mặc định ra flash để hiển thị
            flash.addFlashAttribute("newDoctorPassword", defaultPassword);
        }
        flash.addFlashAttribute("successMessage", "Lưu bác sĩ thành công!");
        return "redirect:/admin/doctors?adminId=" + adminId
                + (search!=null? "&search="+search:"")
                + (statusFilter!=null? "&statusFilter="+statusFilter:"")
                + "&page="+page+"&size="+size;
    }


    @PostMapping("/{id}/toggleLock")
    public String toggleLock(@PathVariable int id,
                             @RequestParam int adminId) {
        doctorService.toggleDoctorLock(id);
        return "redirect:/admin/doctors?adminId=" + adminId;
    }
}