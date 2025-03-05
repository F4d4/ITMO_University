package org.example.entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @JsonIgnore // Не отправлять поле в ответе
    @Column(nullable = false)
    private String passwordHash;

    @Transient // Не сохранять в базу данных
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Только для десериализации
    private String password;


}
