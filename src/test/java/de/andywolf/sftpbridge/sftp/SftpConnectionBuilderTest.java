package de.andywolf.sftpbridge.sftp;

import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.s3.S3ConnectionBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
@Testcontainers
class SftpConnectionBuilderTest {

    @Test
    void testCreateConnection_withOptionsMethod() throws Exception {
        withSftpServer(server -> {
            // given
            int port = server.getPort();

            // when
            Connection connection = new SftpConnectionBuilder()
                    .withOption(ConnectionOptions.ADDRESS, "localhost")
                    .withOption(ConnectionOptions.PORT, port)
                    .withOption(ConnectionOptions.USERNAME, "foo")
                    .withOption(ConnectionOptions.PASSWORD, "bar")
                    .build();

            // then
            assertNotNull(connection);

            connection.close();
        });
    }

    @Test
    void testCreateConnection_withOptionsParam() throws Exception {
        withSftpServer(server -> {
            // given
            int port = server.getPort();

            ConnectionOptions options = new ConnectionOptions();
            options.set(ConnectionOptions.ADDRESS, "localhost");
            options.set(ConnectionOptions.PORT, port);
            options.set(ConnectionOptions.USERNAME, "foo");
            options.set(ConnectionOptions.PASSWORD, "bar");

            // when
            Connection connection = new SftpConnectionBuilder(options).build();

            // then
            assertNotNull(connection);

            connection.close();
        });
    }


    @AfterAll
    static void afterAll() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
        });
    }
}