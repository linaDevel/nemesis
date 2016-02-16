package ru.linachan.nemesis.executor;

import ru.linachan.nemesis.NemesisCore;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.layout.PipeLine;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PipeLineExecutor implements Runnable {

    private NemesisCore service;

    private List<Job> jobs;
    private PipeLine pipeLine;
    private Event event;

    public PipeLineExecutor(NemesisCore serviceInstance, List<Job> jobList, PipeLine pipeLineData, Event eventData) {
        service = serviceInstance;
        pipeLine = pipeLineData;
        jobs = jobList;
        event = eventData;
    }

    public void execute() {
        Thread executionThread = new Thread(this);
        executionThread.start();
    }

    @Override
    public void run() {
        List<JobExecutor> jobExecutors = new ArrayList<>();

        jobs.stream().filter(job -> job != null).forEach(job -> {
            JobExecutor executor = new JobExecutor(job);
            executor.setEventData(event);

            try {
                executor.setLogDir(Utils.createJobLogDirectory(job.name, event));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (job.name.equals("noop")) {
                executor.noop();
            } else {
                executor.execute();
            }

            jobExecutors.add(executor);
        });

        if (jobExecutors.size() > 0) {
            boolean isExecuting = true;

            try {
                service.review(
                    event.getChangeRequest(), event.getPatchSet(),
                    String.format("Starting %s jobs", pipeLine.name),
                    pipeLine.onStart
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (isExecuting) {
                isExecuting = jobExecutors.stream()
                    .filter(JobExecutor::isRunning)
                    .findFirst().isPresent();
            }

            String jobsResult = "";
            boolean isSuccess = true;

            for (JobExecutor executor: jobExecutors) {
                boolean jobSuccess = (executor.getExitCode() == 0);
                jobsResult += String.format(
                    "* %s %s : %s\n",
                    executor.getJob(),
                    Utils.getJobLogURL(executor.getJob(), event),
                    (jobSuccess ? "SUCCESS" : "FAILURE") + (executor.getVoting() ? "" : " (non-voting)")
                );

                System.out.println(String.format(
                    "%s::%s : %s%s",
                    pipeLine.name, executor.getJob(),
                    jobSuccess ? "SUCCESS" : "FAILURE", executor.getVoting() ? "" : " (non-voting)"
                ));

                try {
                    File logFile = new File(executor.getLogDir(), "console.log");
                    FileWriter logFileWriter = new FileWriter(logFile);
                    for (String logLine: executor.getProcessOutput()) {
                        logFileWriter.write(String.format("%s\r\n", logLine));
                    }
                    logFileWriter.flush();
                    logFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                isSuccess = !(isSuccess && executor.getVoting()) || jobSuccess;
            }

            String message = String.format(
                "Build %s.\n\n%s", isSuccess ? "successful" : "failed", jobsResult
            );

            try {
                service.review(
                    event.getChangeRequest(), event.getPatchSet(),
                    message, isSuccess ? pipeLine.onSuccess : pipeLine.onFailure
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
