package ru.linachan.nemesis.layout;

import java.util.Map;

public class Job {

    public String name;
    public Map<String, String> env;
    public boolean voting = true;
    public Map<String, Map<String, Object>> builders;
    public Map<String, Map<String, Object>> publishers;

}
