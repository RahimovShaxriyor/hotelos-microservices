package com.hotelos.maintenance.dto;

public class CreateInternalIssueRequest {
    private String roomNumber;
    private String description;
    private String priority;
    private String source;
    private String sourceReferenceId;

    public CreateInternalIssueRequest() {}

    public CreateInternalIssueRequest(String roomNumber, String description, String priority, String source, String sourceReferenceId) {
        this.roomNumber = roomNumber;
        this.description = description;
        this.priority = priority;
        this.source = source;
        this.sourceReferenceId = sourceReferenceId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public void setSourceReferenceId(String sourceReferenceId) {
        this.sourceReferenceId = sourceReferenceId;
    }
}
