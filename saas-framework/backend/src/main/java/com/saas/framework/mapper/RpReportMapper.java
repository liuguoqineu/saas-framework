package com.saas.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saas.framework.entity.report.RpReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RpReportMapper extends BaseMapper<RpReport> {

    /**
     * 查询用户某周期报表的总数（包括已删除的）
     * 用于判断用户是否曾经创建过该报表
     */
    @Select("SELECT COUNT(*) FROM rp_report " +
            "WHERE user_id = #{userId} " +
            "AND report_type = #{reportType} " +
            "AND report_period = #{period}")
    long countAllIncludingDeleted(@Param("userId") Long userId,
                                  @Param("reportType") String reportType,
                                  @Param("period") String period);
}
