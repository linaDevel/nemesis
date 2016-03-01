package ru.linachan.nemesis.executor.publisher;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.executor.JobExecutor;
import ru.linachan.nemesis.executor.JobPublisher;
import ru.linachan.nemesis.layout.Job;
import ru.linachan.nemesis.utils.Utils;

import java.io.IOException;
import java.util.Map;

@SuppressWarnings("unchecked")
public class SlackPublisher implements JobPublisher {

    @Override
    public void publish(JobExecutor executor, Job job, Map<String, Object> publisher, Map<String, String> environment) throws InterruptedException, IOException {
        JSONObject postHook = new JSONObject();

        Integer changeID = Integer.valueOf(environment.get("NEMESIS_CHANGE_ID"));
        Integer pathSetID = Integer.valueOf(environment.get("NEMESIS_PATCHSET_ID"));

        if (publisher.containsKey("user"))
            postHook.put("username", publisher.get("user"));

        if (publisher.containsKey("icon"))
            postHook.put("icon_url", publisher.get("icon"));

        if (publisher.containsKey("channel"))
            postHook.put("channel", publisher.get("channel"));

        JSONArray attachments = new JSONArray();

        JSONObject attachment = new JSONObject();

        attachment.put("color", executor.isSuccess() ? "good" : "danger");
        attachment.put("title", job.name);
        attachment.put("title_link", String.format(
            "%s%s/%s/%s/%s",
            NemesisConfig.getNemesisURL(), changeID % 100, changeID, pathSetID, job.name
        ));

        JSONArray fields = new JSONArray();

        JSONObject projectField = new JSONObject();
        projectField.put("title", "Project");
        projectField.put("value", environment.get("NEMESIS_PROJECT"));
        projectField.put("short", "true");
        fields.add(projectField);

        JSONObject changeField = new JSONObject();
        changeField.put("title", "Change");
        changeField.put("value", String.format(
            "%s,%s", changeID, pathSetID
        ));
        changeField.put("short", "true");
        fields.add(changeField);

        JSONObject statusField = new JSONObject();
        statusField.put("title", "Status");
        statusField.put("value", executor.isSuccess() ? "Success" : "Failure");
        statusField.put("short", "true");
        fields.add(statusField);

        attachment.put("fields", fields);

        attachments.add(attachment);

        postHook.put("attachments", attachments);

        Utils.doPOST((String) publisher.get("hook"), postHook);
    }

}
