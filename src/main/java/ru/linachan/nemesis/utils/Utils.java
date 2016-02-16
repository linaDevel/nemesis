package ru.linachan.nemesis.utils;

import org.apache.commons.io.FileUtils;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.gerrit.Approval;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.layout.Score;
import ru.linachan.nemesis.layout.ScoreType;

import java.io.File;
import java.io.IOException;

public class Utils {

    public static File createTempDirectory(String postfix) throws IOException {
        final File temp;

        temp = File.createTempFile("nemesis", postfix);

        if(!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if(!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return temp;
    }

    public static File createTempFile(String postfix) throws IOException {
        return File.createTempFile("nemesis", postfix);
    }

    public static File ensureDirectory(File file, boolean reCreate) throws IOException {
        if (!file.exists()) {
            file.mkdir();
        } else if (!file.isDirectory()) {
            file.delete();
            file.mkdir();
        } else if (reCreate) {
            FileUtils.deleteDirectory(file);
            file.mkdir();
        }

        return file;
    }

    public static File ensureDirectory(File file) throws IOException {
        return ensureDirectory(file, false);
    }

    public static File createJobLogDirectory(String jobName, Event event) throws IOException {
        Integer changeID = event.getChangeRequest().getChangeNumber();
        Integer pathSetID = event.getPatchSet().getPatchSetNumber();

        File logsDir = Utils.ensureDirectory(NemesisConfig.getPath("logs").toFile());
        File changeSubLogsDir = Utils.ensureDirectory(new File(logsDir, String.valueOf(changeID % 100)));
        File changeLogsDir = Utils.ensureDirectory(new File(changeSubLogsDir, String.valueOf(changeID)));
        File patchSetLogsDir = Utils.ensureDirectory(new File(changeLogsDir, String.valueOf(pathSetID)));
        return Utils.ensureDirectory(new File(patchSetLogsDir, jobName), true);
    }

    public static String getJobLogURL(String jobName, Event event) {
        Integer changeID = event.getChangeRequest().getChangeNumber();
        Integer pathSetID = event.getPatchSet().getPatchSetNumber();

        return String.format(
            "%s%s/%s/%s/%s",
            NemesisConfig.getNemesisURL(), changeID % 100, changeID, pathSetID, jobName
        );
    }

    public static Score approvalToScore(Approval approval) {
        Score score = new Score();
        switch (approval.getType()) {
            case VERIFIED:
                score.approval = ScoreType.VERIFIED;
                break;
            case CODE_REVIEW:
                score.approval = ScoreType.CODE_REVIEW;
                break;
            case WORKFLOW:
                score.approval = ScoreType.WORKFLOW;
                break;
        }
        score.score = approval.getValue();

        return score;
    }

    public static Job noopJob() {
        Job noopJob = new Job();

        noopJob.name = "noop";

        return noopJob;
    }
}
