package ru.linachan.nemesis.executor.builder;

public class CustomBuilder extends ScriptBuilder {

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder((String) getBuilder().get("interpreter"), jobScript.getPath());
    }
}
