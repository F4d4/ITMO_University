package org.example.security;

import lombok.RequiredArgsConstructor;
import org.example.entity.User;
import org.example.exception.BusinessException;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Пользователь не авторизован", HttpStatus.UNAUTHORIZED);
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Пользователь не найден: " + username, HttpStatus.UNAUTHORIZED));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
