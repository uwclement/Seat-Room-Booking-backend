package com.auca.library.dto.response;

import com.auca.library.model.WaitList;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaitListDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Long seatId;
    private String seatNumber;
    private LocalDateTime requestedStartTime;
    private LocalDateTime requestedEndTime;
    private LocalDateTime createdAt;
    private int queuePosition;
    private boolean notified;
    private LocalDateTime notifiedAt;
    private WaitList.WaitListStatus status;
}
