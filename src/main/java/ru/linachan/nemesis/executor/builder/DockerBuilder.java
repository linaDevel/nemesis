package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.executor.JobExecutor;
import ru.linachan.nemesis.layout.Builder;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DockerBuilder extends SimpleBuilder {

    private File jobScript;
    private File jobShellScript;

    public DockerBuilder(JobExecutor executor, Job job, Builder builder, File workingDirectory) {
        super(executor, job, builder, workingDirectory);
    }

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        jobShellScript = new File(workingDirectory, "jobScript.sh");

        FileWriter jobShellScriptWriter = new FileWriter(jobShellScript);

        jobShellScriptWriter.write((String) getBuilder().params.get("script"));

        jobShellScriptWriter.flush();
        jobShellScriptWriter.close();

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        jobScriptWriter.write(String.format(
            "docker pull %s\n", getBuilder().params.getOrDefault("image", "ubuntu:trusty")
        ));

        jobScriptWriter.write(String.format(
            "docker run --rm --name %s -v %s:/workspace %s /bin/bash -xe /workspace/jobScript.sh\n",
            jobScript.getName(), workingDirectory.getAbsolutePath(),
            getBuilder().params.getOrDefault("image", "ubuntu:trusty")
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
