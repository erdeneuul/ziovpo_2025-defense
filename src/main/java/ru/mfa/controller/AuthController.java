package ru.mfa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.mfa.model.AppUser;
import ru.mfa.repository.UserRepository;
import ru.mfa.service.TokenPairService;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenPairService tokenPairService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          TokenPairService tokenPairService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenPairService = tokenPairService;
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        try {
            AppUser user = userRepository.findByUsername(request.username)
                    .orElseThrow(() -> new RuntimeException("Неверный логин или пароль"));

            if (!passwordEncoder.matches(request.password, user.getPassword())) {
                throw new RuntimeException("Неверный логин или пароль");
            }

            Map<String, String> tokens = tokenPairService.login(request.username, request.password);
            return ResponseEntity.ok(tokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // POST /auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestBody RefreshRequest request) {
        try {
            Map<String, String> tokens = tokenPairService.refresh(request.refreshToken);
            return ResponseEntity.ok(tokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // POST /auth/register
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity.badRequest()
                    .body("Пользователь уже существует: " + request.username);
        }
        String passwordError = validatePassword(request.password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(passwordError);
        }
        String role = "ROLE_USER";
        AppUser user = new AppUser(
                request.username,
                passwordEncoder.encode(request.password),
                role
        );
        userRepository.save(user);
        return ResponseEntity.ok("Зарегистрирован: " + request.username + " роль: " + role);
    }

    @PostMapping("/admin/register")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> registerAdmin(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity.badRequest()
                    .body("Пользователь уже существует: " + request.username);
        }
        String passwordError = validatePassword(request.password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(passwordError);
        }
        AppUser user = new AppUser(
                request.username,
                passwordEncoder.encode(request.password),
                "ROLE_ADMIN"
        );
        userRepository.save(user);
        return ResponseEntity.ok("Администратор зарегистрирован: " + request.username);
    }

    // GET /auth/me — requires valid access token
    @GetMapping("/me")
    public ResponseEntity<String> me(Authentication authentication) {
        return ResponseEntity.ok(
            "Вы вошли как: " + authentication.getName() +
            " | Роли: " + authentication.getAuthorities()
        );
    }

    private String validatePassword(String password) {
        if (password == null || password.length() < 8)
            return "Пароль должен содержать минимум 8 символов.";
        if (!password.matches(".*[A-Z].*"))
            return "Пароль должен содержать хотя бы одну заглавную букву.";
        if (!password.matches(".*[0-9].*"))
            return "Пароль должен содержать хотя бы одну цифру.";
        if (!password.matches(".*[!@#$%^&*].*"))
            return "Пароль должен содержать хотя бы один спецсимвол: !@#$%^&*";
        return null;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class RefreshRequest {
        public String refreshToken;
    }

    public static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }
}
