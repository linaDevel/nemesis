package ru.linachan.nemesis.executor.publisher;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class LocalPublisher extends SimplePublisher {

    @Override
    public void publish() throws InterruptedException, IOException {
        File artifactsDirectory = new File(workingDirectory, "artifacts");
        File targetDirectory = new File((String) publisher.get("target"));

        if (!targetDirectory.exists()) {
            FileUtils.forceMkdir(targetDirectory);
        }

        if (artifactsDirectory.exists()) {
            File[] filesToPublish = artifactsDirectory.listFiles((FilenameFilter) new WildcardFileFilter((String) publisher.get("source")));
            if (filesToPublish != null) {
                for (File fileToPublish: filesToPublish) {
                    if (fileToPublish.isDirectory()) {
                        FileUtils.copyDirectory(fileToPublish, new File(targetDirectory, fileToPublish.getName()));
                    } else if (fileToPublish.isFile()) {
                        Files.copy(fileToPublish, new File(targetDirectory, fileToPublish.getName()));
                    }
                }
            }
        }
    }
}
