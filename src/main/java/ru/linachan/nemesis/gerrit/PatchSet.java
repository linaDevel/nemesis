package ru.linachan.nemesis.gerrit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PatchSet {

    private Integer patchSetNumber;
    private String revision;
    private String ref;
    private Author uploader;
    private Long createdOn;
    private Author author;
    private Boolean isDraft;
    private Long sizeInsertions;
    private Long sizeDeletions;
    private List<Approval> approvals;

    public PatchSet(JSONObject patchSetData) {
        JSONObject uploaderData = (JSONObject) patchSetData.get("uploader");
        JSONObject authorData = (JSONObject) patchSetData.get("author");
        JSONArray approvalsData = (JSONArray) patchSetData.get("approvals");

        patchSetNumber = Integer.valueOf((String) patchSetData.get("number"));
        revision = (String) patchSetData.get("revision");
        ref = (String) patchSetData.get("ref");
        uploader = (uploaderData != null) ? new Author(uploaderData) : null;
        createdOn = (Long) patchSetData.get("createdOn");
        author = (authorData != null) ? new Author(authorData) : null;
        isDraft = (Boolean) patchSetData.get("isDraft");
        sizeInsertions = (Long) patchSetData.get("sizeInsertions");
        sizeDeletions = (Long) patchSetData.get("sizeDeletions");

        if (approvalsData != null) {
            approvals = new ArrayList<>();
            for (Object approval : approvalsData) {
                approvals.add(new Approval((JSONObject) approval));
            }
        }
    }

    public Integer getPatchSetNumber() {
        return patchSetNumber;
    }

    public String getRevision() {
        return revision;
    }

    public String getRef() {
        return ref;
    }

    public Author getUploader() {
        return uploader;
    }

    public Long getCreatedOn() {
        return createdOn;
    }

    public Author getAuthor() {
        return author;
    }

    public Boolean getIsDraft() {
        return isDraft;
    }

    public Long getSizeInsertions() {
        return sizeInsertions;
    }

    public Long getSizeDeletions() {
        return sizeDeletions;
    }

    public List<Approval> getApprovals() {
        return approvals;
    }
}