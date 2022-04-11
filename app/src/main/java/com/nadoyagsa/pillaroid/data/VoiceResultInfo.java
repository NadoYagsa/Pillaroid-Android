package com.nadoyagsa.pillaroid.data;

public class VoiceResultInfo {
    private final Long medicineIdx;   //품목일련변호
    private final String medicineName;//품목명

    public VoiceResultInfo(Long medicineIdx, String medicineName) {
        this.medicineIdx = medicineIdx;
        this.medicineName = medicineName;
    }

    public Long getMedicineIdx() {
        return medicineIdx;
    }

    public String getMedicineName() {
        return medicineName;
    }
}
