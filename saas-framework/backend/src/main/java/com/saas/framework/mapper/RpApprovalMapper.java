package com.saas.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saas.framework.entity.report.RpApproval;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface RpApprovalMapper extends BaseMapper<RpApproval> {

    @Select("SELECT a.* FROM rp_approval a INNER JOIN rp_report r ON a.report_id = r.id WHERE a.approver_id = #{approverId} AND a.status = 'PENDING' AND r.deleted = 0 ORDER BY a.create_time DESC")
    List<RpApproval> selectPendingByApproverId(@Param("approverId") Long approverId);
}
