package com.nadoyagsa.pillaroid.data;

public class VoiceResultInfo {
    private final int medicineIdx;      // 의약품 DB 내 고유 번호
    private final String medicineName;  // 품목명

    public VoiceResultInfo(int medicineIdx, String medicineName) {
        this.medicineIdx = medicineIdx;
        this.medicineName = medicineName;
    }

    public int getMedicineIdx() {
        return medicineIdx;
    }

    public String getMedicineName() {
        return medicineName;
    }
}
