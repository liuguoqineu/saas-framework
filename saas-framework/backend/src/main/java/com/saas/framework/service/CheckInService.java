package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.CheckInRequest;
import com.saas.framework.entity.BizCheckIn;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

public interface CheckInService {

    BizCheckIn checkIn(CheckInRequest request, MultipartFile photo);

    IPage<BizCheckIn> page(int page, int size, String userName, String checkInTimeStart, String checkInTimeEnd);

    BizCheckIn detail(Long id);

    void delete(Long id);

    BizCheckIn todayStatus();

    void downloadPhoto(Long id, HttpServletResponse response);
}
