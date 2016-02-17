package ru.linachan.nemesis.layout;

import java.util.List;
import java.util.Map;

public class Job {

    public String name;
    public Map<String, String> env;
    public boolean voting = true;
    public List<Builder> builders;
}
