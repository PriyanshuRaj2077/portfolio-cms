package com.priyanshu.portfolio.config;

import com.priyanshu.portfolio.entity.AdminUser;
import com.priyanshu.portfolio.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String envUsername;
    private final String envPassword;

    public AdminInitializer(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this(adminUserRepository, passwordEncoder, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminInitializer(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_USERNAME:${admin.username:}}") String username,
            @Value("${ADMIN_INITIAL_PASSWORD:${admin.initial-password:}}") String password
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.envUsername = username;
        this.envPassword = password;
    }

    @Override
    public void run(String... args) throws Exception {
        if (adminUserRepository.count() == 0) {
            String username = (envUsername != null && !envUsername.isBlank()) ? envUsername : System.getenv("ADMIN_USERNAME");
            String password = (envPassword != null && !envPassword.isBlank()) ? envPassword : System.getenv("ADMIN_INITIAL_PASSWORD");

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new IllegalStateException(
                    "FATAL SECURITY CONFIGURATION: No admin account exists in database and initial credentials were not provided. " +
                    "Both 'ADMIN_USERNAME' and 'ADMIN_INITIAL_PASSWORD' environment variables MUST be provided to bootstrap the initial admin account."
                );
            }

            AdminUser admin = AdminUser.builder()
                .username(username.trim())
                .passwordHash(passwordEncoder.encode(password.trim()))
                .role("ROLE_ADMIN")
                .build();

            adminUserRepository.save(admin);
            System.out.println(">>> Initial Admin Account successfully created for user: " + username.trim());
        }
    }
}


