package ru.linachan.nemesis.executor.builder;

import java.io.IOException;

public class NoopBuilder extends SimpleBuilder {

    @Override
    protected void preBuild() throws IOException {

    }

    @Override
    protected ProcessBuilder build() {
        return getProcessBuilder("ls");
    }

    @Override
    protected void postBuild() {

    }
}
