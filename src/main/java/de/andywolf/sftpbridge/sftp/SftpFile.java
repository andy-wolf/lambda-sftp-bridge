package de.andywolf.sftpbridge.sftp;

import de.andywolf.sftpbridge.RuntimeIOException;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.util.Utils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.*;
import java.util.EnumSet;

import static de.andywolf.sftpbridge.ConnectionOptions.*;
import static java.lang.String.format;


/**
 * A file on a host connected through SSH that is accessed using SFTP.
 */
@Slf4j
@ToString
@EqualsAndHashCode
class SftpFile implements de.andywolf.sftpbridge.base.File {

    @Getter
    @NonNull
    protected final SftpConnection connection;

    @Getter
    @NonNull
    protected final Directory directory;

    @Getter
    @NonNull
    protected final String fileName;

    public SftpFile(SftpConnection connection, SftpDirectory directory, String fileName) {
        this.connection = connection;
        this.directory = directory;
        this.fileName = fileName;
    }

    @Override
    public String getFullFilePath() {
        return Utils.constructPath(getDirectory(), fileName);
    }

    @Override
    public InputStream getInputStream() {
        log.debug("Opening SFTP input stream for {}", this);

        try {
            //connection.connect();
            final SFTPClient sftp = connection.getSharedSftpClient();
            final RemoteFile remoteFile = sftp.open(getFullFilePath(), EnumSet.of(OpenMode.READ));
            final InputStream wrapped = remoteFile.new RemoteFileInputStream();

            InputStream is = new InputStream() {

                @Override
                public int read() throws IOException {
                    return wrapped.read();
                }

                @Override
                public int read(byte[] b) throws IOException {
                    return wrapped.read(b);
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return wrapped.read(b, off, len);
                }

                @Override
                public long skip(long n) throws IOException {
                    return wrapped.skip(n);
                }

                @Override
                public int available() throws IOException {
                    return wrapped.available();
                }

                @Override
                public boolean markSupported() {
                    return wrapped.markSupported();
                }

                @Override
                public void mark(int readlimit) {
                    wrapped.mark(readlimit);
                }

                @Override
                public void reset() throws IOException {
                    wrapped.reset();
                }

                @Override
                public void close() throws IOException {
                    log.info("Closing SFTP input stream for {}", SftpFile.this);
                    try {
                        wrapped.close();
                    } finally {
                        try {
                            remoteFile.close();
                        } catch (IOException e) {
                            log.warn("IOException while closing remote file", e);
                        }
                        connection.close();
                    }
                }
            };

            int streamBufferSize = connection.getStreamBufferSize();
            log.debug("Using buffer of size [{}] for streaming from [{}]", streamBufferSize, this);
            return new BufferedInputStream(is, streamBufferSize);
        } catch (IOException e) {
            throw new RuntimeIOException("Cannot read from file " + this, e);
        }
    }

    @Override
    public OutputStream getOutputStream() {
        log.debug("Opening SFTP ouput stream for {}", this);

        try {
            //connection.connect();
            final SFTPClient sftp = connection.getSharedSftpClient();
            final RemoteFile remoteFile = sftp.open(getFullFilePath(), EnumSet.of(OpenMode.CREAT, OpenMode.WRITE, OpenMode.TRUNC));
            final OutputStream wrapped = remoteFile.new RemoteFileOutputStream();

            OutputStream os = new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    wrapped.write(b);
                }

                @Override
                public void write(byte[] b) throws IOException {
                    wrapped.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    wrapped.write(b, off, len);
                }

                @Override
                public void flush() throws IOException {
                    wrapped.flush();
                }

                @Override
                public void close() throws IOException {
                    log.info("Closing SFTP output stream for {}", SftpFile.this);
                    try {
                        wrapped.close();
                    } finally {
                        try {
                            remoteFile.close();
                        } catch (IOException e) {
                            log.warn("IOException while closing remote file", e);
                        }
                        connection.close();
                    }
                }
            };

            int streamBufferSize = connection.getStreamBufferSize();
            log.debug("Using buffer of size [{}] for streaming to [{}]", streamBufferSize, this);
            return new BufferedOutputStream(os, streamBufferSize);
        } catch (IOException e) {
            throw new RuntimeIOException(format("Cannot write to %s", this), e);
        }
    }

    @Override
    public boolean exists() {
        log.debug("Checking {} for existence", this);

        try {
            return connection.getSharedSftpClient().statExistence(getFullFilePath()) != null;
        } catch (IOException e) {
            throw new RuntimeIOException(format("Cannot check existence of file %s", this), e);
        }
    }


    // Deleting

    @Override
    public void delete() {
        if (exists()) {
            log.debug("Deleting file {}", this);

            try {
                connection.getSharedSftpClient().rm(getFullFilePath());
            } catch (IOException e) {
                throw new RuntimeIOException(format("Cannot delete file %s", this), e);
            }
        }
    }

}
