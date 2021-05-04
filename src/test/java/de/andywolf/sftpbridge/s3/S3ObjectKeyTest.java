package de.andywolf.sftpbridge.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.base.Directory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
@Testcontainers
class S3ObjectKeyTest {

    private static String BUCKET_NAME_1 = "existing-empty-bucket";
    private static String BUCKET_NAME_2 = "existing-full-bucket";

    public static final String LOCALSTACK_IMAGE_NAME = "localstack/localstack:0.12.6";
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse(LOCALSTACK_IMAGE_NAME);
    private static final int LOCALSTACK_PORT = 4566;

    @Container
    private static final LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE)
        .withServices(S3)
        .withExposedPorts(LOCALSTACK_PORT)
        .withLogConsumer(new Slf4jLogConsumer(log));

    private static final ConnectionOptions options = new ConnectionOptions();

    @BeforeAll
    static void beforeAll() {
        AmazonS3 s3Client = AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .withPathStyleAccessEnabled(true)
            .build();

        s3Client.createBucket(BUCKET_NAME_1);
        s3Client.createBucket(BUCKET_NAME_2);

        s3Client.shutdown();

        URI endpoint = localstack.getEndpointOverride(S3);

        options.set(ConnectionOptions.ADDRESS, BUCKET_NAME_1);
        options.set(ConnectionOptions.ENDPOINT_URL, endpoint.toString());
    }

    @Test
    void testCreateDirectory_nonExistingDirectory() {
        // given
        Connection connection = new S3ConnectionBuilder(options).build();

        // when
        Directory directory = connection.getDirectory("/my/test/folder");
        directory.mkdir();

        // then
        boolean exists = directory.exists();

        assertNotNull(directory);
        assertTrue(exists);

        connection.close();
    }

    @Test
    void testCreateDirectory_existingDirectory() {
        // given
        Connection connection = new S3ConnectionBuilder(options).build();

        Directory directory = connection.getDirectory("/my/test/folder");
        directory.mkdir();

        // when
        directory.mkdir();

        // then
        boolean exists = directory.exists();

        assertNotNull(directory);
        assertTrue(exists);

        connection.close();
    }

    @Test
    void testDeleteDirectory_existingDirectory() {
        // given
        Connection connection = new S3ConnectionBuilder(options).build();

        Directory directory = connection.getDirectory("/my/test/folder");
        directory.mkdir();

        // when
        directory.delete();

        // then
        boolean exists = directory.exists();

        assertNotNull(directory);
        assertFalse(exists);

        connection.close();
    }

    @Test
    void testDeleteDirectory_nonExistingDirectory() {
        // given
        String directoryName = generateRandomAlphanumericString(12L);

        Connection connection = new S3ConnectionBuilder(options).build();

        Directory directory = connection.getDirectory(directoryName);

        // when
        directory.delete();

        // then
        boolean exists = directory.exists();

        assertNotNull(directory);
        assertFalse(exists);

        connection.close();
    }

    @Test
    void testExistsDirectory_existingDirectory() {
        // given
        Connection connection = new S3ConnectionBuilder(options).build();

        Directory directory = connection.getDirectory("/my/test/folder");
        directory.mkdir();

        // when
        boolean exists = directory.exists();

        // then
        assertNotNull(directory);
        assertTrue(exists);

        connection.close();
    }

    @Test
    void testExistsDirectory_nonExistingDirectory() {
        // given
        String directoryName = generateRandomAlphanumericString(12L);

        Connection connection = new S3ConnectionBuilder(options).build();

        Directory directory = connection.getDirectory(directoryName);

        // when
        boolean exists = directory.exists();

        // then
        assertNotNull(directory);
        assertFalse(exists);

        connection.close();
    }

    private String generateRandomAlphanumericString(final long length) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }
}
