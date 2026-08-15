package com.priyanshu.portfolio.controller;

import com.priyanshu.portfolio.entity.AdminUser;
import com.priyanshu.portfolio.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> creds, HttpServletRequest request) {
        String username = creds.get("username");
        String password = creds.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("authenticated", false, "message", "Username and password required"));
        }

        Optional<AdminUser> adminOpt = adminUserRepository.findByUsername(username);
        if (adminOpt.isEmpty() || !passwordEncoder.matches(password, adminOpt.get().getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false, "message", "Invalid username or password"));
        }

        List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(adminOpt.get().getRole()));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("username", username);
        response.put("role", adminOpt.get().getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of("authenticated", true, "username", auth.getName()));
        }
        return ResponseEntity.ok(Map.of("authenticated", false));
    }
}
