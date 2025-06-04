package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class SmartRoomSuggestionResponse {
     private List<RoomSuggestion> suggestions;
    private String basedOn; // "history", "preferences", "equipment", "availability"
    
    @Data
    public static class RoomSuggestion {
        private RoomResponse room;
        private Double matchScore; // 0.0 to 1.0
        private List<String> reasons;
        private List<TimeSlot> suggestedTimes;
        private boolean isAvailable;
        private LocalDateTime nextAvailableTime;
        
        @Data
        public static class TimeSlot {
            private LocalDateTime startTime;
            private LocalDateTime endTime;
            private String reason;
        }
    }
}
