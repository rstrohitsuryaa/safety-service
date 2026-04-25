package com.buildsmart.safety.web.dto;

import com.buildsmart.safety.domain.model.InspectionStatus;
import com.buildsmart.safety.domain.model.InspectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class InspectionDtos {

    public record CreateInspectionRequest(
            @NotBlank(message = "projectId is required")
            String projectId,

            @NotNull(message = "inspectionType is required")
            InspectionType inspectionType,

            @NotBlank(message = "findings is required")
            @Size(min = 20, max = 200, message = "findings must be between 20 and 200 characters")
            String findings
    ) {}

    public record InspectionResponse(
            String inspectionId,
            String projectId,
            LocalDate date,
            String officerId,
            String officerName,
            InspectionType inspectionType,
            String findings,
            InspectionStatus status
    ) {}
}
