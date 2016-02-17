package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.executor.JobIOThread;
import ru.linachan.nemesis.layout.Builder;
import ru.linachan.nemesis.layout.Job;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SimpleBuilder {

    private Builder builder;
    private Job job;

    private File workingDirectory;
    private Map<String, String> environment = new HashMap<>();

    private JobIOThread jobIOThread;

    private boolean running;
    private boolean started;
    private int exitCode;

    public SimpleBuilder(Job job, Builder builder, File workingDirectory) {
        this.job = job;
        this.builder = builder;
        this.workingDirectory = workingDirectory;
    }

    protected Job getJob() {
        return job;
    }

    protected Builder getBuilder() {
        return builder;
    }

    protected ProcessBuilder getProcessBuilder(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);

        processBuilder.environment().clear();
        processBuilder.environment().put("WORKSPACE", workingDirectory.getAbsolutePath());

        if (job.env != null) {
            processBuilder.environment().putAll(job.env);
        }

        processBuilder.environment().putAll(environment);
        processBuilder.directory(workingDirectory);

        return processBuilder;
    }

    public void setEnvironment(Map<String, String> newEnvironment) {
        environment.putAll(newEnvironment);
    }

    public Integer execute() throws InterruptedException, IOException {
        preBuild();

        Process process = build().start();
        InputStream processOutput = process.getInputStream();

        running = true;
        started = true;

        jobIOThread = new JobIOThread(this, processOutput);
        new Thread(jobIOThread).start();

        process.waitFor();

        exitCode = process.exitValue();

        process.destroy();

        postBuild();

        return exitCode;
    }

    public boolean isRunning() {
        return running || !started;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    protected abstract void postBuild();

    protected abstract ProcessBuilder build();

    protected abstract void preBuild() throws IOException;

    public List<String> getOutput() {
        return jobIOThread.getOutput();
    }
}
