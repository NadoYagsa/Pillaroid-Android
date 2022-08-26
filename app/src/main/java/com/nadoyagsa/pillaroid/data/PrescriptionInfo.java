package com.nadoyagsa.pillaroid.data;

public class PrescriptionInfo {
    private final int medicineIdx;          // 의약품 번호
    private final String name;              // 의약품 이름
    private final AppearanceInfo appearanceInfo;      // 외형 정보
    private final String efficacy;          // 효능 및 효과
    private final String dosage;            // 용법 및 용량
    private final boolean isFavorites;      // 즐겨찾기 여부

    public PrescriptionInfo(int medicineIdx, String name, AppearanceInfo appearanceInfo, String efficacy, String dosage, boolean isFavorites) {
        this.medicineIdx = medicineIdx;
        this.name = name;
        this.appearanceInfo = appearanceInfo;
        this.efficacy = efficacy;
        this.dosage = dosage;
        this.isFavorites = isFavorites;
    }

    public int getMedicineIdx() {
        return medicineIdx;
    }

    public String getName() {
        return name;
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

    public boolean isFavorites() {
        return isFavorites;
    }
}
