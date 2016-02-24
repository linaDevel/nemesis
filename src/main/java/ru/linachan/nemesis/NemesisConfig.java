package ru.linachan.nemesis;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NemesisConfig {

    public static String getNemesisURL() {
        String url = System.getenv("NEMESIS_URL");
        return (url != null) ? url : "http://localhost/";
    }

    public static String getGerritURL() {
        String url = System.getenv("NEMESIS_GERRIT_URL");
        return (url != null) ? url : "http://localhost/";
    }

    public static String getGerritHost() {
        String host = System.getenv("NEMESIS_GERRIT_HOST");
        return (host != null) ? host : "localhost";
    }

    public static Integer getGerritPort() {
        String port = System.getenv("NEMESIS_GERRIT_PORT");
        return (port != null) ? Integer.valueOf(port) : 29418;
    }

    public static String getGerritUser() {
        String user = System.getenv("NEMESIS_GERRIT_USER");
        return (user != null) ? user : "gerrit";
    }

    public static File getGerritKey() {
        String key = System.getenv("NEMESIS_GERRIT_KEY");
        return new File((key != null) ? key : "~/.ssh/id_rsa");
    }

    public static String getGerritPassPhrase() {
        return System.getenv("NEMESIS_GERRIT_KEYPASS");
    }

    public static Path getRoot() {
        String path = System.getenv("NEMESIS_ROOT");
        return Paths.get((path != null) ? path : "./");
    }

    public static Path getPath(String... paths) {
        return Paths.get(getRoot().toString(), paths);
    }
}
