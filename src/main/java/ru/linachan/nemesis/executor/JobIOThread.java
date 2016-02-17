package ru.linachan.nemesis.executor;

import ru.linachan.nemesis.executor.builder.SimpleBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JobIOThread implements Runnable {

    private SimpleBuilder process;
    private InputStream outputStream;
    private List<String> outputLog;

    public JobIOThread(SimpleBuilder process, InputStream outputStream) {
        this.process = process;
        this.outputStream = outputStream;
        this.outputLog = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(outputStream));
            while (process.isRunning()) {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputLog.add(line);
                    }
                } catch (final IOException ignored) {}
            }
            reader.close();
        } catch (final IOException ignored) {}
    }

    public List<String> getOutput() {
        return outputLog;
    }

    public void putLine(String line, String... args) {
        outputLog.add(String.format(line, (Object) args));
    }
}
