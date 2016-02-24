package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SSHPublisher extends SimpleBuilder {

    private File jobScript;

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        jobScriptWriter.write(String.format(
            "scp -i ${SSH_KEY} ${WORKSPACE}/artifacts/%s %s:%s\n",
            getBuilder().params.get("sourcePath"),
            getBuilder().params.get("targetHost"),
            getBuilder().params.get("targetPath")
        ));

        jobScriptWriter.flush();
        jobScriptWriter.close();
    }

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder("/bin/bash", "-xe", jobScript.getPath());
    }

    @Override
    protected void postBuild() {
        jobScript.delete();
    }
}
