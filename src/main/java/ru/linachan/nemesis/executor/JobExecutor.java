package ru.linachan.nemesis.executor;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.NemesisCore;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JobExecutor implements Runnable {

    private NemesisCore service;
    private Job job;

    private Map<String, String> environment = new HashMap<>();

    private File logDir;
    private File workingDirectory;

    private boolean running = true;
    private boolean success = true;

    private Long startTime;
    private FileWriter logFileWriter;

    private static Logger logger = LoggerFactory.getLogger(JobExecutor.class);

    public JobExecutor(NemesisCore serviceObject, Job jobDefinition) {
        service = serviceObject;
        job = jobDefinition;
    }

    private JobBuilder getBuilder(String builderName) throws IllegalAccessException, InstantiationException {
        for (Class<? extends JobBuilder> jobBuilderClass: service.getDiscoveryHelper().getSubTypesOf(JobBuilder.class)) {
            if (jobBuilderClass.getSimpleName().equals(builderName)) {
                return jobBuilderClass.newInstance();
            }
        }

        return null;
    }

    private JobPublisher getPublisher(String publisherName) throws IllegalAccessException, InstantiationException {
        for (Class<? extends JobPublisher> jobPublisherClass: service.getDiscoveryHelper().getSubTypesOf(JobPublisher.class)) {
            if (jobPublisherClass.getSimpleName().equals(publisherName)) {
                return jobPublisherClass.newInstance();
            }
        }

        return null;
    }

    @Override
    public void run() {
        try {
            File logFile = new File(logDir, "console.log");
            logFileWriter = new FileWriter(logFile);

            startTime = System.currentTimeMillis();
            workingDirectory = Utils.createTempDirectory(job.name + "-wd");

            putLine("INFO[GIT]: Cloning repository %s", environment.get("NEMESIS_PROJECT"));

            try (Git repo = Git.cloneRepository()
                .setURI(String.format(
                    "%s/%s",
                    environment.get("NEMESIS_URL"),
                    environment.get("NEMESIS_PROJECT")
                ))
                .setBranch(environment.get("NEMESIS_BRANCH"))
                .setDirectory(new File(workingDirectory, "source"))
                .call()
            ) {

                repo.fetch().setRefSpecs(new RefSpec(environment.get("NEMESIS_REF"))).call();
                repo.checkout().setName("FETCH_HEAD").call();

                putLine(
                    "INFO[GIT]: Checking out %s",
                    repo.log().setMaxCount(1).call().iterator().next().getName()
                );

            } catch (GitAPIException e) {
                logger.error("Unable to prepare Git repository: {}", e.getMessage());
            }

            for (String builder: job.builders.keySet()) {
                JobBuilder jobBuilder = getBuilder(builder);

                if (jobBuilder != null) {
                    jobBuilder.setUp(this, job, job.builders.get(builder), workingDirectory);
                    jobBuilder.setEnvironment(environment);

                    putLine("INFO[%s]: Starting builder", builder);

                    int exitCode = jobBuilder.execute();

                    putLine("INFO[%s]: Builder exited with code %d", builder, exitCode);

                    success = success && (exitCode == 0);

                    if (!success) {
                        putLine("ERROR[%s]: Interrupting build", builder);
                        break;
                    }
                } else {
                    putLine("ERROR[%s]: Builder not found", builder);
                    success = false;
                    break;
                }
            }

            for (String publisher: job.publishers.keySet()) {
                JobPublisher jobPublisher = getPublisher(publisher);

                if (jobPublisher != null) {
                    jobPublisher.setUp(this, job, job.publishers.get(publisher), workingDirectory);
                    jobPublisher.setEnvironment(environment);

                    jobPublisher.publish();
                } else {
                    putLine("ERROR[%s]: Publisher not found", publisher);
                }
            }

            File artifactsDir = new File(workingDirectory, "artifacts");
            if (artifactsDir.exists()) {
                putLine("INFO[POST_BUILD]: Copying artifacts");
                FileUtils.copyDirectory(artifactsDir, new File(logDir, "artifacts"));
            }

            Long stopTime = System.currentTimeMillis();
            putLine("INFO[TIME]: Job completed in %5.3f", (stopTime - startTime) / 1000.0);

            logFileWriter.flush();
            logFileWriter.close();
        } catch (InterruptedException | IOException | InstantiationException | IllegalAccessException e) {
            try {
                putLine("ERROR[%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            success = false;
        } finally {
            workingDirectory.delete();
            running = false;
        }
    }

    public Job getJob() {
        return job;
    }

    public NemesisCore getService() {
        return service;
    }

    public void setEventData(Event event) {
        if ((event.getPatchSet() != null)&&(event.getChangeRequest() != null)) {
            environment.put("NEMESIS_URL", NemesisConfig.getGerritURL());
            environment.put("NEMESIS_PROJECT", event.getChangeRequest().getProject());
            environment.put("NEMESIS_BRANCH", event.getChangeRequest().getBranch());
            environment.put("NEMESIS_REF", event.getPatchSet().getRef());

            environment.put("NEMESIS_PATCHSET_ID", String.valueOf(event.getPatchSet().getPatchSetNumber()));
            environment.put("NEMESIS_CHANGE_ID", String.valueOf(event.getChangeRequest().getChangeNumber()));
        }
    }

    public void setLogDir(File jobLogDir) {
        logDir = jobLogDir;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isSuccess() {
        return success;
    }

    public void putLine(String line, Object... args) throws IOException {
        logFileWriter.write(String.format(
            " [%10.3f] %s\n",
            (System.currentTimeMillis() - startTime) / 1000.0,
            String.format(line, args)
        ));
        logFileWriter.flush();
    }
}
