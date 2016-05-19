package ru.linachan.nemesis.executor.publisher;

import com.jcraft.jsch.Channel;
import ru.linachan.nemesis.NemesisConfig;
import ru.linachan.nemesis.ssh.SSHAuth;
import ru.linachan.nemesis.ssh.SSHConnection;

import java.io.*;

public class SCPPublisher extends SimplePublisher {

    private OutputStream out;
    private InputStream in;

    @Override
    public void publish() throws InterruptedException, IOException {
        String remoteHost = (String) publisher.get("host");
        Integer remotePort = (Integer) publisher.getOrDefault("port", 22);
        String remoteUser = (String) publisher.get("user");

        String remotePath = (String) publisher.get("targetPath");

        File keyFile = NemesisConfig.getGerritKey();
        String keyPass = NemesisConfig.getGerritPassPhrase();

        File artifactsDirectory = new File(workingDirectory, "artifacts");
        if (artifactsDirectory.exists()) {

            SSHAuth auth = (keyPass != null) ? new SSHAuth(keyFile, remoteUser, keyPass) : new SSHAuth(keyFile, remoteUser);
            SSHConnection connection = new SSHConnection(remoteHost, remotePort, null, auth);

            String command = String.format("scp -t %s", remotePath);
            Channel channel = connection.executeCommandChannel(command);

            out = channel.getOutputStream();
            in = channel.getInputStream();

            if (assertAck(in)) {
                return;
            }

            File[] artifactFiles = artifactsDirectory.listFiles();
            if (artifactFiles != null) {
                for (File artifactFile: artifactFiles) {
                    if (artifactFile.isFile()) {
                        sendFile(artifactFile);
                    } else if (artifactFile.isDirectory()) {
                        sendDirectory(artifactFile);
                    }
                }
            }

            out.close();
            channel.disconnect();

            connection.disconnect();
        } else {
            executor.putLine("No artifacts, skipping publishing");
        }
    }

    private void sendDirectory(File directoryToSend) throws IOException {
        String directoryNameSizeInfo = String.format(
            "D0755 0 %s\n", directoryToSend.getName()
        );

        out.write(directoryNameSizeInfo.getBytes());
        out.flush();
        if (assertAck(in)) {
            return;
        }

        File[] directoryFiles = directoryToSend.listFiles();
        if (directoryFiles != null) {
            for (File directoryItem: directoryFiles) {
                if (directoryItem.isDirectory()) {
                    sendDirectory(directoryItem);
                } else if (directoryItem.isFile()) {
                    sendFile(directoryItem);
                }
            }
        }

        out.write("E".getBytes());

        assertAck(in);
    }

    private void sendFile(File fileToSend) throws IOException {
        String fileNameSizeInfo = String.format(
            "C0644 %d %s\n",
            fileToSend.length(), fileToSend.getName()
        );

        out.write(fileNameSizeInfo.getBytes());
        out.flush();
        if (assertAck(in)) {
            return;
        }

        try (FileInputStream tarFileIS = new FileInputStream(fileToSend)) {
            byte[] buf = new byte[1024];
            int len;

            while((len = tarFileIS.read(buf, 0, buf.length)) > 0){
                out.write(buf, 0, len);
                // out.flush();
            }
        }

        out.write(new byte[] {0});
        out.flush();

        assertAck(in);
    }

    private boolean assertAck(InputStream in) throws IOException {
        if (checkAck(in) != 0) {
            executor.putLine("Unable to publish artifacts");
            return false;
        }

        return true;
    }

    private int checkAck(InputStream in) throws IOException{
        int state = in.read();

        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1

        if (state == 0) return state;
        if (state == -1) return state;

        if (state == 1 || state == 2) {
            StringBuffer errorMsg = new StringBuffer();

            int chr;
            do {
                chr = in.read();
                errorMsg.append((char) chr);
            } while (chr != '\n');

            if (state == 1) {
                executor.putLine("SCP Error: %s", errorMsg.toString());
            } else {
                executor.putLine("SCP Fatal Error: %s", errorMsg.toString());
            }
        }

        return state;
    }
}
