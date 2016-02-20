package ru.linachan.nemesis.executor.builder;

import org.apache.commons.lang.StringUtils;
import ru.linachan.nemesis.layout.Builder;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("unchecked")
public class MavenBuilder extends SimpleBuilder {

    private File jobScript;

    public MavenBuilder(Job job, Builder builder, File workingDirectory) {
        super(job, builder, workingDirectory);
    }

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        jobScriptWriter.write("#!/bin/bash\n");
        jobScriptWriter.write("set -xe\n");
        jobScriptWriter.write("pushd ${WORKSPACE}/source\n");

        jobScriptWriter.write(String.format(
            "mvn %s\n",
            StringUtils.join(((List<String>) getBuilder().params.get("target")).toArray(), " ")
        ));

        jobScriptWriter.write("popd\n");

        if ((Boolean) getBuilder().params.get("copyJar")) {
            jobScriptWriter.write("mkdir -p ${WORKSPACE}/artifacts\n");
            jobScriptWriter.write("cp ${WORKSPACE}/source/target/*.jar ${WORKSPACE}/artifacts/\n");
        }

        jobScriptWriter.flush();
        jobScriptWriter.close();
    }

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder("/bin/bash", jobScript.getPath());
    }

    @Override
    protected void postBuild() {
        //jobScript.delete();
    }
}