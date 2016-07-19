package ru.linachan.nemesis.watchdog;

import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.layout.Layout;
import ru.linachan.nemesis.utils.FileWatchDog;
import ru.linachan.nemesis.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.WatchEvent;

public class LayoutWatchDog extends FileWatchDog {

    private File layoutDir;
    private Layout layoutData;

    private static Logger logger = LoggerFactory.getLogger(LayoutWatchDog.class);

    public LayoutWatchDog() throws IOException {
        super(NemesisConfig.getPath("layout").toFile());
        layoutDir = NemesisConfig.getPath("layout").toFile();

        checkLayoutFile();
    }

    @Override
    protected void onCreate(WatchEvent event) throws IOException {
        checkLayoutFile();
    }

    @Override
    protected void onModify(WatchEvent event) throws IOException {
        checkLayoutFile();
    }

    @Override
    protected void onDelete(WatchEvent event) throws IOException {
        checkLayoutFile();
    }

    public void checkLayoutFile() throws FileNotFoundException {
        logger.info("Reading Nemesis layout...");

        try {
            layoutData = Utils.readLayoutData(layoutDir);
        } catch (Exception e) {
            logger.error("Unable to read layout configuration: {}", e.getMessage());
        }
    }

    public Layout getLayout() {
        return layoutData;
    }
}
