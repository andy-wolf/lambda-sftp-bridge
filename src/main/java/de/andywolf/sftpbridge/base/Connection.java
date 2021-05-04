package de.andywolf.sftpbridge.base;

import java.io.Closeable;

public interface Connection extends Closeable {


    /**
     * Opens the connection
     */
    void connect();

    /**
     * Closes the connection.
     */
    @Override
    void close();

    /**
     * Creates a reference to a file in a directory on the host.
     *
     * @param parent the reference to the directory on the host
     * @param child  the name of the file in the directory
     * @return a reference to the file in the directory
     */
    File getFile(Directory parent, String child);

    /**
     * Creates a reference to a directory on the host.
     *
     * @param name the reference to the directory on the host
     * @return a reference to the directory
     */
    Directory getDirectory(String name);
}
