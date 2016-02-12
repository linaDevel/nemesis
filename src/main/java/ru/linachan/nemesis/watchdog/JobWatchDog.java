package ru.linachan.nemesis.watchdog;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.layout.JobDefinition;
import ru.linachan.nemesis.utils.FileWatchDog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;

public class JobWatchDog extends FileWatchDog {

    private File jobsDir;
    private Map<String, Job> jobData;

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

    private void checkJobFiles() throws FileNotFoundException {
        System.out.println("Reading Nemesis job definition...");
        jobData = new HashMap<>();

        Yaml jobParser = new Yaml(new Constructor(JobDefinition.class));

        for (File jobFile: jobsDir.listFiles()) {
            if (!jobFile.isDirectory()) {
                JobDefinition jobDefinition = (JobDefinition) jobParser.load(new FileReader(jobFile));
                for (Job job: jobDefinition.jobs) {
                    jobData.put(job.name, job);
                }
            }
        }
    }

    public Job getJob(String name) {
        return jobData.containsKey(name) ? jobData.get(name) : null;
    }
}
