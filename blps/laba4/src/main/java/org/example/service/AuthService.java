package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.example.config.CamundaIdentityConfig;
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
    private final IdentityService identityService;

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

        createCamundaUser(user, request.getPassword());

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole());
    }

    private void createCamundaUser(User user, String rawPassword) {
        String userId = user.getUsername();
        String groupId = user.getRole() == Role.MODERATOR
                ? CamundaIdentityConfig.GROUP_MODERATOR
                : CamundaIdentityConfig.GROUP_USER;

        if (identityService.createUserQuery().userId(userId).count() == 0) {
            org.camunda.bpm.engine.identity.User camundaUser = identityService.newUser(userId);
            camundaUser.setFirstName(user.getUsername());
            camundaUser.setLastName(user.getRole().name());
            camundaUser.setEmail(user.getEmail());
            camundaUser.setPassword(rawPassword);
            identityService.saveUser(camundaUser);
        }

        boolean alreadyMember = identityService.createGroupQuery()
                .groupMember(userId)
                .groupId(groupId)
                .count() > 0;
        if (!alreadyMember) {
            identityService.createMembership(userId, groupId);
        }

        log.info("Создан пользователь Camunda '{}' в группе '{}'", userId, groupId);
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
