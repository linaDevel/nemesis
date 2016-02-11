package ru.linachan.nemesis.gerrit;

import org.json.simple.JSONObject;

public class Approval {

    private ApprovalType type;
    private String description;
    private Integer value;

    public Approval(JSONObject approvalData) {
        type = ApprovalType.getApprovalType((String) approvalData.get("type"));
        description = (String) approvalData.get("description");
        value = Integer.valueOf((String) approvalData.get("value"));
    }

    public ApprovalType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Integer getValue() {
        return value;
    }

    public String toString() {
        return type.toString() + ": " + value;
    }
}