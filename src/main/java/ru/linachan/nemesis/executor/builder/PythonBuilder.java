package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.executor.JobExecutor;
import ru.linachan.nemesis.layout.Builder;
import ru.linachan.nemesis.layout.Job;

import java.io.File;

public class PythonBuilder extends ScriptBuilder {

    public PythonBuilder(JobExecutor executor, Job job, Builder builder, File workingDirectory) {
        super(executor, job, builder, workingDirectory);
    }

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder("/usr/bin/python", jobScript.getPath());
    }
}
