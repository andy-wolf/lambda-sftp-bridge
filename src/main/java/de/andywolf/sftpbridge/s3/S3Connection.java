package de.andywolf.sftpbridge.s3;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.RuntimeIOException;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static de.andywolf.sftpbridge.ConnectionOptions.*;

/**
 * Connections to a remote object store using S3.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class S3Connection implements Connection {

    @NonNull
    private final ConnectionOptions options;

    @Getter
    private AmazonS3 s3Client;
    private volatile boolean isConnected;


    // Constructor

    public S3Connection(ConnectionOptions options) {
        this.options = options;
    }

    public String getBucket() {
        return options.get(ADDRESS);
    }

    public int getStreamBufferSize() {
        return options.getInteger(REMOTE_COPY_BUFFER_SIZE, REMOTE_COPY_BUFFER_SIZE_DEFAULT);
    }

    @Override
    public void connect() {
        try {
            String endpointURL = options.get(ENDPOINT_URL, ENDPOINT_URL_DEFAULT);
            String signingRegion = options.get(SIGNING_REGION, SIGNING_REGION_DEFAULT);
            final AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(endpointURL, signingRegion);

            log.debug("Connecting to S3 endpoint " + endpoint);
            s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpoint)
                .withPathStyleAccessEnabled(true)
                .build();

            this.isConnected = true;
        } catch (Exception e) {
            throw new RuntimeIOException("Unexpected exception " + this, e);
        }
    }

    @Override
    public File getFile(Directory parent, String child) {
        if (!(parent instanceof S3ObjectKey)) {
            throw new IllegalStateException("parent is not an object key in an S3 bucket");
        }
        if (parent.getConnection() != this) {
            throw new IllegalStateException("parent is not an object key in this connection");
        }

        return new S3Object(this, (S3ObjectKey) parent, child);
    }

    @Override
    public Directory getDirectory(String name) {
        return new S3ObjectKey(this, name);
    }

    /**
     * Closes the connection.
     */
    @Override
    public final void close() {
        if (!isConnected) {
            return;
        }

        try {
            s3Client.shutdown();
            s3Client = null;
        } finally {
            log.info("Disconnected from {}", this);
            isConnected = false;
        }
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
