package orochi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import orochi.config.CustomUserDetails;
import orochi.dto.UserDTO;
import orochi.model.Users;
import orochi.repository.UserRepository;

@RestController
@RequestMapping("/api")
public class ApiPatientController {

    private static final Logger logger = LoggerFactory.getLogger(ApiPatientController.class);

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/patient/details")
    public ResponseEntity<UserDTO> getPatientDetails(Authentication authentication) {
        logger.info("Received request for /api/patient/details");
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthenticated request to /api/patient/details");
            return ResponseEntity.status(401).build();
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Integer userId = userDetails.getUserId();
        logger.info("Fetching user details for UserID: {}", userId);
        Users user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.warn("No user found for UserID: {}", userId);
            return ResponseEntity.status(404).build();
        }
        logger.info("Returning user details: {}", user.getEmail());
        UserDTO userDTO = new UserDTO(user.getFullName(), user.getEmail(), user.getPhoneNumber());
        return ResponseEntity.ok(userDTO);
    }
}