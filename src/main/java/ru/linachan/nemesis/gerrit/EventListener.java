package ru.linachan.nemesis.gerrit;

import ru.linachan.nemesis.NemesisCore;
import ru.linachan.nemesis.ssh.SSHConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

public class EventListener extends Thread {

    private NemesisCore serviceInstance;

    private SSHConnection serverLink;

    private static Logger logger = LoggerFactory.getLogger(EventListener.class);

    public EventListener(NemesisCore serviceInstanceObject) throws IOException {
        serviceInstance = serviceInstanceObject;
        serverLink = serviceInstanceObject.getGerritConnection();
    }

    @Override
    public void run() {
        while (true) {
            try (SSHConnection serverLink = serviceInstance.getGerritConnection()) {
                BufferedReader eventReader = new BufferedReader(serverLink.executeCommandReader("gerrit stream-events"));

                String eventData;

                while (serviceInstance.isRunning()) {
                    if ((eventData = eventReader.readLine()) != null) {
                        if (eventData.length() > 0) {
                            Event event = new Event(eventData, serviceInstance);

                            serviceInstance.handleEvent(event);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("EventListener failure: [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
