package de.andywolf.sftpbridge;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import de.andywolf.sftpbridge.s3.S3ConnectionBuilder;
import de.andywolf.sftpbridge.sftp.SftpConnectionBuilder;
import de.andywolf.sftpbridge.util.FileCopier;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.util.function.Consumer;

@Slf4j
@SpringBootApplication
//public class Application implements ApplicationContextInitializer<GenericApplicationContext> {
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Value(value = "${TARGET_URI}")
    private String targetURI;
    @Value(value = "${TARGET_USER}")
    private String targetUser;
    @Value(value = "${TARGET_PASS}")
    private String targetPassword;
    @Value(value = "${TARGET_PRIVATE_KEY}")
    private String targetPrivateKey;

    @Value(value = "${SOURCE_URI}")
    private String sourceURI;
    @Value(value = "${SOURCE_USER}")
    private String sourceUser;
    @Value(value = "${SOURCE_PASS}")
    private String sourcePassword;
    @Value(value = "${SOURCE_PRIVATE_KEY}")
    private String sourcePrivateKey;

    /**
     * Moves one single file from an S3 bucket to an SFTP target
     * Expects only TARGET_* in environment variables
     */
    @Bean
    public Consumer<S3Event> s3EventFunction() {
        return s3Event -> {
            log.debug("TARGET_URI: " + targetURI + ", TARGET_USER: " + targetUser);

            String sourceBucketName = s3Event.getRecords().get(0).getS3().getBucket().getName();
            String sourceObjectKey = s3Event.getRecords().get(0).getS3().getObject().getKey();

            String s3SourceURI = "s3://" + sourceBucketName + "/" + sourceObjectKey;
            log.info("S3 Bucket event for {}", s3SourceURI);
            log.info("Uploading {} to {}", s3SourceURI, targetURI);

            copyS3ToSftp(URI.create(s3SourceURI), URI.create(targetURI), targetUser, targetPassword);
        };
    }

    /**
     * Moves files recursively from an SFTP source to an S3 target
     * Expects TARGET_URI and SOURCE_* in environment variables
     */
    @Bean
    public Consumer<ScheduledEvent> scheduledFunction() {
        return scheduledEvent -> {
            log.debug("SOURCE_URI: " + sourceURI + ", SOURCE_USER: " + sourceUser + ", TARGET_URI: " + targetURI);

            DateTime dateTime = scheduledEvent.getTime();
            String id = scheduledEvent.getId();

            log.info("Scheduled event {} at {}", id, dateTime);
            log.info("Uploading {} to {}", sourceURI, targetURI);

            copySftpToS3(URI.create(sourceURI), sourceUser, sourcePassword, URI.create(targetURI));
        };
    }

    private void copyS3ToSftp(URI sourceURI, URI targetURI, String targetUser, String targetPassword) {
        // Source
        String sourcePath = sourceURI.getPath();
        String sourceDirectory = extractDirectory(sourcePath);
        String sourceFilename = extractFilename(sourcePath);

        Connection s3Connection = getS3Connection(sourceURI.getHost());
        Directory s3SourceDirectory = s3Connection.getDirectory(sourceDirectory);
        File s3SourceFile = s3SourceDirectory.getFile(sourceFilename);

        // Target
        String targetPath = targetURI.getPath();
        String targetDirectory = extractDirectory(targetPath);

        Connection sftpConnection = getSftpConnection(targetURI.getHost(), targetURI.getPort(), targetUser, targetPassword);
        Directory sftpTargetDirectory = sftpConnection.getDirectory(targetDirectory);

        // Copy source file to target directory
        log.debug("S3 source file: {}", s3SourceFile);
        log.debug("SFTP target directory: {}", sftpTargetDirectory);

        FileCopier.copy(s3SourceFile, sftpTargetDirectory);

        // Close connections
        s3Connection.close();
        sftpConnection.close();
    }

    private void copySftpToS3(URI sourceURI, String sourceUser, String sourcePassword, URI targetURI) {
        // Source
        String sourcePath = sourceURI.getPath();
        String sourceDirectory = extractDirectory(sourcePath);
        String sourceFilename = extractFilename(sourcePath);

        Connection sftpSourceConnection = getSftpConnection(sourceURI.getHost(), sourceURI.getPort(), sourceUser, sourcePassword);
        Directory sftpSourceDirectory = sftpSourceConnection.getDirectory(sourceDirectory);

        // Target
        String targetPath = targetURI.getPath();
        String targetDirectory = extractDirectory(targetPath);

        Connection s3TargetConnection = getS3Connection(targetURI.getHost());
        Directory s3TargetDirectory = s3TargetConnection.getDirectory(targetDirectory);

        // Copy source to target directory recursively
        log.debug("SFTP source directory: {}", sftpSourceDirectory);
        log.debug("S3 target directory: {}", s3TargetDirectory);

        if(sourceFilename.isEmpty()) {
            FileCopier.copy(sftpSourceDirectory, s3TargetDirectory);
        }
        else {
            File sftpSourceFile = sftpSourceDirectory.getFile(sourceFilename);
            log.debug("SFTP source file: {}", sftpSourceFile);
            FileCopier.copy(sftpSourceFile, s3TargetDirectory);
        }

        // Close connections
        s3TargetConnection.close();
        sftpSourceConnection.close();
    }

    private String extractFilename(String path) {
        String filename = path.substring(path.lastIndexOf("/"));

        log.debug("File: " + filename);

        return filename;
    }

    private String extractDirectory(String path) {
        String directory = path.substring(0, path.lastIndexOf("/"));

        log.debug("Directory: " + directory);

        return directory;
    }

    private Connection getS3Connection(String bucketName) {
        ConnectionOptions s3Options = new ConnectionOptions();

        s3Options.set(ConnectionOptions.ADDRESS, bucketName);

        return new S3ConnectionBuilder(s3Options).build();
    }

    private Connection getSftpConnection(String host, int port, String user, String password) {
        ConnectionOptions sftpOptions = new ConnectionOptions();

        sftpOptions.set(ConnectionOptions.ADDRESS, host);
        sftpOptions.set(ConnectionOptions.PORT, port);
        sftpOptions.set(ConnectionOptions.USERNAME, user);
        sftpOptions.set(ConnectionOptions.PASSWORD, password);

        return new SftpConnectionBuilder(sftpOptions).build();
    }

/*
    @Override
    public void initialize(GenericApplicationContext applicationContext) {
        applicationContext.registerBean("s3EventFunction", FunctionRegistration.class,
                () -> new FunctionRegistration<Consumer<S3Event>>(s3EventFunction())
                        .type(FunctionType.from(S3Event.class).getType()));
        applicationContext.registerBean("scheduledFunction", FunctionRegistration.class,
                () -> new FunctionRegistration<Consumer<ScheduledEvent>>(scheduledFunction())
                        .type(FunctionType.from(ScheduledEvent.class).getType()));
    }
*/
}
