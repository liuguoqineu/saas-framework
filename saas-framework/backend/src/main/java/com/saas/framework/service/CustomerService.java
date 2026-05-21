package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.CustomerRequest;
import com.saas.framework.entity.BizCustomer;
import com.saas.framework.entity.BizCustomerAttachment;
import com.saas.framework.entity.BizCustomerModifyLog;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 客户服务接口
 */
public interface CustomerService {

    /**
     * 分页查询客户（支持多条件组合筛选，自动租户隔离，默认过滤无效客户）
     */
    IPage<BizCustomer> page(int page, int size, String name, String businessCategory,
                            String businessType, String cooperationCategory,
                            String cooperationStatus, String region, String contactPerson,
                            String maintenanceCategory);

    /**
     * 新增客户（自动填充 tenant_id）
     */
    void create(CustomerRequest request);
    
    /**
     * 修改客户信息（自动记录修改日志）
     */
    void update(Long id, CustomerRequest request);

    /**
     * 查看客户详情
     */
    BizCustomer detail(Long id);

    /**
     * 标记客户为无效
     */
    void markInvalid(Long id);

    /**
     * 彻底删除客户（物理删除，需权限验证）
     */
    void delete(Long id);

    /**
     * 查询客户附件列表
     */
    List<BizCustomerAttachment> listAttachments(Long customerId);

    /**
     * 上传客户附件
     */
    void uploadAttachment(Long customerId, MultipartFile file, String fileType);

    /**
     * 删除客户附件
     */
    void deleteAttachment(Long attachmentId);

    /**
     * 下载客户附件
     */
    void downloadAttachment(Long attachmentId, HttpServletResponse response);

    /**
     * 查询客户修改记录
     */
    List<BizCustomerModifyLog> listModifyLogs(Long customerId);

    /**
     * Excel批量导入客户
     */
    void importCustomers(MultipartFile file);

    /**
     * Excel导出客户列表
     */
    void exportCustomers(HttpServletResponse response, String name, String businessCategory,
                         String businessType, String cooperationCategory,
                         String cooperationStatus, String region);
}
