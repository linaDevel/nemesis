package ru.linachan.nemesis.gerrit;

import org.json.simple.JSONObject;

public class ChangeRequest {

    private Integer changeNumber;
    private String changeId;
    private String subject;
    private String project;
    private String branch;
    private String topic;
    private String commitMessage;
    private String status;
    private String url;
    private Author author;

    public ChangeRequest(JSONObject changeRequestData) {
        JSONObject authorData = (JSONObject) changeRequestData.get("owner");

        changeNumber = Integer.valueOf((String) changeRequestData.get("number"));
        changeId = (String) changeRequestData.get("id");
        subject = (String) changeRequestData.get("subject");
        project = (String) changeRequestData.get("project");
        branch = (String) changeRequestData.get("branch");
        topic = (String) changeRequestData.get("topic");
        commitMessage = (String) changeRequestData.get("commitMessage");
        status = (String) changeRequestData.get("status");
        url = (String) changeRequestData.get("url");
        author = (authorData != null) ? new Author(authorData) : null;
    }

    public Integer getChangeNumber() {
        return changeNumber;
    }

    public String getChangeId() {
        return changeId;
    }

    public String getSubject() {
        return subject;
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public String getTopic() {
        return topic;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public String getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }

    public Author getAuthor() {
        return author;
    }
}