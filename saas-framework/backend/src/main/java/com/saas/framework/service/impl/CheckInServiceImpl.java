package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.CheckInRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.config.FilePathConfig;
import com.saas.framework.entity.BizCheckIn;
import com.saas.framework.mapper.BizCheckInMapper;
import com.saas.framework.service.CheckInService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Service
public class CheckInServiceImpl implements CheckInService {

    @Resource
    private BizCheckInMapper checkInMapper;

    @Resource
    private FilePathConfig filePathConfig;

    @Override
    public BizCheckIn checkIn(CheckInRequest request, MultipartFile photo) {
        Long userId = UserContext.getUserId();
        String userName = UserContext.getUsername();
        Long tenantId = TenantContext.getTenantId();

        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }

        // 检查今天是否已打卡
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<BizCheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizCheckIn::getUserId, userId)
                .between(BizCheckIn::getCheckInTime, today.atStartOfDay(), today.atTime(LocalTime.MAX));
        Long count = checkInMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("今天已经打卡了");
        }

        BizCheckIn checkIn = new BizCheckIn();
        checkIn.setUserId(userId);
        checkIn.setUserName(userName);
        checkIn.setCheckInTime(LocalDateTime.now());
        checkIn.setAddress(request.getAddress());
        checkIn.setRemark(request.getRemark());
        checkIn.setTenantId(tenantId);

        // 处理照片上传
        if (photo != null && !photo.isEmpty()) {
            String photoPath = savePhoto(photo);
            checkIn.setPhotoPath(photoPath);
        }

        checkInMapper.insert(checkIn);
        log.info("用户 {} 打卡成功, 时间: {}, 地点: {}", userName, checkIn.getCheckInTime(), request.getAddress());
        return checkIn;
    }

    @Override
    public IPage<BizCheckIn> page(int page, int size, String userName, String checkInTimeStart, String checkInTimeEnd) {
        LambdaQueryWrapper<BizCheckIn> wrapper = new LambdaQueryWrapper<>();

        // 普通员工只能查看自己的打卡记录，管理员可查看所有
        if (!UserContext.isSuperAdmin()) {
            Long currentUserId = UserContext.getUserId();
            boolean hasDeletePerm = UserContext.getPermissions() != null
                    && UserContext.getPermissions().contains("checkin:delete");
            if (!hasDeletePerm) {
                wrapper.eq(BizCheckIn::getUserId, currentUserId);
            }
        }

        wrapper.like(userName != null && !userName.isEmpty(), BizCheckIn::getUserName, userName);

        if (checkInTimeStart != null && !checkInTimeStart.isEmpty()) {
            wrapper.ge(BizCheckIn::getCheckInTime, LocalDateTime.parse(checkInTimeStart.replace(" ", "T")));
        }
        if (checkInTimeEnd != null && !checkInTimeEnd.isEmpty()) {
            wrapper.le(BizCheckIn::getCheckInTime, LocalDateTime.parse(checkInTimeEnd.replace(" ", "T")));
        }

        wrapper.orderByDesc(BizCheckIn::getCheckInTime);
        return checkInMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public BizCheckIn detail(Long id) {
        BizCheckIn checkIn = checkInMapper.selectById(id);
        if (checkIn == null) {
            throw new BusinessException(404, "打卡记录不存在");
        }
        return checkIn;
    }

    @Override
    public void delete(Long id) {
        BizCheckIn checkIn = checkInMapper.selectById(id);
        if (checkIn == null) {
            throw new BusinessException(404, "打卡记录不存在");
        }
        checkInMapper.deleteById(id);
    }

    @Override
    public BizCheckIn todayStatus() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<BizCheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizCheckIn::getUserId, userId)
                .between(BizCheckIn::getCheckInTime, today.atStartOfDay(), today.atTime(LocalTime.MAX))
                .orderByDesc(BizCheckIn::getCheckInTime)
                .last("LIMIT 1");
        return checkInMapper.selectOne(wrapper);
    }

    private String savePhoto(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        File uploadDir = new File(filePathConfig.getUploadPath() + "check-in/");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File dest = new File(uploadDir, storedFileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("打卡照片上传失败", e);
            throw new BusinessException("打卡照片上传失败: " + e.getMessage());
        }

        return "check-in/" + storedFileName;
    }

    @Override
    public void downloadPhoto(Long id, HttpServletResponse response) {
        BizCheckIn checkIn = checkInMapper.selectById(id);
        if (checkIn == null) {
            throw new BusinessException(404, "打卡记录不存在");
        }

        if (checkIn.getPhotoPath() == null || checkIn.getPhotoPath().isEmpty()) {
            throw new BusinessException(404, "该打卡记录没有照片");
        }

        File file = new File(filePathConfig.getUploadPath(), checkIn.getPhotoPath());
        if (!file.exists()) {
            throw new BusinessException(404, "照片文件不存在");
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            String fileName = checkIn.getPhotoPath().substring(checkIn.getPhotoPath().lastIndexOf("/") + 1);
            response.setContentType("image/jpeg");
            response.setHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            response.setContentLengthLong(file.length());

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            log.error("照片下载失败", e);
            throw new BusinessException("照片下载失败: " + e.getMessage());
        }
    }
}
