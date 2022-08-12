package com.nadoyagsa.pillaroid.data;

public class AppearanceInfo {
    private String feature;             // 성상
    private String formulation;         // 제형
    private String shape;               // 모양
    private String color;               // 색상
    private String dividingLine;        // 분할선
    private String identificationMark;  // 식별표기

    private boolean isNull = false;

    public AppearanceInfo(String feature, String formulation, String shape, String color, String dividingLine, String identificationMark) {
        this.feature = feature;
        this.formulation = formulation;
        this.shape = shape;
        this.color = color;
        this.dividingLine = dividingLine;
        this.identificationMark = identificationMark;
    }

    public String getFeature() {
        return feature;
    }

    public String getFormulation() {
        return formulation;
    }

    public String getShape() {
        return shape;
    }

    public String getColor() {
        return color;
    }

    public String getDividingLine() {
        return dividingLine;
    }

    public String getIdentificationMark() {
        return identificationMark;
    }

    public void setIsNull(boolean isNull) {
        this.isNull = isNull;
    }
    public boolean isNull() {
        return isNull;
    }
}
