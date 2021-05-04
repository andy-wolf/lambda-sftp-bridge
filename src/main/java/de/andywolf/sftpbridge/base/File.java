package de.andywolf.sftpbridge.base;

import de.andywolf.sftpbridge.RuntimeIOException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An abstract representation of a remote file that can be access through an {@link Connection}.
 * <p/>
 * All methods in this interface may throw a {@link RuntimeIOException} if an error occurs.
 * Checked {@link IOException IOExceptions} are never thrown.
 */
public interface File {

    /**
     * Return the connection through which this file is accessible. If the connection is closed,
     * this file may no longer be accessible.
     *
     * @return the connection through which this file is accessible.
     */
    Connection getConnection();

    /**
     * Return the full path of the file on the remote system.
     *
     * @return the full path of the file.
     */
    String getFullFilePath();

    /**
     * The name of the file on the remote system.
     *
     * @return the name of the file.
     */
    String getFileName();


    /**
     * Tests whether the file represented by this object exists.
     *
     * @return <code>true</code> if and only if this file exists.
     * @throws RuntimeIOException if an I/O error occured
     */
    boolean exists();

    /**
     * Returns an input stream to read from this file. The complete contents of this input stream
     * must be read before another operation on this file or its corresponding {@link Connection}
     * is performed.
     *
     * @return an input stream connected to this file.
     */
    InputStream getInputStream();

    /**
     * Returns an output stream to write to this file. The complete contents of this output stream
     * must be written before another operation on this file or its corresponding {@link Connection}
     * is performed.
     *
     * @return an output stream connected to this file.
     */
    OutputStream getOutputStream();

    /**
     * Deletes this file. If this file is a directory and it is not empty,
     * a {@link RuntimeIOException} is thrown.
     */
    void delete();

}
