package com.nadoyagsa.pillaroid.data;

public class FavoritesInfo {
    private Long favoritesIdx;
    private String medicineName;

    public FavoritesInfo(Long favoritesIdx, String medicineName) {
        this.favoritesIdx = favoritesIdx;
        this.medicineName = medicineName;
    }

    public Long getFavoritesIdx() {
        return favoritesIdx;
    }

    public String getMedicineName() {
        return medicineName;
    }
}
