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

            executor.execute();

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

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            String jobsResult = "";
            boolean isSuccess = true;

            for (JobExecutor executor: jobExecutors) {
                boolean jobSuccess = (executor.isSuccess());
                jobsResult += String.format(
                    "* %s %s : %s\n",
                    executor.getJob().name,
                    Utils.getJobLogURL(executor.getJob().name, event),
                    (jobSuccess ? "SUCCESS" : "FAILURE") + (executor.getJob().voting ? "" : " (non-voting)")
                );

                System.out.println(String.format(
                    "%s::%s : %s%s",
                    pipeLine.name, executor.getJob().name,
                    jobSuccess ? "SUCCESS" : "FAILURE", executor.getJob().voting ? "" : " (non-voting)"
                ));

                try {
                    File logFile = new File(executor.getLogDir(), "console.log");
                    FileWriter logFileWriter = new FileWriter(logFile);
                    for (String logLine: executor.getOutput()) {
                        logFileWriter.write(String.format("%s\r\n", logLine));
                    }
                    logFileWriter.flush();
                    logFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                isSuccess = !(isSuccess && executor.getJob().voting) || jobSuccess;
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
