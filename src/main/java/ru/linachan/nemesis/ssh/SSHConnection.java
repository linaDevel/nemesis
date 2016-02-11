package ru.linachan.nemesis.ssh;

import com.jcraft.jsch.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;

public class SSHConnection {

    private static final int ALIVE_INTERVAL = 30 * 1000;
    protected static final String CMD_EXEC = "exec";
    protected static final int PROTO_HOST_DELIM_LENGTH = 3;
    private final JSch client;
    private Session connectSession;

    public SSHConnection(String host, int port, String proxy, SSHAuth authentication) throws IOException {
        try {
            client = new JSch();
            client.addIdentity(authentication.getPrivateKeyFile().getAbsolutePath(),
                    authentication.getPrivateKeyFilePassword());
            client.setHostKeyRepository(new BlindHostKeyRepository());
            connectSession = client.getSession(authentication.getUsername(), host, port);
            if (proxy != null && !proxy.isEmpty()) {
                String[] splitted = proxy.split(":");
                if (splitted.length > 2 && splitted[1].length() >= PROTO_HOST_DELIM_LENGTH) {
                    String pproto = splitted[0];
                    String phost = splitted[1].substring(2);
                    int pport = Integer.parseInt(splitted[2]);
                    if (pproto.equals("socks5") || pproto.equals("http")) {
                        if (pproto.equals("socks5")) {
                            connectSession.setProxy(new ProxySOCKS5(phost, pport));
                        } else {
                            connectSession.setProxy(new ProxyHTTP(phost, pport));
                        }
                    } else {
                        throw new MalformedURLException("Only HTTP and SOCKS5 protocols are supported");
                    }
                } else {
                    throw new MalformedURLException(proxy);
                }
            }
            connectSession.connect();
            connectSession.setServerAliveInterval(ALIVE_INTERVAL);
        } catch (JSchException ex) {
            throw new SSHException(ex);
        }
    }

    public synchronized boolean isConnected() {
        return isAuthenticated();
    }

    public synchronized boolean isAuthenticated() {
        return client != null && connectSession != null && connectSession.isConnected();
    }

    public synchronized String executeCommand(String command) throws SSHException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected!");
        }
        try {
            Channel channel = connectSession.openChannel(CMD_EXEC);
            ((ChannelExec)channel).setCommand(command);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String incomingLine;
            StringBuilder commandOutput = new StringBuilder();
            while ((incomingLine = bufferedReader.readLine()) != null) {
                commandOutput.append(incomingLine);
                commandOutput.append('\n');
            }
            bufferedReader.close();
            channel.disconnect();

            return commandOutput.toString();
        } catch (JSchException | IOException ex) {
            throw new SSHException(ex);
        }
    }

    public synchronized Reader executeCommandReader(String command) throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected!");
        }
        try {
            Channel channel = connectSession.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            InputStreamReader reader = new InputStreamReader(channel.getInputStream());
            channel.connect();
            return reader;
        } catch (JSchException ex) {
            throw new SSHException(ex);
        }
    }

    public synchronized ChannelExec executeCommandChannel(String command) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected!");
        }
        try {
            ChannelExec channel = (ChannelExec)connectSession.openChannel("exec");
            channel.setCommand(command);
            channel.connect();
            return channel;
        } catch (JSchException ex) {
            throw new SSHException(ex);
        }
    }

    public synchronized void disconnect() {
        if (connectSession != null) {
            connectSession.disconnect();
            connectSession = null;
        }
    }

    static class BlindHostKeyRepository implements HostKeyRepository {

        private static final HostKey[] EMPTY = new HostKey[0];

        @Override
        public int check(String host, byte[] key) {
            return HostKeyRepository.OK;
        }

        @Override
        public void add(HostKey hostkey, UserInfo ui) {
        }

        @Override
        public void remove(String host, String type) {
        }

        @Override
        public void remove(String host, String type, byte[] key) {
        }

        @Override
        public String getKnownHostsRepositoryID() {
            return "";
        }

        @Override
        public HostKey[] getHostKey() {
            return EMPTY;
        }

        @Override
        public HostKey[] getHostKey(String host, String type) {
            return EMPTY;
        }
    }
}
