package com.saas.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saas.framework.entity.BizContract;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface BizContractMapper extends BaseMapper<BizContract> {

    @Delete("DELETE FROM biz_contract WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
