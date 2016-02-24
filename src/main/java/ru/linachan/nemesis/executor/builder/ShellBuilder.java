package ru.linachan.nemesis.executor.builder;

public class ShellBuilder extends ScriptBuilder {

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder("/bin/bash", "-xe", jobScript.getPath());
    }
}
