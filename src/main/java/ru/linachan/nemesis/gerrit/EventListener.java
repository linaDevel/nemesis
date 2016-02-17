package ru.linachan.nemesis.gerrit;

import ru.linachan.nemesis.NemesisCore;
import ru.linachan.nemesis.ssh.SSHConnection;

import java.io.BufferedReader;
import java.io.IOException;

public class EventListener extends Thread {

    private NemesisCore serviceInstance;

    private SSHConnection serverLink;

    public EventListener(NemesisCore serviceInstanceObject) throws IOException {
        serviceInstance = serviceInstanceObject;
        serverLink = serviceInstanceObject.getGerritConnection();
    }

    @Override
    public void run() {
        if (serverLink != null) {
            try {
                BufferedReader eventReader = new BufferedReader(serverLink.executeCommandReader("gerrit stream-events"));

                String eventData;

                while (serviceInstance.isRunning()) {
                    if ((eventData = eventReader.readLine()) != null) {
                        if (eventData.length() > 0) {
                            Event event = new Event(eventData);

                            serviceInstance.handleEvent(event);
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}