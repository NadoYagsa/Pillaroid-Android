package com.nadoyagsa.pillaroid.data;

public class PrescriptionInfo {
    private final int medicineIdx;
    private final String name;
    private boolean isFavorites = false;
    private final AppearanceInfo appearanceInfo;      // 외형 정보
    private final String efficacy;        // 효능 및 효과
    private final String dosage;          // 용법 및 용량

    public PrescriptionInfo(int medicineIdx, String name, AppearanceInfo appearanceInfo, String efficacy, String dosage) {
        this.medicineIdx = medicineIdx;
        this.name = name;
        this.appearanceInfo = appearanceInfo;
        this.efficacy = efficacy;
        this.dosage = dosage;
    }

    public int getMedicineIdx() {
        return medicineIdx;
    }

    public String getName() {
        return name;
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

    public String getDosage() {
        return dosage;
    }
}
