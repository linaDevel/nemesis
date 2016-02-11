package ru.linachan.nemesis.layout;

import ru.linachan.nemesis.gerrit.EventType;

import java.util.List;

public class Trigger {

    public EventType event;
    public String commentFilter;
    public List<Score> approvals;
    public String ref;

}
