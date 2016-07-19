package ru.linachan.nemesis.gerrit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.linachan.nemesis.NemesisCore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class Event {

    private EventType eventType;
    private ChangeRequest changeRequest = null;
    private PatchSet patchSet = null;
    private Author author = null;
    private String comment;
    private Long eventTime;
    private List<Approval> approvals;

    private NemesisCore serviceCore;

    private Map customAttributes = new HashMap<>();

    private static Logger logger = LoggerFactory.getLogger(Event.class);

    public Event(String eventData, NemesisCore service) {
        serviceCore = service;

        try {
            JSONObject eventObject = (JSONObject) new JSONParser().parse(eventData);

            eventType = EventType.getEventType((String) eventObject.get("type"));

            if (eventObject.containsKey("change")) {
                changeRequest = new ChangeRequest((JSONObject) eventObject.get("change"));
            }

            if (eventObject.containsKey("patchSet")) {
                patchSet = new PatchSet((JSONObject) eventObject.get("patchSet"));
            }

            if (eventObject.containsKey("eventCreatedOn")) {
                eventTime = (Long) eventObject.get("eventCreatedOn");
            }

            switch(eventType) {
                case CHANGE_ABANDONED:
                    author = new Author((JSONObject) eventObject.get("abandoner"));
                    comment = (String) eventObject.get("reason");
                    break;
                case CHANGE_MERGED:
                    author = new Author((JSONObject) eventObject.get("submitter"));
                    comment = (String) eventObject.get("newRev");
                    customAttributes.put("newRev", eventObject.get("newRev"));
                    break;
                case CHANGE_RESTORED:
                    author = new Author((JSONObject) eventObject.get("restorer"));
                    comment = (String) eventObject.get("reason");
                    break;
                case COMMENT_ADDED:
                    author = new Author((JSONObject) eventObject.get("author"));
                    comment = (String) eventObject.get("comment");
                    approvals = new ArrayList<>();

                    try {
                        JSONObject changeRequestData = serviceCore.query(changeRequest.getChangeId());
                        JSONObject currentPatchSetData = (JSONObject) changeRequestData.get("currentPatchSet");
                        JSONArray presentApprovalsData = (JSONArray) currentPatchSetData.get("approvals");

                        if (presentApprovalsData != null) {
                            approvals.addAll(
                                (Collection<? extends Approval>) presentApprovalsData.stream()
                                    .map(approval -> new Approval((JSONObject) approval))
                                    .collect(Collectors.toList())
                            );
                        }
                    } catch (IOException e) {
                        logger.error("Unable to handle change approvals: {}", e.getMessage());
                    }
                    break;
                case DRAFT_PUBLISHED:
                    author = new Author((JSONObject) eventObject.get("uploader"));
                    break;
                case HASHTAGS_CHANGED:
                    author = new Author((JSONObject) eventObject.get("editor"));

                    customAttributes.put("hashTags", eventObject.get("hashtags"));
                    customAttributes.put("hashTagsAdded", eventObject.get("added"));
                    customAttributes.put("hashTagsRemoved", eventObject.get("removed"));
                    break;
                case MERGE_FAILED:
                    author = new Author((JSONObject) eventObject.get("submitter"));
                    comment = (String) eventObject.get("reason");
                    break;
                case PATCHSET_CREATED:
                    author = new Author((JSONObject) eventObject.get("uploader"));
                    break;
                case PROJECT_CREATED:
                    customAttributes.put("projectName", eventObject.get("projectName"));
                    customAttributes.put("projectHead", eventObject.get("projectHead"));
                    break;
                case REF_UPDATED:
                    JSONObject authorData = (JSONObject) eventObject.get("submitter");

                    author = new Author((authorData != null) ? authorData : new JSONObject());
                    customAttributes.put("refUpdate", new RefUpdate((JSONObject) eventObject.get("refUpdate")));
                    break;
                case REVIEWER_ADDED:
                    author = new Author((JSONObject) eventObject.get("reviewer"));
                    break;
                case TOPIC_CHANGED:
                    author = new Author((JSONObject) eventObject.get("changer"));
                    customAttributes.put("oldTopic", eventObject.get("oldTopic"));
                    break;
            }
        } catch (ParseException e) {
            logger.error("Unable to parse event data: {}", e.getMessage());
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public ChangeRequest getChangeRequest() {
        return changeRequest;
    }

    public PatchSet getPatchSet() {
        return patchSet;
    }

    public Author getAuthor() {
        return author;
    }

    public String getComment() {
        return comment;
    }

    public Long getEventTime() {
        return eventTime;
    }

    public List<Approval> getApprovals() {
        return approvals;
    }

    public <T> T getCustomAttribute(String attribute, Class<T> attributeType) {
        return (T) (customAttributes.containsKey(attribute) ? customAttributes.get(attribute) : null);
    }

    public String toString() {
        String representation = "";

        representation += (author != null) ? author.getName() + " " : "";
        representation += "[" + eventType + "]";
        representation += (changeRequest != null) ? " " + changeRequest.getProject() + " :: " + changeRequest.getSubject() : "";

        return representation;
    }
}
