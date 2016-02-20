package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.executor.JobExecutor;
import ru.linachan.nemesis.layout.Builder;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class ScriptBuilder extends SimpleBuilder {

    protected File jobScript;

    public ScriptBuilder(JobExecutor executor, Job job, Builder builder, File workingDirectory) {
        super(executor, job, builder, workingDirectory);
    }

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        jobScriptWriter.write((String) getBuilder().params.get("script"));
        jobScriptWriter.flush();
        jobScriptWriter.close();
    }

    @Override
    protected abstract ProcessBuilder build();

    @Override
    protected void postBuild() {
        jobScript.delete();
    }
}
