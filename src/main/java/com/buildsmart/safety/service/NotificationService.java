package com.buildsmart.safety.service;

import com.buildsmart.safety.domain.model.Incident;
import com.buildsmart.safety.domain.model.IncidentStatus;
import com.buildsmart.safety.domain.model.SafetyInspection;
import com.buildsmart.safety.domain.model.InspectionStatus;
import com.buildsmart.safety.web.dto.NotificationDtos.NotificationResponse;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    List<NotificationResponse> getUserNotifications(String userId);

    long getUnreadCount(String userId);

    NotificationResponse markAsRead(String notificationId);

    int markAllAsRead(String userId);

    void notifyIncidentReported(Incident incident);

    void notifyIncidentStatusChanged(Incident incident, IncidentStatus oldStatus);

    void notifyInspectionScheduled(SafetyInspection inspection);

    void notifyInspectionStatusChanged(SafetyInspection inspection, InspectionStatus oldStatus);
}
