package com.saas.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saas.framework.entity.BizCustomer;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 客户信息 Mapper
 * 包含物理删除方法，用于彻底删除误录客户（绕过 @TableLogic 逻辑删除）
 */
public interface BizCustomerMapper extends BaseMapper<BizCustomer> {

    /**
     * 物理删除客户（真正从数据库删除，不受 @TableLogic 影响）
     * 仅用于误录客户的彻底删除场景
     */
    @Delete("DELETE FROM biz_customer WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
