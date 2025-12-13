package org.example.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * Операция импорта
 * Маппинг через ImportOperation.hbm.xml
 */
public class ImportOperation implements Serializable {

    private Long id;
    private ImportStatus status;
    private User user;
    private Integer addedCount; // Число добавленных объектов (только для успешных операций)
    private String errorMessage; // Сообщение об ошибке (для неудачных операций)
    private Date createdAt;

    public ImportOperation() {
        this.createdAt = new Date();
        this.status = ImportStatus.IN_PROGRESS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public void setStatus(ImportStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(Integer addedCount) {
        this.addedCount = addedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

