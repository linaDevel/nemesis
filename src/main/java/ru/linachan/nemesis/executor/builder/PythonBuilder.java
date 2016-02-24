package ru.linachan.nemesis.executor.builder;

public class PythonBuilder extends ScriptBuilder {

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder("/usr/bin/python", jobScript.getPath());
    }
}
