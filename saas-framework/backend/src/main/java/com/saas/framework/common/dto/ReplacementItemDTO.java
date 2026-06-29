package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class ReplacementItemDTO {

    private Integer itemType;

    private Long oldDeviceId;

    private String oldItemName;

    private String oldItemModel;

    private Integer oldItemStatus;

    private Long newDeviceId;

    private String newItemName;

    private String newItemModel;

    private Integer newItemQty;
}
