package com.nadoyagsa.pillaroid.data;

public class PrescriptionInfo {
    private String medicineName;
    private boolean isFavorites = false;
    private String pillShape = null;
    private String pillDivision = null;
    private String pillFormulation = null;
    private String classification;
    private String efficacy1;   //주효능·효과
    private String efficacy2;   //약품이 사용될 수 있는 질환

    public PrescriptionInfo(String medicineName, boolean isFavorites, String classification, String efficacy1, String efficacy2) {
        this.medicineName = medicineName;
        this.isFavorites = isFavorites;
        this.classification = classification;
        this.efficacy1 = efficacy1;
        this.efficacy2 = efficacy2;
    }

    public PrescriptionInfo(String medicineName, boolean isFavorites, String pillShape, String pillDivision, String pillFormulation, String classification, String efficacy1, String efficacy2) {
        this.medicineName = medicineName;
        this.isFavorites = isFavorites;
        this.pillShape = pillShape;
        this.pillDivision = pillDivision;
        this.pillFormulation = pillFormulation;
        this.classification = classification;
        this.efficacy1 = efficacy1;
        this.efficacy2 = efficacy2;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public boolean getIsFavorites() {
        return isFavorites;
    }

    public String getPillShape() {
        return pillShape;
    }

    public String getPillDivision() {
        return pillDivision;
    }

    public String getPillFormulation() {
        return pillFormulation;
    }

    public String getClassification() {
        return classification;
    }

    public String getEfficacy1() {
        return efficacy1;
    }

    public String getEfficacy2() {
        return efficacy2;
    }
}
