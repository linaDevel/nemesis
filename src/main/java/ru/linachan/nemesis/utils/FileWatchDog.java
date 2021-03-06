package ru.linachan.nemesis.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@SuppressWarnings("unchecked")
public abstract class FileWatchDog implements Runnable {

    private WatchService watcher;

    private static Logger logger = LoggerFactory.getLogger(FileWatchDog.class);

    public FileWatchDog(File targetFile) throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        targetFile.toPath().register(watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        );
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                try {
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        onCreate(event);
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        onModify(event);
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        onDelete(event);
                    }
                } catch (IOException e) {
                    logger.error("Something went wrong during file monitoring: [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    protected abstract void onCreate(WatchEvent event) throws IOException;
    protected abstract void onModify(WatchEvent event) throws IOException;
    protected abstract void onDelete(WatchEvent event) throws IOException;
}
