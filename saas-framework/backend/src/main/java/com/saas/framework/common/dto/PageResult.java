package com.saas.framework.common.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页响应 DTO
 * 格式: { records: [], total: 0, size: 10, current: 1 }
 */
@Data
public class PageResult<T> {

    /** 当前页数据列表 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 每页条数 */
    private long size;

    /** 当前页码 */
    private long current;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setSize(page.getSize());
        result.setCurrent(page.getCurrent());
        return result;
    }
}
