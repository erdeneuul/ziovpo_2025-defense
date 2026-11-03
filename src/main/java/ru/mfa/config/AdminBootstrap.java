package ru.mfa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.mfa.model.AppUser;
import ru.mfa.repository.UserRepository;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.username:}")
    private String username;

    @Value("${bootstrap.admin.password:}")
    private String password;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank()) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD is required when BOOTSTRAP_ADMIN_USERNAME is set");
        }
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new AppUser(
                    username,
                    passwordEncoder.encode(password),
                    "ROLE_ADMIN"));
        }
    }
}
