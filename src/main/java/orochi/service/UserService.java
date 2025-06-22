package orochi.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import orochi.model.Users;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Integer getTotalUsers();
    Integer getGuestUsers();
    Integer getNewUsersToday();
    Integer getGrowthPercentage();

    // Methods from your current UserService class
    Users registerNewUser(Users user);
    String generatePasswordResetToken(String email);
    boolean validatePasswordResetToken(String token, String email);
    void resetPassword(String token, String email, String newPassword);

//    Page<Users> getAllReceptionists(Pageable pageable);
    Page<Users> getAllReceptionists(String search, String statusFilter, Pageable pageable);
    Optional<Users> findById(Integer userId);
    Users save(Users user);
}