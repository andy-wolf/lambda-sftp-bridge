package de.andywolf.sftpbridge.sftp;

import de.andywolf.sftpbridge.ConnectionOptions;
import de.andywolf.sftpbridge.base.Connection;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.util.Random;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.junit.jupiter.api.Assertions.*;

class SftpFileTest {

    @Test
    void testCreateFile() throws Exception {
        withSftpServer(server -> {
            // given
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
            directory.mkdir();

            // when
            File file = connection.getFile(directory, "test.txt");

            byte[] bytesArray = generateRandomBytes(100);
            OutputStream outputStream = file.getOutputStream();
            outputStream.write(bytesArray);
            outputStream.close();

            // then
            assertNotNull(directory);
            assertNotNull(file);

            boolean directoryExists = directory.exists();
            boolean fileExists = file.exists();

            assertTrue(directoryExists);
            assertTrue(fileExists);

            connection.close();
        });
    }

    @Test
    void testDeleteFile() throws Exception {
        withSftpServer(server -> {
            // given
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
            directory.mkdir();

            File file = connection.getFile(directory, "test.txt");

            byte[] bytesArray = generateRandomBytes(100);
            OutputStream outputStream = file.getOutputStream();
            outputStream.write(bytesArray);
            outputStream.close();

            // when
            file.delete();

            // then
            assertNotNull(directory);
            assertNotNull(file);

            boolean directoryExists = directory.exists();
            boolean fileExists = file.exists();

            assertTrue(directoryExists);
            assertFalse(fileExists);

            connection.close();
        });

    }

    protected static byte[] generateRandomBytes(final int size) {
        byte[] randomBytes = new byte[size];
        new Random().nextBytes(randomBytes);
        return randomBytes;
    }
}