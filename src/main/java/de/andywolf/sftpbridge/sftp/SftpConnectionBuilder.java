package de.andywolf.sftpbridge.sftp;

import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.base.ConnectionBuilder;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import static de.andywolf.sftpbridge.ConnectionOptions.registerFilteredKey;


/**
 * Builds SFTP connections.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class SftpConnectionBuilder implements ConnectionBuilder {

    private static final ConnectionOptions defaultOptions = new ConnectionOptions();

    private final ConnectionOptions options;

    public SftpConnectionBuilder() {
        this(defaultOptions);
    }

    public SftpConnectionBuilder(final ConnectionOptions options) {
        this.options = options;
    }

    @Override
    public ConnectionBuilder withOption(String key, String value) {
        options.set(key, value);
        return this;
    }

    @Override
    public ConnectionBuilder withOption(String key, int value) {
        options.set(key, value);
        return this;
    }

    @Override
    public Connection build() {
        SftpConnection connection = new SftpConnection(options);
        connection.connect();
        return connection;
    }
}
