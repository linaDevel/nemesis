package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.executor.JobExecutor;
import ru.linachan.nemesis.layout.Builder;
import ru.linachan.nemesis.layout.Job;

import java.io.File;
import java.io.IOException;

public class NoopBuilder extends SimpleBuilder {

    public NoopBuilder(JobExecutor executor, Job job, Builder builder, File workingDirectory) {
        super(executor, job, builder, workingDirectory);
    }

    @Override
    protected void preBuild() throws IOException {

    }

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder("ls");
    }

    @Override
    protected void postBuild() {

    }
}
