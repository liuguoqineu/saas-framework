package com.saas.framework.entity.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rp_report_revision")
public class RpReportRevision {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private String revisionType;
    private String contentSnapshot;
    private Long operatorId;
    private LocalDateTime createTime;
}
