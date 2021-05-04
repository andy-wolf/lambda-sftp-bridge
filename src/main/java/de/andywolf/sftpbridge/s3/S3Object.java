package de.andywolf.sftpbridge.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import de.andywolf.sftpbridge.RuntimeIOException;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.*;


/**
 * An object in an S3 bucket
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class S3Object implements File {

    @Getter
    @NonNull
    protected final S3Connection connection;

    @Getter
    @NonNull
    protected final Directory directory;

    @Getter
    @NonNull
    protected final String fileName;

    public S3Object(S3Connection connection, S3ObjectKey objectKey, String fileName) {
        this.connection = connection;
        this.directory = objectKey;
        this.fileName = fileName;
    }

    @Override
    public String getFullFilePath() {
        // Strip slash prefix usually used with file paths
        return getDirectory().getFullDirectoryPath() + fileName;
    }

    @Override
    public InputStream getInputStream() {
        log.debug("Opening S3 input stream for {}", this);

        final AmazonS3 s3Client = connection.getS3Client();
        final com.amazonaws.services.s3.model.S3Object s3Object = s3Client.getObject(connection.getBucket(), getFullFilePath());
        final InputStream in = s3Object.getObjectContent();

        InputStream is = new InputStream() {

            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return in.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return in.read(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return in.skip(n);
            }

            @Override
            public int available() throws IOException {
                return in.available();
            }

            @Override
            public boolean markSupported() {
                return in.markSupported();
            }

            @Override
            public void mark(int readlimit) {
                in.mark(readlimit);
            }

            @Override
            public void reset() throws IOException {
                in.reset();
            }

            @Override
            public void close() throws IOException {
                log.debug("Closing S3 input stream for {}", S3Object.this);
                try {
                    in.close();
                } finally {
                    try {
                        s3Object.close();
                    } catch (IOException e) {
                        log.warn("IOException while closing S3 object", e);
                    }
                }
            }
        };

        int streamBufferSize = connection.getStreamBufferSize();
        log.debug("Using buffer of size [{}] for streaming from [{}]", streamBufferSize, this);
        return new BufferedInputStream(is, streamBufferSize);
    }

    @Override
    public OutputStream getOutputStream() {
        log.debug("Opening S3 ouput stream for {}", this);

        final AmazonS3 s3Client = connection.getS3Client();
        OutputStream out = new S3OutputStream(s3Client, connection.getBucket(), getFullFilePath());

        OutputStream os = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                log.debug("Closing S3 output stream for {}", S3Object.this);
                out.close();
            }
        };

        int streamBufferSize = connection.getStreamBufferSize();
        log.debug("Using buffer of size [{}] for streaming to [{}]", streamBufferSize, this);
        return new BufferedOutputStream(os, streamBufferSize);

    }

    @Override
    public boolean exists() {
        log.debug("Checking {} for existence", this);

        boolean objectExists = false;
        try {
            objectExists = connection.getS3Client().doesObjectExist(connection.getBucket(), getFullFilePath());
        }
        catch (AmazonServiceException e) {
            String errorCode = e.getErrorCode();
            if (!errorCode.equals("NoSuchKey")) {
                throw e;
            }
            log.debug("Object does not exist: {}", this);
        }

        return objectExists;
    }


    // Deleting

    @Override
    public void delete() {
        if (exists()) {
            log.debug("Deleting object {}", this);

            try {
                connection.getS3Client().deleteObject(connection.getBucket(), getFullFilePath());
            }
            catch(AmazonServiceException e) {
                throw new RuntimeIOException("Deleting object failed", e);
            }
        }
    }

}
