package org.example.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " с id=" + id + " не найден", HttpStatus.NOT_FOUND);
    }
}
