package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.CustomerStatusChangeRequest;
import com.saas.framework.common.dto.FollowUpRecordRequest;
import com.saas.framework.entity.BizCustomerStatusLog;
import com.saas.framework.entity.BizFollowUpRecord;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface FollowUpService {

    IPage<BizFollowUpRecord> pageRecords(int page, int size, Long customerId, String customerName,
                                          Long followUpPersonId, String followUpPerson,
                                          Integer followUpStatus, Integer followUpMethod,
                                          String startTime, String endTime);

    BizFollowUpRecord createRecord(FollowUpRecordRequest request);

    BizFollowUpRecord updateRecord(Long id, FollowUpRecordRequest request);

    void deleteRecord(Long id);

    BizFollowUpRecord getRecordDetail(Long id);

    void exportRecords(HttpServletResponse response, Long customerId, String customerName,
                       Long followUpPersonId, String followUpPerson,
                       Integer followUpStatus, Integer followUpMethod,
                       String startTime, String endTime);

    List<BizFollowUpRecord> listRecordsByCustomerId(Long customerId);

    void changeCustomerStatus(Long customerId, CustomerStatusChangeRequest request);

    List<BizCustomerStatusLog> listStatusLogs(Long customerId);
}
