package ru.linachan.nemesis.executor.publisher;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.output.StringBuilderWriter;
import ru.linachan.nemesis.utils.FileHash;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

            if ((Boolean) publisher.get("checksum")) {
                File[] files = targetDirectory.listFiles();
                List<FileHash> checkSumList = new ArrayList<>();

                if (files != null) {
                    for (File file: files) {
                        if (!file.getName().equals("CHECKSUMS")) {
                            checkSumList.add(new FileHash(file));
                        }
                    }
                }

                File checkSums = new File(targetDirectory, "CHECKSUMS");
                try(FileWriter checkSumsWriter = new FileWriter(checkSums)) {
                    for (FileHash fileHash: checkSumList) {
                        checkSumsWriter.write(
                            String.format("%s\n", fileHash.toString())
                        );
                    }

                    checkSumsWriter.flush();
                    checkSumsWriter.close();
                }
            }
        }
    }
}
