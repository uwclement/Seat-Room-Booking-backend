package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantSummaryResponse {
    private Integer totalInvited;
    private Integer totalAccepted;
    private Integer totalDeclined;
    private Integer totalPending;
    private Integer roomCapacity;
    private Boolean capacityMet;
    private String capacityWarning;
}