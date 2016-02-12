package ru.linachan.nemesis;

import ru.linachan.nemesis.executor.EventHandler;
import ru.linachan.nemesis.gerrit.ChangeRequest;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.gerrit.EventListener;
import ru.linachan.nemesis.gerrit.PatchSet;
import ru.linachan.nemesis.layout.*;
import ru.linachan.nemesis.ssh.SSHAuth;
import ru.linachan.nemesis.ssh.SSHConnection;
import ru.linachan.nemesis.utils.ShutdownHook;
import ru.linachan.nemesis.watchdog.JobWatchDog;
import ru.linachan.nemesis.watchdog.LayoutWatchDog;
import ru.linachan.nemesis.web.NemesisWeb;

import java.io.*;
import java.util.*;

public class NemesisCore {

    private String serverHost;
    private Integer serverPort;
    private SSHAuth serverAuth;

    private EventListener eventListener;
    private NemesisWeb webServer;

    private boolean running = true;

    private LayoutWatchDog layoutWatchDog;
    private JobWatchDog jobWatchDog;

    public NemesisCore() throws IOException {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

        serverHost = NemesisConfig.getHost();
        serverPort = NemesisConfig.getPort();

        String serverUser = NemesisConfig.getUser();
        File serverKey = NemesisConfig.getKey();
        String serverPassPhrase = NemesisConfig.getPassPhrase();

        if (serverPassPhrase.length() > 0) {
            serverAuth = new SSHAuth(serverKey, serverUser, serverPassPhrase);
        } else {
            serverAuth = new SSHAuth(serverKey, serverUser);
        }

        layoutWatchDog = new LayoutWatchDog();
        jobWatchDog = new JobWatchDog();

        eventListener = new EventListener(this);
        webServer = new NemesisWeb();
    }

    public SSHConnection getGerritConnection() throws IOException {
        return new SSHConnection(serverHost, serverPort, "", serverAuth);
    }

    public static void main(String[] args) {
        try {
            NemesisCore instance = new NemesisCore();

            instance.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run() throws Exception {
        layoutWatchDog.start();
        jobWatchDog.start();

        eventListener.start();
        webServer.start();

        while (isRunning()) {
            Thread.sleep(100);
        }

        System.exit(0);
    }

    public void handleEvent(Event gerritEvent) {
        new EventHandler(this, gerritEvent).execute();
    }

    public boolean isRunning() {
        return running;
    }

    public void shutdown() {
        running = false;
    }

    public Layout getLayout() {
        return layoutWatchDog.getLayout();
    }

    public Job getJob(String name) {
        return jobWatchDog.getJob(name);
    }

    public void review(ChangeRequest change, PatchSet patchSet, String message, Score... scores) throws IOException {
        List<Score> scoreList = new ArrayList<>();
        Collections.addAll(scoreList, scores);
        review(change, patchSet, message, scoreList);
    }

    public void review(ChangeRequest change, PatchSet patchSet, String message, List<Score> scores) throws IOException {
        String reviewCommand = String.format(
            "gerrit review %d,%d", change.getChangeNumber(), patchSet.getPatchSetNumber()
        );

        for (Score score: scores) {
            switch (score.approval) {
                case VERIFIED:
                    reviewCommand += String.format(" --verified %d", score.score);
                    break;
                case CODE_REVIEW:
                    reviewCommand += String.format(" --code-review %d", score.score);
                    break;
                case SUBMIT:
                    reviewCommand += " --submit";
                    break;
            }
        }

        if (message != null) {
            reviewCommand += String.format(" --message \"%s\"", message);
        }

        SSHConnection connection = getGerritConnection();
        connection.executeCommand(reviewCommand);
    }
}
