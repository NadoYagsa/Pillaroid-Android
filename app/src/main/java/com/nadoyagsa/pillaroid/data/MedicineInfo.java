package com.nadoyagsa.pillaroid.data;

public class MedicineInfo {
    private final Long medicineIdx;   //품목일련변호
    private final String medicineName;//품목명
    private final String efficacy;    //효능효과
    private final String usage;       //용법용량
    private final String precautions; //주의사항
    private final String storageMethod;   //보관법

    public MedicineInfo(Long medicineIdx, String medicineName, String efficacy, String usage, String precautions, String storageMethod) {
        this.medicineIdx = medicineIdx;
        this.medicineName = medicineName;
        this.efficacy = efficacy;
        this.usage = usage;
        this.precautions = precautions;
        this.storageMethod = storageMethod;
    }

    public Long getMedicineIdx() {
        return medicineIdx;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getEfficacy() {
        return efficacy;
    }

    public String getUsage() {
        return usage;
    }

    public String getPrecautions() {
        return precautions;
    }

    public String getStorageMethod() {
        return storageMethod;
    }
}
