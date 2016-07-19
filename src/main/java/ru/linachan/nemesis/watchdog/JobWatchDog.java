package ru.linachan.nemesis.watchdog;

import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.utils.FileWatchDog;
import ru.linachan.nemesis.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;

public class JobWatchDog extends FileWatchDog {

    private File jobsDir;
    private Map<String, Job> jobData;

    private static Logger logger = LoggerFactory.getLogger(JobWatchDog.class);

    public JobWatchDog() throws IOException {
        super(NemesisConfig.getPath("jobs").toFile());
        jobsDir = NemesisConfig.getPath("jobs").toFile();

        checkJobFiles();
    }

    @Override
    protected void onCreate(WatchEvent event) throws IOException {
        checkJobFiles();
    }

    @Override
    protected void onModify(WatchEvent event) throws IOException {
        checkJobFiles();
    }

    @Override
    protected void onDelete(WatchEvent event) throws IOException {
        checkJobFiles();
    }

    public void checkJobFiles() throws FileNotFoundException {
        logger.info("Reading Nemesis job definition...");

        try {
            jobData = Utils.readJobConfiguration(jobsDir);
        } catch (Exception e) {
            logger.error("Unable to read job configuration: {}", e.getMessage());
        }
    }

    public Job getJob(String name) {
        if (name.equals("noop")) {
            Job job = new Job();

            job.name = "noop";
            job.builders = new HashMap<>();

            return job;
        }

        return jobData.containsKey(name) ? jobData.get(name) : null;
    }
}
