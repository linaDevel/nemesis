package ru.linachan.nemesis.executor;

import ru.linachan.nemesis.layout.Job;

import java.io.IOException;
import java.util.Map;

public interface JobPublisher {

    void publish(JobExecutor executor, Job job, Map<String, Object> publisher, Map<String, String> environment) throws InterruptedException, IOException;

}
