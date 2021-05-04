package de.andywolf.sftpbridge.sftp;

import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.RuntimeIOException;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.Config;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.PKCS5KeyFile;
import net.schmizz.sshj.userauth.method.AuthPassword;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static de.andywolf.sftpbridge.ConnectionOptions.*;
import static de.andywolf.sftpbridge.sftp.SftpConnectionBuilder.*;
import static java.lang.String.format;
import static java.net.InetSocketAddress.createUnresolved;

/**
 * Connections to a remote host using SSH w/ SFTP.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class SftpConnection implements Connection {

    private static final Config config = new DefaultConfig();
    private static final Factory<SSHClient> sshClientFactory = () -> new SSHClient(config);

    // Static initialization block
    static {
        // PKCS5 is missing from 0.19.0 SSHJ config.
        List<Factory.Named<FileKeyProvider>> current = config.getFileKeyProviderFactories();
        current = new ArrayList<>(current);
        current.add(new PKCS5KeyFile.Factory());
        config.setFileKeyProviderFactories(current);
    }

    @NonNull
    private final ConnectionOptions options;

    @Getter
    private SFTPClient sharedSftpClient;
    private SSHClient sshClient;

    private volatile boolean isConnected;


    // Constructor

    public SftpConnection(ConnectionOptions options) {
        this.options = options;
    }

    public void connect() {
        try {
            // TODO: Optimize exception handling
            SSHClient client = connnectSSH();
            authenticateSSH(client);
            connectSFTP(client);

            this.sshClient = client;
            this.isConnected = true;
        } catch (SSHException e) {
            throw new RuntimeIOException("Cannot connect to " + this, e);
        }
    }

    private SSHClient connnectSSH() {
        int connectionTimeoutMillis = options.getInteger(CONNECTION_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MILLIS_DEFAULT);
        int socketTimeoutMillis = options.getInteger(SOCKET_TIMEOUT_MILLIS, SOCKET_TIMEOUT_MILLIS_DEFAULT);
        int heartbeatInterval = options.getInteger(HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL_DEFAULT);

        config.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);

        SSHClient client = sshClientFactory.create();
        client.setSocketFactory(SocketFactory.getDefault());
        client.setConnectTimeout(connectionTimeoutMillis);
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.setTimeout(socketTimeoutMillis);
        client.getConnection().getKeepAlive().setKeepAliveInterval(heartbeatInterval);


        String host = options.get(ADDRESS);
        int port = options.getInteger(PORT, PORT_DEFAULT_SSH);

        try {
            client.connect(host, port);
        } catch (IOException e) {
            throw new RuntimeIOException("Cannot connect to " + host + ":" + port, e);
        }

        return client;
    }

    private void authenticateSSH(@NonNull final SSHClient client) throws UserAuthException, TransportException {
        String username = options.get(USERNAME);
        String privateKey = options.getOptional(PRIVATE_KEY);
        String password = options.getOptional(PASSWORD);
        String passphrase = options.getOptional(PASSPHRASE);

        KeyProvider keys;
        if (privateKey != null) {
            try {
                if (passphrase == null) {
                    keys = client.loadKeys(privateKey, null, null);
                } else {
                    keys = client.loadKeys(privateKey, null, getPassphraseFinder());
                }
            } catch (IOException e) {
                throw new RuntimeIOException("The supplied key is not in a recognized format", e);
            }
            client.authPublickey(username, keys);
        } else if (password != null) {
            PasswordFinder passwordFinder = getPasswordFinder();
            client.auth(username, new AuthPassword(passwordFinder));
        } else {
            log.warn("You should either set a private key or a password");
        }
    }

    private void connectSFTP(@NonNull final SSHClient sshClient) {
        if (sharedSftpClient == null) {
            log.debug("Opening SFTP client to {}", this);

            try {
                sharedSftpClient = sshClient.newSFTPClient();
            } catch (IOException e) {
                throw new RuntimeIOException(format("Cannot start SFTP session for %s", this), e);
            }
        }
    }

    /**
     * Closes the connection.
     */
    @Override
    public final void close() {
        if (!isConnected) {
            return;
        }

        if (sharedSftpClient != null) {
            log.debug("Closing SFTP client to {}", this);

            try {
                sharedSftpClient.close();
            } catch (IOException e) {
                log.warn("IOException while closing SFTP client", e);
            }

            sharedSftpClient = null;
        }

        if (sshClient != null) {
            log.debug("Disconnecting SSH connection to {}", this);

            try {
                sshClient.disconnect();
            } catch (Exception e) {
                // Even though we get an exception, we expect the connection to have been closed, so we are ignoring
                log.error("Unexpected exception received while disconnecting from {}: {}", this, e);
            }

            sshClient = null;
        }

        log.info("Disconnected from {}", this);

        isConnected = false;
    }

    @Override
    public File getFile(Directory parent, String child) {
        if (!(parent instanceof SftpDirectory)) {
            throw new IllegalStateException("parent is not a directory on an SSH host");
        }
        if (parent.getConnection() != this) {
            throw new IllegalStateException("parent is not a directory in this connection");
        }

        return new SftpFile(this, (SftpDirectory) parent, child);
    }

    @Override
    public Directory getDirectory(String name) {
        return new SftpDirectory(this, name);
    }

    public int getStreamBufferSize() {
        return options.getInteger(REMOTE_COPY_BUFFER_SIZE, REMOTE_COPY_BUFFER_SIZE_DEFAULT);
    }


    // Utilities

    private PasswordFinder getPasswordFinder() {
        return new PasswordFinder() {
            String password = options.getOptional(PASSWORD);

            @Override
            public char[] reqPassword(Resource<?> resource) {
                return password.toCharArray();
            }

            @Override
            public boolean shouldRetry(Resource<?> resource) {
                return false;
            }
        };
    }

    private PasswordFinder getPassphraseFinder() {
        return new PasswordFinder() {
            String passphrase = options.getOptional(PASSPHRASE);

            @Override
            public char[] reqPassword(Resource<?> resource) {
                return passphrase.toCharArray();
            }

            @Override
            public boolean shouldRetry(Resource<?> resource) {
                return false;
            }
        };
    }

    private boolean onlyOneNotNull(Object... objs) {
        int guard = 0;
        for (Object obj : objs) {
            guard += obj != null ? 1 : 0;
        }
        return guard == 1;
    }


    /**
     * Make sure that the connection is cleaned up. This will log error messages if the connection is collected before it is cleaned up.
     *
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        if (isConnected) {
            log.error("Connection [%s] was not closed, closing automatically.", this);
            this.close();
        }
        super.finalize();
    }
}
