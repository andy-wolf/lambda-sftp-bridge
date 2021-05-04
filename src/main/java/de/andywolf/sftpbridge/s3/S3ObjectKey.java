package de.andywolf.sftpbridge.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.*;
import de.andywolf.sftpbridge.RuntimeIOException;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import de.andywolf.sftpbridge.util.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;


/**
 * A key of an object in an S3 bucket.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class S3ObjectKey implements Directory {

    @Getter
    @NonNull
    protected final List<String> pathComponents;

    @Getter
    @NonNull
    protected final S3Connection connection;

    // Contructor

    public S3ObjectKey(S3Connection connection, String path) {
        this.connection = connection;
        this.pathComponents = Utils.splitPath(path);
    }


    // Getters

    @Override
    public String getDirectoryName() {
        if (pathComponents.isEmpty()) {
            return Utils.PATH_SEPARATOR;
        } else {
            return pathComponents.get(pathComponents.size() - 1);
        }
    }

    @Override
    public Directory getSubDirectory(String name) {
        return new S3ObjectKey(this.connection, getFullDirectoryPath() + name);
    }

    @Override
    public String getFullDirectoryPath() {
        // Strip slash prefix usually used with file paths
        return Utils.joinPath(pathComponents).substring(1) + Utils.PATH_SEPARATOR;
    }


    @Override
    public boolean exists() {
        log.debug("Checking {} for existence", this);

        ListObjectsV2Result result = null;
        try {
            ListObjectsV2Request request = new ListObjectsV2Request();
            request.setBucketName(connection.getBucket());
            request.setPrefix(getFullDirectoryPath());

            result = connection.getS3Client().listObjectsV2(request);
        }
        catch (AmazonServiceException e) {
            String errorCode = e.getErrorCode();
            if (!errorCode.equals("NoSuchKey")) {
                throw e;
            }
            log.debug("Object does not exist: {}", this);
        }

        return result.getKeyCount() > 0;
    }


    // Listing

    @Override
    public List<File> listFiles() {
        log.debug("Listing directory {}", this);

        List<File> files = new ArrayList<>();

        try {

            ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(connection.getBucket())
                .withPrefix(getFullDirectoryPath())
                .withDelimiter(Utils.PATH_SEPARATOR)
                .withMaxKeys(10);

            ListObjectsV2Result result;

            do {
                result = connection.getS3Client().listObjectsV2(req);

                for(S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    String name = objectSummary.getKey().substring(getFullDirectoryPath().length());
                    if(name.length() > 0) {
                        File file = getFile(name);
                        files.add(file);
                    }
                }

                String token = result.getNextContinuationToken();
                req.setContinuationToken(token);

            } while (result.isTruncated());
        } catch (SdkClientException e) {
            throw new RuntimeIOException(format("Cannot list directory %s", this), e);
        }

        return files;
    }

    @Override
    public List<Directory> listSubDirectories() {
        log.debug("Listing directory {}", this);

        List<Directory> directories = new ArrayList<>();

        try {

            ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(connection.getBucket())
                    .withPrefix(getFullDirectoryPath())
                    .withDelimiter(Utils.PATH_SEPARATOR)
                    .withMaxKeys(10);

            ListObjectsV2Result result;

            do {
                result = connection.getS3Client().listObjectsV2(req);

                for (String prefix : result.getCommonPrefixes()) {
                    String name = prefix.substring(getFullDirectoryPath().length());
                    if (name.length() > 0) {
                        Directory subDirectory = getSubDirectory(name);
                        directories.add(subDirectory);
                    }
                }

                String token = result.getNextContinuationToken();
                req.setContinuationToken(token);

            } while (result.isTruncated());
        } catch (SdkClientException e) {
            throw new RuntimeIOException(format("Cannot list directory %s", this), e);
        }

        return directories;
    }


    // Deleting

    @Override
    public void delete() {
        if (exists()) {
            log.debug("Deleting object {}", this);

            // TODO: Check exception handling best practices for AWS SDK
            connection.getS3Client().deleteObject(connection.getBucket(), getFullDirectoryPath());
        }
    }


    // Creating directories

    @Override
    public void mkdir() {
        log.debug("Creating directory {}", this);

        // create meta-data for your folder and set content-length to 0
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        metadata.setContentType("application/x-directory");

        // create empty content
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

        // create a PutObjectRequest passing the folder name suffixed by /
        PutObjectRequest putObjectRequest = new PutObjectRequest(
            connection.getBucket(),
            getFullDirectoryPath(),
            emptyContent,
            metadata
        );

        // send request to S3 to create folder
        connection.getS3Client().putObject(putObjectRequest);
    }

    @Override
    public File getFile(String name) {
        return connection.getFile(this, name);
    }

    @Override
    public void deleteRecursively() {
        RuntimeIOException accumulator = new RuntimeIOException("Cannot delete " + this + ", not all children are deleted.");
        for (File each : listFiles()) {
            try {
                each.delete();
            } catch (RuntimeIOException rio) {
                log.warn("Unable to delete child {}. Continue...", each);
                accumulator.addSuppressed(rio);
            }
        }

        for (Directory each : listSubDirectories()) {
            try {
                each.deleteRecursively();
            } catch (RuntimeIOException rio) {
                log.warn("Unable to delete child {}. Continue...", each);
                accumulator.addSuppressed(rio);
            }
        }

        Throwable[] suppressed = accumulator.getSuppressed();
        if (suppressed == null || suppressed.length == 0) {
            delete();
        } else {
            throw accumulator;
        }
    }
}
