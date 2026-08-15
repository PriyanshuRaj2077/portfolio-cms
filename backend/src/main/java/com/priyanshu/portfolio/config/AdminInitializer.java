package com.priyanshu.portfolio.config;

import com.priyanshu.portfolio.entity.AdminUser;
import com.priyanshu.portfolio.repository.AdminUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (adminUserRepository.count() == 0) {
            String envUsername = System.getenv("ADMIN_USERNAME");
            String envPassword = System.getenv("ADMIN_INITIAL_PASSWORD");

            if (envUsername == null || envUsername.isBlank()) {
                envUsername = "admin";
            }
            if (envPassword == null || envPassword.isBlank()) {
                envPassword = "admin";
            }

            AdminUser admin = AdminUser.builder()
                .username(envUsername.trim())
                .passwordHash(passwordEncoder.encode(envPassword.trim()))
                .role("ROLE_ADMIN")
                .build();

            adminUserRepository.save(admin);
            System.out.println(">>> Initial Admin Account successfully created for user: " + envUsername);
        }
    }
}
