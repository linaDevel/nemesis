package ru.linachan.nemesis.executor;

import org.apache.commons.io.FileUtils;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobExecutor implements Runnable {

    private Job job;

    private Integer exitCode;

    private JobIOThread jobIOThread = null;

    private boolean started = false;
    private boolean running = false;

    private Map<String, String> environment = new HashMap<>();

    private File logDir;

    public JobExecutor(Job jobDefinition) {
        job = jobDefinition;
    }

    public void execute() {
        Thread executionThread = new Thread(this);
        executionThread.start();
    }

    public void noop() {
        started = true;
        running = false;
        exitCode = 0;
    }

    @Override
    public void run() {
        try {
            File jobScript = Utils.createTempFile(job.name);
            jobScript.setExecutable(true);

            FileWriter jobScriptWriter = new FileWriter(jobScript);

            jobScriptWriter.write(job.shell);
            jobScriptWriter.flush();
            jobScriptWriter.close();

            ProcessBuilder processBuilder = new ProcessBuilder(jobScript.getPath());
            File tmpWorkingDirectory = Utils.createTempDirectory(job.name + "-wd");

            processBuilder.environment().clear();
            processBuilder.environment().put("WORKSPACE", tmpWorkingDirectory.getAbsolutePath());

            if (job.env != null) {
                processBuilder.environment().putAll(job.env);
            }

            processBuilder.environment().putAll(environment);

            Process process = processBuilder.directory(tmpWorkingDirectory).start();

            started = true;
            running = true;

            InputStream processOutput = process.getInputStream();

            jobIOThread = new JobIOThread(this, processOutput);
            new Thread(jobIOThread).start();

            process.waitFor();

            exitCode = process.exitValue();

            process.destroy();

            File artifactsDir = new File(tmpWorkingDirectory, "artifacts");
            if (artifactsDir.exists()) {
                FileUtils.copyDirectory(artifactsDir, new File(logDir, "artifacts"));
            }

            jobScript.delete();
            tmpWorkingDirectory.delete();
        } catch(InterruptedException | IOException e) {
            jobIOThread.putLine("ERROR[%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            exitCode = 1;
        } finally {
            running = false;
        }
    }

    public boolean isRunning() {
        return running || !started;
    }

    public String getJob() {
        return job.name;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public List<String> getProcessOutput() {
        return (jobIOThread != null) ? jobIOThread.getOutput() : new ArrayList<>();
    }

    public boolean getVoting() {
        return job.voting;
    }

    public void setEventData(Event event) {
        if ((event.getPatchSet() != null)&&(event.getChangeRequest() != null)) {
            environment.put("NEMESIS_URL", NemesisConfig.getGerritURL());
            environment.put("NEMESIS_PROJECT", event.getChangeRequest().getProject());
            environment.put("NEMESIS_BRANCH", event.getChangeRequest().getBranch());
            environment.put("NEMESIS_REF", event.getPatchSet().getRef());
        }
    }

    public void setLogDir(File jobLogDir) {
        logDir = jobLogDir;
    }

    public File getLogDir() {
        return logDir;
    }
}
