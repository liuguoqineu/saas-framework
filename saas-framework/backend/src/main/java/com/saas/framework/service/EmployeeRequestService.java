package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.EmployeeRequestBatchReviewDTO;
import com.saas.framework.common.dto.EmployeeRequestCreateDTO;
import com.saas.framework.common.dto.EmployeeRequestReviewDTO;
import com.saas.framework.entity.SysEmployeeRequest;

/**
 * 员工申请服务接口
 */
public interface EmployeeRequestService {

    /**
     * 租户管理员提交员工申请
     */
    void submit(EmployeeRequestCreateDTO request);

    /**
     * 超级管理员分页查询员工申请列表
     */
    IPage<SysEmployeeRequest> page(int page, int size, String status);

    /**
     * 租户管理员查询本租户的申请列表
     */
    IPage<SysEmployeeRequest> myRequests(int page, int size, String status);

    /**
     * 根据ID查询员工申请详情
     */
    SysEmployeeRequest getById(Long id);

    /**
     * 超级管理员审核员工申请
     */
    void review(Long id, EmployeeRequestReviewDTO request);

    /**
     * 超级管理员批量审核员工申请
     */
    void batchReview(EmployeeRequestBatchReviewDTO request);

    /**
     * 租户管理员撤销申请（仅限待审核状态）
     */
    void cancel(Long id);
}
