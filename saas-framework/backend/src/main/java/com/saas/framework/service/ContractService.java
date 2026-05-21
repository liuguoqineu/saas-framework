package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.ContractRequest;
import com.saas.framework.common.dto.ContractStatusChangeRequest;
import com.saas.framework.entity.BizContract;
import com.saas.framework.entity.BizContractAttachment;
import com.saas.framework.entity.BizContractModifyLog;
import com.saas.framework.entity.BizContractReminder;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface ContractService {

    IPage<BizContract> page(int page, int size, String contractNo, String customerName,
                            String signDateStart, String signDateEnd, String expireDateStart,
                            String expireDateEnd, String contractStatus);

    BizContract create(ContractRequest request);

    void update(Long id, ContractRequest request);

    BizContract detail(Long id);

    void delete(Long id);

    void changeStatus(Long id, ContractStatusChangeRequest request);

    List<BizContractAttachment> listAttachments(Long contractId);

    void uploadAttachment(Long contractId, MultipartFile file, String fileType);

    void deleteAttachment(Long attachmentId);

    void downloadAttachment(Long attachmentId, HttpServletResponse response);

    List<BizContractModifyLog> listModifyLogs(Long contractId);

    List<BizContractReminder> listReminders(Long contractId);

    List<BizContractReminder> getPendingReminders();

    void markReminderRead(Long reminderId);

    void markReminderHandled(Long reminderId);

    void generateReminders(Long contractId);

    void exportContracts(HttpServletResponse response, String contractNo, String customerName,
                         String signDateStart, String signDateEnd, String expireDateStart,
                         String expireDateEnd, String contractStatus);
}
