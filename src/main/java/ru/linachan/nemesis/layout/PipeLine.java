package ru.linachan.nemesis.layout;

import java.util.List;

public class PipeLine {

    public String name;
    public List<Trigger> triggers;

    public List<Score> onStart;
    public List<Score> onSuccess;
    public List<Score> onFailure;
}
