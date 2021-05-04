package de.andywolf.sftpbridge.s3;

import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.base.ConnectionBuilder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


/**
 * Builds SFTP connections.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class S3ConnectionBuilder implements ConnectionBuilder {

    private static final ConnectionOptions defaultOptions = new ConnectionOptions();

    private final ConnectionOptions options;

    public S3ConnectionBuilder() {
        this(defaultOptions);
    }

    public S3ConnectionBuilder(final ConnectionOptions options) {
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
        S3Connection connection = new S3Connection(options);
        connection.connect();
        return connection;
    }

}
