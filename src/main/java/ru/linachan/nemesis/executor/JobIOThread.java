package ru.linachan.nemesis.executor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JobIOThread implements Runnable {

    private JobExecutor process;
    private InputStream outputStream;
    private List<String> outputLog;

    public JobIOThread(JobExecutor process, InputStream outputStream) {
        this.process = process;
        this.outputStream = outputStream;
        this.outputLog = new ArrayList<>();
    }

    @Override
    public void run() {
        while (process.isRunning()) {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(outputStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    outputLog.add(line);
                }

                reader.close();
            } catch (final Exception e) {
                // Ignore exception
            }
        }
    }

    public List<String> getOutput() {
        return outputLog;
    }

    public void putLine(String line, String... args) {
        outputLog.add(String.format(line, (Object) args));
    }
}
