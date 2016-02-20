package ru.linachan.nemesis.executor;

import ru.linachan.nemesis.executor.builder.SimpleBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JobIOThread implements Runnable {

    private SimpleBuilder process;
    private InputStream stream;
    private String prefix;

    public JobIOThread(SimpleBuilder process, InputStream stream, String prefix) {
        this.process = process;
        this.stream = stream;
        this.prefix = prefix;
    }

    @Override
    public void run() {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            while (process.isRunning()) {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        putLine("%s: %s", prefix, line);
                    }
                } catch (final IOException ignored) {}
            }
            reader.close();
        } catch (final IOException ignored) {}
    }

    public void putLine(String line, String... args) throws IOException {
        process.putLine(line, args);
    }
}
