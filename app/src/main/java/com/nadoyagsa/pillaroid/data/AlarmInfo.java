package com.nadoyagsa.pillaroid.data;

public class AlarmInfo {
    private Long alarmIdx;
    private String medicineName;
    private String details;

    public AlarmInfo(Long alarmIdx, String medicineName, String details) {
        this.alarmIdx = alarmIdx;
        this.medicineName = medicineName;
        this.details = details;
    }

    public Long getAlarmIdx() {
        return alarmIdx;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getDetails() {
        return details;
    }
}
