package com.buildsmart.safety.domain.model;

public enum SafetyNotificationType {
    INCIDENT_REPORTED("Incident Reported"),
    INCIDENT_STATUS_CHANGED("Incident Status Changed"),
    INSPECTION_SCHEDULED("Inspection Scheduled"),
    INSPECTION_STATUS_CHANGED("Inspection Status Changed");

    private final String displayName;

    SafetyNotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
