package orochi.service;

import orochi.model.Users;
import orochi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Users registerNewUser(Users user) {
        Optional<Users> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            // Consider creating a custom exception for better error handling
            throw new RuntimeException("User with email " + user.getEmail() + " already exists.");
        }

        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        // Ensure RoleID is set if it's a required field and not handled elsewhere
        // For example, set a default role or get it from the registration form.
        // user.setRoleId(1); // Example: setting a default RoleID
        return userRepository.save(user);
    }

    // You can add other user-related methods here, like findByEmail, etc.
}

