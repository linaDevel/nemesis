package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.layout.Builder;
import ru.linachan.nemesis.layout.Job;

import java.io.File;

public class ShellBuilder extends ScriptBuilder {

    public ShellBuilder(Job job, Builder builder, File workingDirectory) {
        super(job, builder, workingDirectory);
    }

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder("/bin/bash", jobScript.getPath());
    }
}
