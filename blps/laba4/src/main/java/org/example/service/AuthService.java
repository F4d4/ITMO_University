package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.CreateUserRequest;
import org.example.dto.request.LoginRequest;
import org.example.dto.response.AuthResponse;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.exception.BusinessException;
import org.example.repository.UserRepository;
import org.example.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(
                    "Пользователь с именем \"" + request.getUsername() + "\" уже существует"
            );
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    "Пользователь с email \"" + request.getEmail() + "\" уже существует"
            );
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);

        user = userRepository.save(user);
        log.info("Зарегистрирован пользователь id={}, username={}", user.getId(), user.getUsername());

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Пользователь не найден", HttpStatus.UNAUTHORIZED));

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        log.info("Пользователь {} вошёл в систему", user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}
