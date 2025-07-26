package orochi.config;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {

    @Getter
    private final Integer userId;

    @Getter
    private final Integer doctorId;

    @Getter
    private final Integer patientId;

    @Getter
    private final String userStatus;

    public CustomUserDetails(String username, String password,
                             Collection<? extends GrantedAuthority> authorities,
                             Integer userId, Integer doctorId, Integer patientId, String userStatus) {
        super(username, password, true, true, true, !"LOCKED".equals(userStatus), authorities);
        this.userId = userId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.userStatus = userStatus;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equals(userStatus);
    }
}