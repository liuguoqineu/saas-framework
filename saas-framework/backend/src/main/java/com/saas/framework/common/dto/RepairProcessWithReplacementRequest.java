package com.saas.framework.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RepairProcessWithReplacementRequest {

    private String processMethod;

    private String faultReason;

    private String repairStartTime;

    private String repairEndTime;

    private BigDecimal repairDuration;

    private String repairPhotoAfter;

    private Integer hasReplacement;

    private Integer replacementType;

    private String replacePerson;

    private String replaceReason;

    private String replacePhoto;

    private List<ReplacementItemDTO> replacementItems;
}
