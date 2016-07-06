package ru.linachan.nemesis.gerrit;

public enum EventType {
    CHANGE_ABANDONED("change-abandoned"),
    CHANGE_MERGED("change-merged"),
    CHANGE_RESTORED("change-restored"),
    COMMENT_ADDED("comment-added"),
    DRAFT_PUBLISHED("draft-published"),
    DROPPED_OUTPUT("dropped-output"),
    HASHTAGS_CHANGED("hashtags-changed"),
    MERGE_FAILED("merge-failed"),
    PATCHSET_CREATED("patchset-created"),
    PROJECT_CREATED("project-created"),
    REVIEWER_ADDED("reviewer-added"),
    REF_UPDATED("ref-updated"),
    TOPIC_CHANGED("topic-changed"),
    REF_REPLICATED("ref-replicated"),
    REF_REPLICATION_DONE("ref-replication-done");

    private String eventType;

    EventType(String eventType) {
        this.eventType = eventType;
    }

    public static EventType getEventType(String eventType) {
        return EventType.valueOf(eventType.toUpperCase().replace("-", "_"));
    }

    public String getEventType() {
        return eventType;
    }
}