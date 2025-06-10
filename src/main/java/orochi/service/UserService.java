package orochi.service;
import java.util.List;
import java.util.Optional;
import orochi.model.Users;

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
    // Other user-related methods

    List<Users> getAllReceptionists();
    Optional<Users> findById(Integer userId);
    Users save(Users user);
}