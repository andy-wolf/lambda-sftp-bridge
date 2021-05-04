package de.andywolf.sftpbridge.sftp;

import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.base.Directory;
import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.junit.jupiter.api.Assertions.*;

class SftpDirectoryTest {

    @Test
    void testCreateSingleDirectory_nonExistingDirectory() throws Exception {
        withSftpServer(server -> {
            int port = server.getPort();

            Connection connection = new SftpConnectionBuilder()
                    .withOption(ConnectionOptions.ADDRESS, "localhost")
                    .withOption(ConnectionOptions.PORT, port)
                    .withOption(ConnectionOptions.USERNAME, "foo")
                    .withOption(ConnectionOptions.PASSWORD, "bar")
                    .build();

            // FIXME: Currently only a directory can be created whose parent already exists
            //        Add recursive creation logic
            Directory directory = connection.getDirectory("/my");

            assertNotNull(directory);

            directory.mkdir();
            boolean exists = directory.exists();

            assertTrue(exists);

            connection.close();
        });
    }
}