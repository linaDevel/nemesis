package ru.linachan.nemesis.executor.builder;

import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VagrantBuilder extends SimpleBuilder  {

    private File jobScript;
    private File vagrantFile;
    private File jobShellScript;

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        vagrantFile = new File(workingDirectory, "Vagrantfile");
        jobShellScript = new File(workingDirectory, "jobScript.sh");

        FileWriter jobShellScriptWriter = new FileWriter(jobShellScript);

        jobShellScriptWriter.write((String) getBuilder().get("script"));

        jobShellScriptWriter.flush();
        jobShellScriptWriter.close();

        if (!vagrantFile.exists()) {
            FileWriter vagrantFileWriter = new FileWriter(vagrantFile);

            vagrantFileWriter.write(
                "Vagrant.configure(\"2\") do |config|\n" +
                    String.format("\tconfig.vm.box = \"%s\"\n", getBuilder().getOrDefault("image", "hashicorp/trusty64")) +
                "end"
            );

            vagrantFileWriter.flush();
            vagrantFileWriter.close();
        }

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        jobScriptWriter.write("vagrant up\n");
        jobScriptWriter.write("set +e\n");
        jobScriptWriter.write("vagrant ssh -c '/bin/bash -xe /vagrant/jobScript.sh'\n");
        jobScriptWriter.write("retval=$?\n");
        jobScriptWriter.write("set -e\n");
        jobScriptWriter.write("vagrant destroy");
        jobScriptWriter.write("exit ${retval}");

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
        vagrantFile.delete();
        jobScript.delete();
    }
}
