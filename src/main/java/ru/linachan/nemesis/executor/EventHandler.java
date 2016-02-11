package ru.linachan.nemesis.executor;

import ru.linachan.nemesis.NemesisCore;
import ru.linachan.nemesis.gerrit.Event;
import ru.linachan.nemesis.layout.*;
import ru.linachan.nemesis.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class EventHandler implements Runnable {

    private NemesisCore service;

    private Event event;

    public EventHandler(NemesisCore serviceCore, Event incomingEvent) {
        service = serviceCore;
        event = incomingEvent;
    }

    public void execute() {
        Thread executionThread = new Thread(this);
        executionThread.start();
    }

    @Override
    public void run() {
        if (event.getChangeRequest() != null) {
            Optional<Map> projectOptional = service.getLayout().projects.stream()
                .filter(project -> Pattern.matches((String) project.get("name"), event.getChangeRequest().getProject()))
                .findFirst();

            boolean projectConfigured = projectOptional.isPresent();
            Map projectData = projectConfigured ? projectOptional.get() : null;
            String projectName = event.getChangeRequest().getProject();

            if (projectConfigured) {
                List<String> projectPipelines = (List<String>) projectData.keySet().stream()
                    .filter(pipelineName -> !pipelineName.equals("name"))
                    .collect(Collectors.toList());

                service.getLayout().pipelines.stream()
                    .filter(pipeLine -> projectPipelines.contains(pipeLine.name))
                    .forEach(pipeLine -> {
                        final boolean[] isTriggered = {false};

                        pipeLine.triggers.stream()
                            .filter(trigger -> event.getEventType().equals(trigger.event))
                            .forEach(trigger -> {
                                isTriggered[0] = true;

                                switch (event.getEventType()) {
                                    case COMMENT_ADDED:
                                        if (trigger.commentFilter != null) {
                                            Pattern commentPattern = Pattern.compile(trigger.commentFilter);
                                            if (!commentPattern.matcher(event.getComment()).matches()) {
                                                isTriggered[0] = false;
                                            }
                                        }

                                        if (trigger.approvals != null) {
                                            final boolean[] approved = { event.getApprovals() != null };

                                            if (approved[0]) {
                                                List<Score> eventApprovals = event.getApprovals().stream()
                                                    .map(Utils::approvalToScore)
                                                    .collect(Collectors.toList());

                                                trigger.approvals.stream()
                                                    .forEach(approval -> {
                                                        boolean eventApproved = eventApprovals.stream()
                                                            .filter(eventApproval ->
                                                                (eventApproval.score.equals(approval.score))&&
                                                                (eventApproval.approval.equals(approval.approval))
                                                            ).findFirst().isPresent();

                                                        approved[0] = approved[0] && eventApproved;
                                                    });
                                            }

                                            isTriggered[0] = isTriggered[0] && approved[0];
                                        }
                                        break;
                                    case REF_UPDATED:
                                        if (trigger.ref != null) {
                                            Pattern refPattern = Pattern.compile(trigger.ref);
                                            String refName = event.getCustomAttribute("refName", String.class);
                                            if (!refPattern.matcher(refName).matches()) {
                                                isTriggered[0] = false;
                                            }
                                        }
                                        break;
                                }
                            });

                        if (isTriggered[0]) {
                            System.out.println(String.format(
                                "Starting %s jobs for %s",
                                pipeLine.name, projectName
                            ));

                            List<Job> jobs = ((List<String>) projectData.get(pipeLine.name)).stream()
                                .map(jobName -> service.getJob(jobName))
                                .collect(Collectors.toList());

                            new PipeLineExecutor(service, jobs, pipeLine, event).execute();
                        }
                    });
            } else {
                System.out.println(String.format("Ignoring event for project: %s", projectName));
            }
        }
    }
}
