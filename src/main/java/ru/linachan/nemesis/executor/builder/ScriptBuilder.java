package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class ScriptBuilder extends SimpleBuilder {

    protected File jobScript;

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        jobScriptWriter.write((String) getBuilder().get("script"));
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
