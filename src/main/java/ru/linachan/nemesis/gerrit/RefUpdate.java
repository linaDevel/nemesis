package ru.linachan.nemesis.gerrit;

import org.json.simple.JSONObject;

public class RefUpdate {

    private String oldRef;
    private String newRef;
    private String refName;
    private String project;

    public RefUpdate(JSONObject refUpdateData) {
        oldRef = (String) refUpdateData.get("oldRef");
        newRef = (String) refUpdateData.get("newRef");
        refName = (String) refUpdateData.get("refName");
        project = (String) refUpdateData.get("project");
    }

    public String getOldRef() {
        return oldRef;
    }

    public String getNewRef() {
        return newRef;
    }

    public String getRefName() {
        return refName;
    }

    public String getProject() {
        return project;
    }
}
