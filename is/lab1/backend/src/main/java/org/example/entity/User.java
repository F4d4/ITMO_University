package org.example.entity;

import java.io.Serializable;

/**
 * Пользователь системы
 * Маппинг через User.hbm.xml
 */
public class User implements Serializable {

    private Long id;
    private String username;
    private boolean admin;

    public User() {
    }

    public User(String username, boolean admin) {
        this.username = username;
        this.admin = admin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}

