package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.layout.Layout;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NemesisConfigInstaller extends SimpleBuilder {

    protected File jobScript;

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        File sourceDirectory = new File(workingDirectory, "source");

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        try {
            Utils.readJobConfiguration(new File(sourceDirectory, "jobs"));
        } catch (Exception e) {
            jobScriptWriter.write(String.format("echo \"Invalid job configuration: %s\"\nexit 1\n", e.getMessage()));
        }

        try {
            Layout layout = Utils.readLayoutData(new File(sourceDirectory, "layout"));
            if (layout == null) {
                throw new IllegalStateException("Unable to find layout file");
            }
        } catch (Exception e) {
            jobScriptWriter.write(String.format("echo \"Invalid layout configuration: %s\"\nexit 1\n", e.getMessage()));
        }

        if ((Boolean) getBuilder().getOrDefault("installConfig", false)) {
            jobScriptWriter.write("echo \"Installing new configuration...\"\n");
            jobScriptWriter.write(String.format("cp -Rv ${WORKSPACE}/source/jobs/* %s\n", NemesisConfig.getPath("jobs")));
            jobScriptWriter.write(String.format("cp -Rv ${WORKSPACE}/source/layout/layout.yaml %s\n", NemesisConfig.getPath("layout")));
            jobScriptWriter.write("exit 0\n");
        } else {
            jobScriptWriter.write("echo \"Skipping config installation...\"\n");
            jobScriptWriter.write("exit 0\n");
        }

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
