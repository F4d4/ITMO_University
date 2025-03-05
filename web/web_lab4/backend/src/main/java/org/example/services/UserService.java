package org.example.services;

import org.example.entities.User;
import org.example.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import at.favre.lib.crypto.bcrypt.BCrypt;

@ApplicationScoped
public class UserService {

    @Inject
    private UserRepository userRepository;

    public boolean register(User user) {
        if (isUsernameTaken(user.getUsername())) {
            return false;
        }
        String passwordHash = hashPassword(user.getPassword());
        user.setPasswordHash(passwordHash);
        userRepository.createUser(user);
        return true;
    }

    private boolean isUsernameTaken(String username) {
        return userRepository.findByUsername(username) != null;
    }

    public boolean authenticate(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        return verifyPassword(password, user.getPasswordHash());
    }

    private String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    private boolean verifyPassword(String password, String hash) {
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
        return result.verified;
    }
}
