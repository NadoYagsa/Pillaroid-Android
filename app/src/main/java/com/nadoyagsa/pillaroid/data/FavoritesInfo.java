package com.nadoyagsa.pillaroid.data;

public class FavoritesInfo {
    private Long favoritesIdx;
    private int medicineIdx;
    private String medicineName;

    public FavoritesInfo(Long favoritesIdx, int medicineIdx, String medicineName) {
        this.favoritesIdx = favoritesIdx;
        this.medicineIdx = medicineIdx;
        this.medicineName = medicineName;
    }

    public Long getFavoritesIdx() {
        return favoritesIdx;
    }

    public int getMedicineIdx() {
        return medicineIdx;
    }

    public String getMedicineName() {
        return medicineName;
    }
}
