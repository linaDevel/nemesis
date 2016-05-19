package ru.linachan.nemesis;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import ru.linachan.nemesis.executor.EventHandler;
import ru.linachan.nemesis.gerrit.ChangeRequest;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.gerrit.EventListener;
import ru.linachan.nemesis.gerrit.PatchSet;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.layout.Layout;
import ru.linachan.nemesis.layout.Score;
import ru.linachan.nemesis.ssh.SSHAuth;
import ru.linachan.nemesis.ssh.SSHConnection;
import ru.linachan.nemesis.utils.ShutdownHook;
import ru.linachan.nemesis.watchdog.JobWatchDog;
import ru.linachan.nemesis.watchdog.LayoutWatchDog;
import ru.linachan.nemesis.web.NemesisWeb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NemesisCore {

    private String serverHost;
    private Integer serverPort;
    private SSHAuth serverAuth;

    private EventListener eventListener;
    private NemesisWeb webServer;

    private boolean running = true;

    private LayoutWatchDog layoutWatchDog;
    private JobWatchDog jobWatchDog;

    private Reflections discoveryHelper;
    private ExecutorService executorService;

    public NemesisCore() throws IOException {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

        discoveryHelper = new Reflections(
                ClasspathHelper.forPackage("ru.linachan"),
                new SubTypesScanner()
        );

        serverHost = NemesisConfig.getGerritHost();
        serverPort = NemesisConfig.getGerritPort();

        String serverUser = NemesisConfig.getGerritUser();
        File serverKey = NemesisConfig.getGerritKey();
        String serverPassPhrase = NemesisConfig.getGerritPassPhrase();

        if (serverPassPhrase.length() > 0) {
            serverAuth = new SSHAuth(serverKey, serverUser, serverPassPhrase);
        } else {
            serverAuth = new SSHAuth(serverKey, serverUser);
        }

        layoutWatchDog = new LayoutWatchDog();
        jobWatchDog = new JobWatchDog();

        executorService = Executors.newCachedThreadPool();

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
        executeThread(layoutWatchDog);
        executeThread(jobWatchDog);

        if (layoutWatchDog.getLayout() == null) {
            System.out.println("No layout data loaded, exitting");
            System.exit(1);
        }

        executeThread(eventListener);
        webServer.start();

        while (isRunning()) {
            Thread.sleep(100);
        }

        System.exit(0);
    }

    public void handleEvent(Event gerritEvent) {
        executeThread(new EventHandler(this, gerritEvent));
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

    public Reflections getDiscoveryHelper() {
        return discoveryHelper;
    }

    public Future executeThread(Runnable thread) {
        return executorService.submit(thread);
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
                case QUALITY_ASSURANCE:
                    reviewCommand += String.format(" --quality-assurance %d", score.score);
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

    public JSONObject query(String query) throws IOException, ParseException {
        String queryCommand = String.format(
            "gerrit query --all-approvals --current-patch-set --format json -- %s",
            query
        );

        SSHConnection connection = getGerritConnection();
        JSONParser parser = new JSONParser();

        return (JSONObject) parser.parse(connection.executeCommand(queryCommand));
    }
}
