package com.tuckersoft.branchengine.user;

import com.tuckersoft.branchengine.common.ApiException;
import com.tuckersoft.branchengine.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("El email ya esta registrado");
        }
        User u = new User();
        u.setEmail(req.email());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setDisplayName(req.displayName());
        u.setRole("ROLE_USER");
        u.setCreatedAt(Instant.now());
        userRepository.save(u);
        return toAuth(u);
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByEmail(req.email())
                .filter(x -> passwordEncoder.matches(req.password(), x.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Credenciales incorrectas"));
        return toAuth(u);
    }

    private AuthResponse toAuth(User u) {
        return new AuthResponse(jwtService.generateToken(u.getEmail()), "Bearer",
                u.getEmail(), u.getDisplayName(), u.getRole());
    }
}
