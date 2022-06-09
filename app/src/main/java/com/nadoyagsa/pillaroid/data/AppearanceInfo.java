package com.nadoyagsa.pillaroid.data;

public class AppearanceInfo {
    private String appearance;          // 성상
    private String formulation;         // 제형
    private String shape;               // 모양
    private String color;               // 색상
    private String dividingLine;        // 분할선
    private String identificationMark;  // 식별표기

    public AppearanceInfo(String appearance, String formulation, String shape, String color, String dividingLine, String identificationMark) {
        this.appearance = appearance;
        this.formulation = formulation;
        this.shape = shape;
        this.color = color;
        this.dividingLine = dividingLine;
        this.identificationMark = identificationMark;
    }

    public String getAppearance() {
        return appearance;
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
}
