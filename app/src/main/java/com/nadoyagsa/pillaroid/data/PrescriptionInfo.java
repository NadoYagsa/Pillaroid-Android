package com.nadoyagsa.pillaroid.data;

public class PrescriptionInfo {
    private final int medicineIdx;          // 의약품 번호
    private final String name;              // 의약품 이름
    private final AppearanceInfo appearanceInfo;      // 외형 정보
    private final String efficacy;          // 효능 및 효과
    private final String dosage;            // 용법 및 용량

    private Long favoritesIdx;              // 즐겨찾기 번호

    public PrescriptionInfo(int medicineIdx, String name, AppearanceInfo appearanceInfo, String efficacy, String dosage, Long favoritesIdx) {
        this.medicineIdx = medicineIdx;
        this.name = name;
        this.appearanceInfo = appearanceInfo;
        this.efficacy = efficacy;
        this.dosage = dosage;
        this.favoritesIdx = favoritesIdx;
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

    public boolean isFavoritesNull() {
        return favoritesIdx == null;
    }

    public Long getFavoritesIdx() {
        return favoritesIdx;
    }

    public void setFavoritesIdx(Long favoritesIdx) {
        this.favoritesIdx = favoritesIdx;
    }
}
