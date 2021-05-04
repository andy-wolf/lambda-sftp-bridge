package de.andywolf.sftpbridge.sftp;

import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.base.Connection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SftpConnectionTest {

    @Test
    void testConnect_existingServer() throws Exception {
        withSftpServer(server -> {
            int port = server.getPort();

            Connection connection = new SftpConnectionBuilder()
                .withOption(ConnectionOptions.ADDRESS, "localhost")
                .withOption(ConnectionOptions.PORT, port)
                .withOption(ConnectionOptions.USERNAME, "foo")
                .withOption(ConnectionOptions.PASSWORD, "bar")
                .build();

            assertNotNull(connection);

            connection.close();
        });
    }

    @Test
    void testConnect_nonExistingServer() {
        Connection connection = null;
        try {
            connection = new SftpConnectionBuilder()
                    .withOption(ConnectionOptions.ADDRESS, "localhost")
                    .withOption(ConnectionOptions.PORT, 22)
                    .withOption(ConnectionOptions.USERNAME, "foo")
                    .withOption(ConnectionOptions.PASSWORD, "bar")
                    .build();
        }
        catch (Exception e) { }

        assertNull(connection);
    }

    @AfterAll
    static void afterAll() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
        });
    }

/*
    @Test
    void testTextFile() throws Exception {
        withSftpServer(server -> {
            server.putFile("/directory/file.txt", "content of file", UTF_8);
            //code that reads the file using the SFTP protocol
        });
    }

    @Test
    void testBinaryFile() throws Exception {
        withSftpServer(server -> {
            byte[] content = createContent();
            server.putFile("/directory/file.bin", content);
            //code that reads the file using the SFTP protocol
        });
    }

    @Test
    void testDirectory() throws Exception {
        withSftpServer(server -> {
            server.createDirectory("/a/directory");
            //code that reads from or writes to that directory
        });
    }

    @Test
    void testDirectories() throws Exception {
        withSftpServer(server -> {
            server.createDirectories(
                    "/a/directory",
                    "/another/directory"
            );
            //code that reads from or writes to that directories
        });
    }

    @Test
    void testTextFile2() throws Exception {
        withSftpServer(server -> {
            //code that uploads the file using the SFTP protocol
            String fileContent = server.getFileContent("/directory/file.txt", UTF_8);
            //verify file content
        });
    }

    @Test
    void testBinaryFile2() throws Exception {
        withSftpServer(server -> {
            //code that uploads the file using the SFTP protocol
            byte[] fileContent = server.getFileContent("/directory/file.bin");
            //verify file content
        });
    }

    @Test
    void testFile() throws Exception {
        withSftpServer(server -> {
            //code that uploads or deletes the file
            boolean exists = server.existsFile("/directory/file.txt");
            //check value of exists variable
        });
    }

    @AfterAll
    void afterAll() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
        });
    }

 */
}
