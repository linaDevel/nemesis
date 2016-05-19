package ru.linachan.nemesis.watchdog;

import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.layout.Layout;
import ru.linachan.nemesis.utils.FileWatchDog;
import ru.linachan.nemesis.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.WatchEvent;

public class LayoutWatchDog extends FileWatchDog {

    private File layoutDir;
    private Layout layoutData;

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

    private void checkLayoutFile() throws FileNotFoundException {
        try {
            layoutData = Utils.readLayoutData(layoutDir);
        } catch (Exception e) {
            System.err.println(String.format("Unable to read layout configuration: %s", e.getMessage()));
        }
    }

    public Layout getLayout() {
        return layoutData;
    }
}
