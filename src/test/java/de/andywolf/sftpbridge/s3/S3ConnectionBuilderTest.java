package de.andywolf.sftpbridge.s3;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
@Testcontainers
class S3ConnectionBuilderTest {

    private static final String BUCKET_NAME_1 = "existing-empty-bucket";

    public static final String LOCALSTACK_IMAGE_NAME = "localstack/localstack:0.12.6";
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse(LOCALSTACK_IMAGE_NAME);
    private static final int LOCALSTACK_PORT = 4566;

    @Container
    private static final LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(S3)
            .withExposedPorts(LOCALSTACK_PORT)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private static URI endpoint;

    @BeforeAll
    static void beforeAll() {
        endpoint = localstack.getEndpointOverride(S3);
    }

    @Test
    void testCreateConnection_withOptionsParam() {
        // given
        ConnectionOptions options = new ConnectionOptions();
        options.set(ConnectionOptions.ADDRESS, BUCKET_NAME_1);
        options.set(ConnectionOptions.ENDPOINT_URL, endpoint.toString());

        // when
        Connection connection = new S3ConnectionBuilder(options).build();

        // then
        assertNotNull(connection);

        connection.close();
    }

    @Test
    void testCreateConnection_withOptionsMethod() {
        // given

        // when
        Connection connection = new S3ConnectionBuilder()
            .withOption(ConnectionOptions.ADDRESS, BUCKET_NAME_1)
            .withOption(ConnectionOptions.ENDPOINT_URL, endpoint.toString())
            .build();

        // then
        assertNotNull(connection);

        connection.close();
    }
}