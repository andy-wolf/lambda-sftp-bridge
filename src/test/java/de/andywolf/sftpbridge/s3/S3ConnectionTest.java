package de.andywolf.sftpbridge.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.base.Connection;
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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
@Testcontainers
class S3ConnectionTest {

    private static final String BUCKET_NAME_1 = "existing-empty-bucket";
    private static final String BUCKET_NAME_2 = "existing-full-bucket";

    public static final String LOCALSTACK_IMAGE_NAME = "localstack/localstack:0.12.6";
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse(LOCALSTACK_IMAGE_NAME);
    private static final int LOCALSTACK_PORT = 4566;

    @Container
    private static final LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE)
        .withServices(S3)
        .withExposedPorts(LOCALSTACK_PORT)
        .withLogConsumer(new Slf4jLogConsumer(log));

    private static final ConnectionOptions options = new ConnectionOptions();

    private static URI endpoint;

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

        endpoint = localstack.getEndpointOverride(S3);

        options.set(ConnectionOptions.ADDRESS, BUCKET_NAME_1);
        options.set(ConnectionOptions.ENDPOINT_URL, endpoint.toString());
    }

    @Test
    void testConnection_existingBucket() {
        // when
        Connection connection = new S3ConnectionBuilder(options).build();

        // then
        assertNotNull(connection);

        connection.close();
    }

    @Test
    void testConnection_nonExistingBucket() {
        // given
        String bucketName = generateRandomAlphanumericString(10L);

        // when
        Connection connection = new S3ConnectionBuilder()
            .withOption(ConnectionOptions.ADDRESS, bucketName)
            .withOption(ConnectionOptions.ENDPOINT_URL, endpoint.toString())
            .build();

        // then
        // FIXME: Builder should fail for non-existing bucket
        assertNotNull(connection);

        connection.close();
    }

    @Test
    void testSeparateConnections() {
        // when
        Connection connection1 = new S3ConnectionBuilder(options).build();
        Connection connection2 = new S3ConnectionBuilder(options).build();

        // then
        assertNotNull(connection1);
        assertNotNull(connection2);
        assertNotEquals(connection1, connection2);

        connection2.close();
        connection1.close();
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
