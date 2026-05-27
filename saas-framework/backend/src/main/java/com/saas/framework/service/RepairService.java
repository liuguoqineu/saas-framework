package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.*;
import com.saas.framework.entity.BizRepairAttachment;
import com.saas.framework.entity.BizRepairOrder;
import com.saas.framework.entity.BizRepairProcessLog;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface RepairService {

    IPage<BizRepairOrder> page(int page, int size, String customerName, String repairTimeStart,
                                String repairTimeEnd, String status, String assigneeName,
                                String urgency, String repairType);

    BizRepairOrder create(RepairOrderRequest request);

    void update(Long id, RepairOrderRequest request);

    BizRepairOrder detail(Long id);

    void delete(Long id);

    void assign(Long id, RepairAssignRequest request);

    void process(Long id, RepairProcessRequest request);

    void confirm(Long id);

    void markException(Long id, RepairExceptionRequest request);

    List<BizRepairAttachment> listAttachments(Long repairId);

    void uploadAttachment(Long repairId, MultipartFile file, String fileType);

    void deleteAttachment(Long attachmentId);

    void downloadAttachment(Long attachmentId, HttpServletResponse response);

    List<BizRepairProcessLog> listProcessLogs(Long repairId);

    RepairStatsVO stats();

    void exportRepairOrders(HttpServletResponse response, String customerName, String repairTimeStart,
                             String repairTimeEnd, String status, String assigneeName,
                             String urgency, String repairType);

    List<BizRepairOrder> getUnconfirmedReminders();
}
