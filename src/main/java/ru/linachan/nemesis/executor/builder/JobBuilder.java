package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.executor.JobExecutor;
import ru.linachan.nemesis.layout.Job;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface JobBuilder {

    void setUp(JobExecutor executor, Job job, ru.linachan.nemesis.layout.Builder builder, File workingDirectory);

    Integer execute() throws InterruptedException, IOException;

    void setEnvironment(Map<String, String> newEnvironment);
    void setEnvironment(String key, String value);

    boolean isRunning();
}
