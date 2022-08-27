package com.nadoyagsa.pillaroid.data;

public class MedicineInfo {
    private final int medicineIdx;          // 의약품 idx
    private final String name;              // 품목명
    private final String efficacy;          // 효능효과
    private final String dosage;            // 용법용량
    private final String precaution;        // 주의사항
    private final AppearanceInfo appearanceInfo;        // 외형
    private final String ingredient;        // 성분
    private final String save;              // 보관법

    private Long favoritesIdx;              // 즐겨찾기 번호

    // 처방전 용으로 외형정보가 있음
    public MedicineInfo(int medicineIdx, String name, String efficacy, String dosage, String precaution, AppearanceInfo appearanceInfo, String ingredient, String save, Long favoritesIdx) {
        this.medicineIdx = medicineIdx;
        this.name = name;
        this.efficacy = efficacy;
        this.dosage = dosage;
        this.precaution = precaution;
        this.appearanceInfo = appearanceInfo;
        this.ingredient = ingredient;
        this.save = save;
        this.favoritesIdx = favoritesIdx;
    }

    public int getMedicineIdx() {
        return medicineIdx;
    }

    public String getName() {
        return name;
    }

    public String getEfficacy() {
        return efficacy;
    }

    public String getDosage() {
        return dosage;
    }

    public String getPrecaution() {
        return precaution;
    }

    public AppearanceInfo getAppearanceInfo() {
        return appearanceInfo;
    }

    public String getIngredient() {
        return ingredient;
    }

    public String getSave() {
        return save;
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
