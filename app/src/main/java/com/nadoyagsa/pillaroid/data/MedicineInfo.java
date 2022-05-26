package com.nadoyagsa.pillaroid.data;

public class MedicineInfo {
    private final Long medicineIdx;         //의약품 idx
    private final Long code;                //품목일련번호
    private final String medicineName;      //품목명
    private final String efficacy;          //효능효과
    private final String usage;             //용법용량
    private final String precautions;       //주의사항
    private final String appearance;        //외형
    private final String ingredient;        //성분
    private final String save;              //보관법

    public MedicineInfo(Long medicineIdx, Long code, String medicineName, String efficacy, String usage, String precautions, String appearance, String ingredient, String save) {
        this.medicineIdx = medicineIdx;
        this.code = code;
        this.medicineName = medicineName;
        this.efficacy = efficacy;
        this.usage = usage;
        this.precautions = precautions;
        this.appearance = appearance;
        this.ingredient = ingredient;
        this.save = save;
    }

    public Long getMedicineIdx() {
        return medicineIdx;
    }

    public Long getCode() {
        return code;
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

    public String getAppearance() {
        return appearance;
    }

    public String getIngredient() {
        return ingredient;
    }

    public String getSave() {
        return save;
    }
}
