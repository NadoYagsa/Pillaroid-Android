package com.nadoyagsa.pillaroid.data;

import org.json.JSONException;
import org.json.JSONObject;

public class AlarmInfo {
    private final Long alarmIdx;
    private final int medicineIdx;
    private final String name;
    private final int period;
    private final String dosage;

    public AlarmInfo(JSONObject alarmJson) throws JSONException {
        this.alarmIdx = alarmJson.getLong("alarmIdx");
        this.medicineIdx = alarmJson.getInt("medicineIdx");
        this.name = alarmJson.getString("name");
        this.period = alarmJson.getInt("period");
        this.dosage = alarmJson.getString("dosage");
    }

    public Long getAlarmIdx() { return alarmIdx; }

    public int getMedicineIdx() { return medicineIdx; }

    public String getName() { return name; }

    public int getPeriod() { return period; }

    public String getDosage() { return dosage; }
}
