package com.nadoyagsa.pillaroid.data;

public class FavoritesInfo {
    private final Long favoritesIdx;
    private final int medicineIdx;
    private final String medicineName;

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
