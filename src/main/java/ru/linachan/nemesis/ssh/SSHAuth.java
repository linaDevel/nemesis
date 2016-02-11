package ru.linachan.nemesis.ssh;

import java.io.File;

public class SSHAuth {

    private File privateKeyFile;
    private String username;
    private String privateKeyFilePassword;

    public SSHAuth(File privateKeyFile, String username, String privateKeyFilePassword) {
        this.privateKeyFile = privateKeyFile;
        this.username = username;
        this.privateKeyFilePassword = privateKeyFilePassword;
    }

    public SSHAuth(File privateKeyFile, String username) {
        this.privateKeyFile = privateKeyFile;
        this.username = username;
        this.privateKeyFilePassword = null;
    }

    public File getPrivateKeyFile() {
        return privateKeyFile;
    }

    public String getPrivateKeyFilePassword() {
        return privateKeyFilePassword;
    }

    public String getUsername() {
        return username;
    }
}