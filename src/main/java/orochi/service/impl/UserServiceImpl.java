package orochi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.PasswordResetToken;
import orochi.model.Users;
import orochi.repository.PasswordResetTokenRepository;
import orochi.repository.UserRepository;
import orochi.service.UserService;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
    }

    @Override
    public Integer getTotalUsers() {
        try {
            return Math.toIntExact(userRepository.count());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getGuestUsers() {
        try {
            return userRepository.countByIsGuest(true);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getNewUsersToday() {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
            return userRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getGrowthPercentage() {
        try {
            YearMonth currentMonth = YearMonth.now();
            YearMonth previousMonth = currentMonth.minusMonths(1);

            LocalDateTime startOfPreviousMonth = previousMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfPreviousMonth = previousMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusSeconds(1);

            LocalDateTime startOfCurrentMonth = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfCurrentMonth = currentMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusSeconds(1);

            Integer previousMonthUsers = userRepository.countByCreatedAtBetween(startOfPreviousMonth, endOfPreviousMonth);
            Integer currentMonthUsers = userRepository.countByCreatedAtBetween(startOfCurrentMonth, endOfCurrentMonth);

            if (previousMonthUsers == null || previousMonthUsers == 0) {
                return currentMonthUsers > 0 ? 100 : 0;
            }

            if (currentMonthUsers == null) {
                currentMonthUsers = 0;
            }

            return (currentMonthUsers - previousMonthUsers) * 100 / previousMonthUsers;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Users registerNewUser(Users user) {
        Optional<Users> existingUser = userRepository.findByEmail(user.getEmail());
        boolean phoneNumberExists = userRepository.existsByPhoneNumber(user.getPhoneNumber());
        if (existingUser.isPresent()) {
            throw new RuntimeException("User with email " + user.getEmail() + " already exists.");
        }

        if (phoneNumberExists) {
            throw new RuntimeException("User with phone number " + user.getPhoneNumber() + " already exists.");
        }

        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public String generatePasswordResetToken(String email) {
        Optional<Users> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("No user found with email: " + email);
        }

        Users user = userOptional.get();
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(10);

        Optional<PasswordResetToken> existingToken = tokenRepository.findByUserId(user.getUserId());
        if (existingToken.isPresent()) {
            PasswordResetToken resetToken = existingToken.get();
            resetToken.setToken(token);
            resetToken.setExpiryDate(expiryDate);
            resetToken.setUsed(false);
            tokenRepository.save(resetToken);
        } else {
            PasswordResetToken resetToken = new PasswordResetToken(user.getUserId(), token, expiryDate);
            tokenRepository.save(resetToken);
        }

        logger.info("Password reset token generated for user: {}", email);
        return token;
    }

    @Override
    public boolean validatePasswordResetToken(String token, String email) {
        Optional<Users> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            logger.warn("Token validation failed: No user found with email: {}", email);
            return false;
        }

        Users user = userOptional.get();
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        if (tokenOptional.isEmpty()) {
            logger.warn("Token validation failed: Token not found: {}", token);
            return false;
        }

        PasswordResetToken resetToken = tokenOptional.get();
        if (!resetToken.getUserId().equals(user.getUserId())) {
            logger.warn("Token validation failed: Token does not belong to user: {}", email);
            return false;
        }

        if (resetToken.isExpired()) {
            logger.warn("Token validation failed: Token expired for user: {}", email);
            return false;
        }

        if (resetToken.isUsed()) {
            logger.warn("Token validation failed: Token already used for user: {}", email);
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String email, String newPassword) {
        if (!validatePasswordResetToken(token, email)) {
            throw new RuntimeException("Invalid or expired password reset token");
        }

        Optional<Users> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with email: " + email);
        }

        Users user = userOptional.get();
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new RuntimeException("New password cannot be the same as your current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        tokenOptional.ifPresent(resetToken -> {
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);
        });

        logger.info("Password reset successful for user: {}", email);
    }

//    @Override
//    public List<Users> getAllReceptionists() {
//        return userRepository.findByRoleId(3);
//    }

//    @Override
//    public Page<Users> getAllReceptionists(Pageable pageable) {
//        return userRepository.findAllByRoleId(3, pageable);
//    }

    @Override
    public Page<Users> getAllReceptionists(String search, String statusFilter, Pageable pageable) {
        if (search == null && statusFilter == null) {
            return userRepository.findAllByRoleId(3, pageable);
        }

        return userRepository.findAll(new Specification<Users>() {
            @Override
            public Predicate toPredicate(Root<Users> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                Predicate predicates = cb.equal(root.get("roleId"), 3);
                if (search != null && !search.isEmpty()) {
                    predicates = cb.or(predicates,
                            cb.like(cb.lower(root.get("fullName")), "%" + search.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("email")), "%" + search.toLowerCase() + "%"));
                }
                if (statusFilter != null && !statusFilter.isEmpty()) {
                    predicates = cb.and(predicates, cb.equal(cb.lower(root.get("status")), statusFilter.toLowerCase()));
                }
                return predicates;
            }
        }, pageable);
    }

    @Override
    public Optional<Users> findById(Integer userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Users save(Users user) {
        return userRepository.save(user);
    }
}