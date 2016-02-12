package ru.linachan.nemesis.watchdog;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.layout.Layout;
import ru.linachan.nemesis.utils.FileWatchDog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
        File layoutFile = new File(layoutDir, "layout.yaml");
        if (layoutFile.exists()) {
            System.out.println("Reading Nemesis layout...");
            Yaml layoutParser = new Yaml(new Constructor(Layout.class));
            layoutData = (Layout) layoutParser.load(new FileReader(layoutFile));
        }
    }

    public Layout getLayout() {
        return layoutData;
    }
}
