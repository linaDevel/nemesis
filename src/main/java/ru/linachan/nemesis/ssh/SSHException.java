package ru.linachan.nemesis.ssh;


import java.io.IOException;

public class SSHException extends IOException {

    public SSHException(Throwable cause) {
        super(cause);
    }

    public SSHException(String message, Throwable cause) {
        super(message, cause);
    }

    public SSHException(String message) {
        super(message);
    }

    public SSHException() {}

}