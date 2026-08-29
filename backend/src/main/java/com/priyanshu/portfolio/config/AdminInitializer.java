package com.priyanshu.portfolio.config;

import com.priyanshu.portfolio.entity.AdminUser;
import com.priyanshu.portfolio.entity.ProfileEntity;
import com.priyanshu.portfolio.entity.SectionEntity;
import com.priyanshu.portfolio.repository.AdminUserRepository;
import com.priyanshu.portfolio.repository.ProfileRepository;
import com.priyanshu.portfolio.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final SectionRepository sectionRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final String envUsername;
    private final String envPassword;

    public AdminInitializer(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this(adminUserRepository, null, null, passwordEncoder, null, null);
    }

    public AdminInitializer(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String password
    ) {
        this(adminUserRepository, null, null, passwordEncoder, username, password);
    }

    public AdminInitializer(
            AdminUserRepository adminUserRepository,
            SectionRepository sectionRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this(adminUserRepository, sectionRepository, profileRepository, passwordEncoder, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminInitializer(
            AdminUserRepository adminUserRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SectionRepository sectionRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_USERNAME:${admin.username:}}") String username,
            @Value("${ADMIN_INITIAL_PASSWORD:${admin.initial-password:}}") String password
    ) {
        this.adminUserRepository = adminUserRepository;
        this.sectionRepository = sectionRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.envUsername = username;
        this.envPassword = password;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Bootstrap Admin Account if needed
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

        // 2. Bootstrap Initial Profile if empty
        if (profileRepository != null && profileRepository.count() == 0) {
            ProfileEntity profile = ProfileEntity.builder()
                    .name("PRIYANSHU")
                    .title("Software & Systems Engineering")
                    .bio("Engineering scalable systems, high-throughput distributed architectures, and atomic static web applications.")
                    .location("Remote / Global")
                    .email("priyanshuraj2077@gmail.com")
                    .githubUrl("https://github.com/Priyanshuraj2077")
                    .linkedinUrl("https://linkedin.com/in/priyanshuraj")
                    .twitterUrl("https://x.com/priyanshuraj")
                    .avatarUrl("")
                    .build();
            profileRepository.save(profile);
            System.out.println(">>> Initial Profile seeded successfully.");
        }

        // 3. Bootstrap Default Sections if empty
        if (sectionRepository != null && sectionRepository.count() == 0) {
            List<SectionEntity> defaultSections = List.of(
                    SectionEntity.builder().id("sec-achievements").title("Achievements").label("01 // HIGHLIGHTS").type("ACHIEVEMENTS").navLetter("A").icon("A").order(1).visible(true).theme("default").build(),
                    SectionEntity.builder().id("sec-experience").title("Experience").label("02 // TIMELINE").type("TIMELINE").navLetter("E").icon("E").order(2).visible(true).theme("default").build(),
                    SectionEntity.builder().id("sec-tech-stack").title("Tech Stack").label("03 // SKILLS").type("SKILLS").navLetter("T").icon("T").order(3).visible(true).theme("default").build(),
                    SectionEntity.builder().id("sec-projects").title("Projects").label("04 // WORK").type("PROJECTS").navLetter("P").icon("P").order(4).visible(true).theme("default").build(),
                    SectionEntity.builder().id("sec-blog").title("Blogs").label("05 // JOURNAL").type("BLOG").navLetter("B").icon("B").order(5).visible(true).theme("default").build(),
                    SectionEntity.builder().id("sec-contact").title("Contact").label("06 // CONNECT").type("CONTACT").navLetter("C").icon("C").order(6).visible(true).theme("default").build()
            );
            sectionRepository.saveAll(defaultSections);
            System.out.println(">>> Default Portfolio Sections seeded successfully.");
        }
    }
}



