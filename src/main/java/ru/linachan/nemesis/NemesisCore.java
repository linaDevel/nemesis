package ru.linachan.nemesis;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import ru.linachan.nemesis.executor.EventHandler;
import ru.linachan.nemesis.gerrit.ChangeRequest;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.gerrit.EventListener;
import ru.linachan.nemesis.gerrit.PatchSet;
import ru.linachan.nemesis.layout.*;
import ru.linachan.nemesis.ssh.SSHAuth;
import ru.linachan.nemesis.ssh.SSHConnection;
import ru.linachan.nemesis.utils.FileWatchDog;
import ru.linachan.nemesis.utils.ShutdownHook;
import ru.linachan.nemesis.utils.Utils;

import java.io.*;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.*;

public class NemesisCore {

    private String serverHost;
    private Integer serverPort;
    private SSHAuth serverAuth;

    private EventListener eventListener;
    private boolean running = true;

    private Layout layout;
    private Map<String, Job> jobDefinitions;

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

        layout = readLayout();
        jobDefinitions = readJobDefinition();

        setUpWatchDog();

        eventListener = new EventListener(this);
    }

    private void setUpWatchDog() throws IOException {
        new Thread(new FileWatchDog(NemesisConfig.getPath("layout").toFile()) {
            @Override
            protected void onCreate(WatchEvent event) {

            }

            @Override
            protected void onModify(WatchEvent event) {
                try {
                    layout = readLayout();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void onDelete(WatchEvent event) {

            }
        }).start();

        new Thread(new FileWatchDog(NemesisConfig.getPath("layout").toFile()) {
            @Override
            protected void onCreate(WatchEvent event) {
                try {
                    jobDefinitions = readJobDefinition();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void onModify(WatchEvent event) {
                try {
                    jobDefinitions = readJobDefinition();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void onDelete(WatchEvent event) {
                try {
                    jobDefinitions = readJobDefinition();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public SSHConnection getGerritConnection() throws IOException {
        return new SSHConnection(serverHost, serverPort, "", serverAuth);
    }

    public static void main(String[] args) {
        try {
            NemesisCore instance = new NemesisCore();

            instance.run();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void run() throws InterruptedException {
        eventListener.start();

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
        return layout;
    }

    public Job getJob(String name) {
        return jobDefinitions.containsKey(name) ? jobDefinitions.get(name) : name.equals("noop") ? Utils.noopJob() : null;
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

    private Layout readLayout() throws FileNotFoundException {
        System.out.println("Reading Nemesis layout...");
        Yaml layoutParser = new Yaml(new Constructor(Layout.class));
        return (Layout) layoutParser.load(new FileReader("layout/nemesis.yaml"));
    }

    private Map<String, Job> readJobDefinition() throws FileNotFoundException {
        System.out.println("Reading Nemesis job definition...");
        Map<String, Job> jobDefinitions = new HashMap<>();

        File jobFolder = new File("jobs");
        Yaml jobParser = new Yaml(new Constructor(JobDefinition.class));

        for (File jobFile: jobFolder.listFiles()) {
            if (!jobFile.isDirectory()) {
                JobDefinition jobDefinition = (JobDefinition) jobParser.load(new FileReader(jobFile));
                for (Job job: jobDefinition.jobs) {
                    jobDefinitions.put(job.name, job);
                }
            }
        }

        return jobDefinitions;
    }
}
