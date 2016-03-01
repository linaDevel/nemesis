package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DockerBuilder extends SimpleBuilder {

    private File jobScript;
    private File jobShellScript;

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        jobShellScript = new File(workingDirectory, "jobScript.sh");

        FileWriter jobShellScriptWriter = new FileWriter(jobShellScript);

        jobShellScriptWriter.write((String) getBuilder().get("script"));

        jobShellScriptWriter.flush();
        jobShellScriptWriter.close();

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        jobScriptWriter.write(String.format(
            "docker pull %s\n", getBuilder().getOrDefault("image", "ubuntu:trusty")
        ));

        jobScriptWriter.write(String.format(
            "docker run --rm --name %s -v %s:/workspace %s /bin/bash -xe /workspace/jobScript.sh\n",
            jobScript.getName(), workingDirectory.getAbsolutePath(),
            getBuilder().getOrDefault("image", "ubuntu:trusty")
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
        jobShellScript.delete();
        jobScript.delete();
    }
}
