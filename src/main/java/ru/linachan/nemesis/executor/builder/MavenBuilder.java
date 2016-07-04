package ru.linachan.nemesis.executor.builder;

import org.apache.commons.lang3.StringUtils;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("unchecked")
public class MavenBuilder extends SimpleBuilder {

    private File jobScript;

    @Override
    protected void preBuild() throws IOException {
        jobScript = Utils.createTempFile(getJob().name);
        jobScript.setExecutable(true);

        FileWriter jobScriptWriter = new FileWriter(jobScript);

        jobScriptWriter.write(
            "MAVEN=$(which mvn)\n" +
            "if [[ -z \"${MAVEN}\" ]]; then" +
            "\techo \"Maven is not installed!\" >&2" +
            "\texit 1" +
            "fi"
        );

        jobScriptWriter.write("pushd ${WORKSPACE}/source\n");

        jobScriptWriter.write(String.format(
            "$MAVEN -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true %s\n",
            StringUtils.join(((List<String>) getBuilder().get("target")).toArray(), " ")
        ));

        jobScriptWriter.write("popd\n");

        if ((Boolean) getBuilder().get("copyJar")) {
            jobScriptWriter.write("mkdir -p ${WORKSPACE}/artifacts\n");
            jobScriptWriter.write("cp ${WORKSPACE}/source/target/*.jar ${WORKSPACE}/artifacts/\n");
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
