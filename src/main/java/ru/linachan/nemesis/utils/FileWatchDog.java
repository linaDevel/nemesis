package ru.linachan.nemesis.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@SuppressWarnings("unchecked")
public abstract class FileWatchDog implements Runnable {

    private WatchService watcher;

    public FileWatchDog(File targetFile) throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        targetFile.toPath().register(watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        );
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

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    onCreate(event);
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    onModify(event);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    onDelete(event);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    protected abstract void onCreate(WatchEvent event);
    protected abstract void onModify(WatchEvent event);
    protected abstract void onDelete(WatchEvent event);
}
