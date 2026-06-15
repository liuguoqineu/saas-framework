package com.saas.framework.common.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoginReminderVO {
    private int totalCount;
    private List<ReminderItem> contractReminders;
    private List<ReminderItem> followUpReminders;
    private List<ReminderItem> repairReminders;
    private List<ReminderItem> reportReminders;

    @Data
    public static class ReminderItem {
        private Long id;
        private String type;
        private String title;
        private String content;
        private LocalDateTime time;
        private String person;
        private Integer isRead;
        private Long relatedId;
        private Integer isMine;
    }
}
