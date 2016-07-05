package ru.linachan.nemesis.utils;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.gerrit.Approval;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.layout.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String hashFile(File file, String algorithm) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            byte[] bytesBuffer = new byte[1024];
            int bytesRead = -1;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            byte[] hashedBytes = digest.digest();

            return bytesToHex(hashedBytes);
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new IOException("Could not generate hash from file", ex);
        }
    }

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

        if (jobName.equals("noop"))
            return "";

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
            case QUALITY_ASSURANCE:
                score.approval = ScoreType.QUALITY_ASSURANCE;
                break;
        }
        score.score = approval.getValue();

        return score;
    }

    public static String doPOST(String url, JSONObject data) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("POST");

        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(data.toJSONString());
        wr.flush();

        StringBuilder sb = new StringBuilder();
        int HttpResult = con.getResponseCode();

        if (HttpResult == HttpURLConnection.HTTP_OK){
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            br.close();

        }

        return sb.toString();
    }

    public static Map<String, Job> readJobConfiguration(File jobsDir) throws FileNotFoundException {
        Map<String, Job> jobData = new HashMap<>();

        Yaml jobParser = new Yaml(new Constructor(JobDefinition.class));

        File[] jobFiles = jobsDir.listFiles();
        if (jobFiles != null) {
            for (File jobFile: jobFiles) {
                if (!jobFile.isDirectory() && jobFile.getName().endsWith(".yaml")) {
                    JobDefinition jobDefinition = (JobDefinition) jobParser.load(new FileReader(jobFile));
                    for (Job job : jobDefinition.jobs) {
                        jobData.put(job.name, job);
                    }
                }
            }
        }

        return jobData;
    }

    public static Layout readLayoutData(File layoutDir) throws FileNotFoundException {
        Layout layoutData = null;

        File layoutFile = new File(layoutDir, "layout.yaml");

        if (layoutFile.exists()) {
            Yaml layoutParser = new Yaml(new Constructor(Layout.class));
            layoutData = (Layout) layoutParser.load(new FileReader(layoutFile));
        }

        return layoutData;
    }
}
