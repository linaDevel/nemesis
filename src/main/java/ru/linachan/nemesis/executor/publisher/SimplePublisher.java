package ru.linachan.nemesis.executor.publisher;

import ru.linachan.nemesis.executor.JobExecutor;
import ru.linachan.nemesis.executor.JobPublisher;
import ru.linachan.nemesis.layout.Job;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class SimplePublisher implements JobPublisher {

    protected JobExecutor executor;
    protected Job job;
    protected Map<String, Object> publisher;
    protected File workingDirectory;
    protected Map<String, String> environment = new HashMap<>();

    @Override
    public void setUp(JobExecutor executor, Job job, Map<String, Object> publisher, File workingDirectory) {
        this.executor = executor;
        this.job = job;
        this.publisher = publisher;
        this.workingDirectory = workingDirectory;
    }

    @Override
    public abstract void publish() throws InterruptedException, IOException;

    @Override
    public void setEnvironment(Map<String, String> newEnvironment) {
        this.environment.putAll(environment);
    }

    @Override
    public void setEnvironment(String key, String value) {
        this.environment.put(key, value);
    }
}
