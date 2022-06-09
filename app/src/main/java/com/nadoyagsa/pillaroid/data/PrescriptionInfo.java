package com.nadoyagsa.pillaroid.data;

public class PrescriptionInfo {
    private Long medicineIdx;
    private String medicineName;
    private boolean isFavorites = false;
    private AppearanceInfo appearanceInfo;
    private String efficacy;    // 효능 및 효과
    private String usage;       // 용법 및 용량

    public PrescriptionInfo(Long medicineIdx, String medicineName, AppearanceInfo appearanceInfo, String efficacy, String usage) {
        this.medicineIdx = medicineIdx;
        this.medicineName = medicineName;
        this.appearanceInfo = appearanceInfo;
        this.efficacy = efficacy;
        this.usage = usage;
    }

    public Long getMedicineIdx() {
        return medicineIdx;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public boolean isFavorites() {
        return isFavorites;
    }

    public AppearanceInfo getAppearanceInfo() {
        return appearanceInfo;
    }

    public String getEfficacy() {
        return efficacy;
    }

    public String getUsage() {
        return usage;
    }
}
