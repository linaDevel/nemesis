package ru.linachan.nemesis.layout;

import java.util.HashMap;
import java.util.Map;

public class Job {

    public String name;
    public Map<String, String> env;
    public boolean voting = true;
    public Map<String, Map<String, Object>> builders = new HashMap<>();
    public Map<String, Map<String, Object>> publishers = new HashMap<>();

}
