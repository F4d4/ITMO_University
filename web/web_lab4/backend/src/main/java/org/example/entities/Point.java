// backend/src/main/java/org/example/entities/Point.java
package org.example.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "points")
public class Point {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Координата X
    @Column(nullable = false)
    private double x;

    // Координата Y
    @Column(nullable = false)
    private double y;

    // Радиус R
    @Column(nullable = false)
    private double r;

    // Результат попадания (true - попал, false - не попал)
    @Column(nullable = false)
    private boolean hit;

    @Column(nullable = false)
    private long time;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
