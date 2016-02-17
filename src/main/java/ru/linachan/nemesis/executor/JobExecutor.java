package ru.linachan.nemesis.executor;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.executor.builder.NoopBuilder;
import ru.linachan.nemesis.executor.builder.PythonBuilder;
import ru.linachan.nemesis.executor.builder.ShellBuilder;
import ru.linachan.nemesis.executor.builder.SimpleBuilder;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.layout.Builder;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobExecutor implements Runnable {

    private Job job;

    private Map<String, String> environment = new HashMap<>();

    private File logDir;
    private List<String> outputLog = new ArrayList<>();

    private boolean running = true;
    private boolean success = true;

    public JobExecutor(Job jobDefinition) {
        job = jobDefinition;
    }

    public void execute() {
        Thread executionThread = new Thread(this);
        executionThread.start();
    }

    @Override
    public void run() {
        try {
            File tmpWorkingDirectory = Utils.createTempDirectory(job.name + "-wd");

            outputLog.add(String.format("INFO[GIT]: Cloning repository %s", environment.get("NEMESIS_PROJECT")));

            try (Git repo = Git.cloneRepository()
                .setURI(String.format(
                    "%s/%s",
                    environment.get("NEMESIS_URL"),
                    environment.get("NEMESIS_PROJECT")
                ))
                .setBranch(environment.get("NEMESIS_BRANCH"))
                .setDirectory(new File(tmpWorkingDirectory, "source"))
                .call()
            ) {

                repo.fetch().setRefSpecs(new RefSpec(environment.get("NEMESIS_REF"))).call();
                repo.checkout().setName("FETCH_HEAD").call();

                outputLog.add(String.format(
                    "INFO[GIT]: Checking out %s",
                    repo.log().setMaxCount(1).call().iterator().next().getName()
                ));

            } catch (GitAPIException e) {
                e.printStackTrace();
            }

            for (Builder builder: job.builders) {
                SimpleBuilder jobBuilder;

                switch (builder.type) {
                    case SHELL:
                        jobBuilder = new ShellBuilder(job, builder, tmpWorkingDirectory);
                        break;
                    case PYTHON:
                        jobBuilder = new PythonBuilder(job, builder, tmpWorkingDirectory);
                        break;
                    case NOOP:
                    default:
                        jobBuilder = new NoopBuilder(job, builder, tmpWorkingDirectory);
                }

                outputLog.add(String.format("INFO[%s]: Starting builder", builder.type));

                jobBuilder.setEnvironment(environment);
                int exitCode = jobBuilder.execute();

                outputLog.addAll(jobBuilder.getOutput());
                outputLog.add(String.format("INFO[%s]: Builder exited with code %d", builder.type, exitCode));

                success = success && (exitCode == 0);

                if (!success) {
                    outputLog.add(String.format("ERROR[%s]: Interrupting build", builder.type));
                    break;
                }
            }

            File artifactsDir = new File(tmpWorkingDirectory, "artifacts");
            if (artifactsDir.exists()) {
                outputLog.add("INFO[POST_BUILD]: Copying artifacts");
                FileUtils.copyDirectory(artifactsDir, new File(logDir, "artifacts"));
            }

            tmpWorkingDirectory.delete();
        } catch (InterruptedException | IOException e) {
            outputLog.add(String.format("ERROR[%s]: %s", e.getClass().getSimpleName(), e.getMessage()));
        } finally {
            running = false;
        }
    }

    public Job getJob() {
        return job;
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

    public boolean isRunning() {
        return running;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getOutput() {
        return outputLog;
    }
}
