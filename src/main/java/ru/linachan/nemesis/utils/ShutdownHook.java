package ru.linachan.nemesis.utils;

import ru.linachan.nemesis.NemesisCore;

public class ShutdownHook extends Thread {

    private NemesisCore serviceInstance;

    public ShutdownHook(NemesisCore serviceInstanceObject) {
        serviceInstance = serviceInstanceObject;
    }

    @Override
    public void run() {
        serviceInstance.shutdown();
    }
}
