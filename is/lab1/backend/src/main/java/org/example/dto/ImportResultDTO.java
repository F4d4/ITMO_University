package org.example.dto;

import java.io.Serializable;

/**
 * DTO для результата операции импорта
 */
public class ImportResultDTO implements Serializable {
    
    private Long operationId;
    private String status;
    private Integer addedCount;
    private String errorMessage;

    public ImportResultDTO() {}

    public ImportResultDTO(Long operationId, String status, Integer addedCount, String errorMessage) {
        this.operationId = operationId;
        this.status = status;
        this.addedCount = addedCount;
        this.errorMessage = errorMessage;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}

