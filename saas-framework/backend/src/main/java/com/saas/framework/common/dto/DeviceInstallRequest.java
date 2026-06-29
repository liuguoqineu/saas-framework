package com.saas.framework.common.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DeviceInstallRequest {
    private String installLocation;
    private LocalDate installDate;
    private String installPerson;
    private LocalDate useDate;
    private String acceptRecord;
    private String installFile;
    private String acceptPhoto;
}
