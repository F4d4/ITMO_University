package org.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Имя пользователя обязательно")
    private String username;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;
}
