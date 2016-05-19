package ru.linachan.nemesis.executor;

import ru.linachan.nemesis.layout.Job;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface JobPublisher {

    void setUp(JobExecutor executor, Job job, Map<String, Object> builder, File workingDirectory);

    void publish() throws InterruptedException, IOException;

    void setEnvironment(Map<String, String> newEnvironment);
    void setEnvironment(String key, String value);

}
