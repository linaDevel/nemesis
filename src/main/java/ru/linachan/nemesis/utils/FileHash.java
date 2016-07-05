package ru.linachan.nemesis.utils;

import java.io.File;
import java.io.IOException;

public class FileHash {

    private final File file;
    private final String md5;
    private final String sha256;

    public FileHash(File fileToHash) throws IOException {
        file = fileToHash;
        md5 = Utils.hashFile(fileToHash, "MD5");
        sha256 = Utils.hashFile(fileToHash, "SHA-256");
    }

    public File getFile() {
        return file;
    }

    public String getMD5() {
        return md5;
    }

    public String getSHA256() {
        return sha256;
    }

    public String toString() {
        return String.format("%s %s %s", md5, sha256, file.getName());
    }
}
