package de.andywolf.sftpbridge.base;

import de.andywolf.sftpbridge.RuntimeIOException;

import java.io.IOException;
import java.util.List;

/**
 * An abstract representation of a remote directory that can be access through a {@link Connection}.
 * It can also represent non-existent directory.
 * <p/>
 * All methods in this interface may throw a {@link RuntimeIOException} if an error occurs.
 * Checked {@link IOException IOExceptions} are never thrown.
 */
public interface Directory {

    /**
     * Return the connection through which this directory is accessible. If the connection is closed,
     * this directory may no longer be accessible.
     *
     * @return the connection through which this directory is accessible.
     */
    Connection getConnection();

    /**
     * Return the full path of the directory on the remote system.
     *
     * @return the full path of the directory.
     */
    String getFullDirectoryPath();

    /**
     * The name of the directory on the remote system.
     *
     * @return the name of the directory.
     */
    String getDirectoryName();


    /**
     * Returns a reference to a named file of this directory. The child file returned may or may not exist.
     *
     * @param name the name of the file relative to this directory. May not contain path separators.
     * @return the file within this directory.
     */
    File getFile(String name);

    /**
     * Returns a reference to a directory of this directory. The child directory returned may or may not exist.
     *
     * @param name the name of the directory relative to this directory. May not contain path separators.
     * @return the directory within this directory.
     */
    Directory getSubDirectory(String name);

    /**
     * Tests whether the directory represented by this object exists.
     *
     * @return <code>true</code> if and only if this directory exists.
     * @throws RuntimeIOException if an I/O error occured
     */
    boolean exists();

    /**
     * Deletes this directory. If this directory is not empty, a {@link RuntimeIOException} is thrown.
     */
    void delete();

    /**
     * Deletes this directory. If this directory is not empty its contents are deleted first.
     */
    void deleteRecursively();

    /**
     * Lists the files in this directory.
     *
     * @return the files in this directory, in an unspecified order.
     */
    List<File> listFiles();

    /**
     * Lists the sub-directories in this directory.
     *
     * @return the sub-directories in this directory, in an unspecified order.
     */
    List<Directory> listSubDirectories();

    /**
     * Creates this directory. If the parent directory does not exists, a {@link RuntimeIOException} is thrown.
     */
    void mkdir();

}
