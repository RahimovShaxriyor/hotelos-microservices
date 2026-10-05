package com.hotelos.maintenance.dto;

public class ResolveIssueRequest {
    private Boolean requiresCleaning;

    public ResolveIssueRequest() {}

    public ResolveIssueRequest(Boolean requiresCleaning) {
        this.requiresCleaning = requiresCleaning;
    }

    public Boolean getRequiresCleaning() {
        return requiresCleaning;
    }

    public void setRequiresCleaning(Boolean requiresCleaning) {
        this.requiresCleaning = requiresCleaning;
    }

    public boolean isRequiresCleaning() {
        return Boolean.TRUE.equals(requiresCleaning);
    }
}
