package de.andywolf.sftpbridge.sftp;

import de.andywolf.sftpbridge.RuntimeIOException;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import de.andywolf.sftpbridge.util.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.sftp.RemoteResourceInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;


/**
 * A directory on a host connected through SSH that is accessed using SFTP.
 */
@Slf4j
@ToString
@EqualsAndHashCode
class SftpDirectory implements Directory {

    @Getter
    @NonNull
    protected final List<String> pathComponents;

    @Getter
    @NonNull
    protected final SftpConnection connection;

    // Contructors

    public SftpDirectory(SftpConnection connection, String path) {
        this.connection = connection;
        this.pathComponents = Utils.splitPath(path);
    }


    // Getters

    @Override
    public String getDirectoryName() {
        if (pathComponents.isEmpty()) {
            return Utils.PATH_SEPARATOR;
        } else {
            return pathComponents.get(pathComponents.size() - 1);
        }
    }

    @Override
    public Directory getSubDirectory(String name) {
        return new SftpDirectory(this.connection, Utils.constructPath(this, name));
    }

    @Override
    public String getFullDirectoryPath() {
        return Utils.joinPath(pathComponents);
    }

    @Override
    public boolean exists() {
        log.debug("Checking {} for existence", this);

        try {
            return connection.getSharedSftpClient().statExistence(getFullDirectoryPath()) != null;
        } catch (IOException e) {
            throw new RuntimeIOException(format("Cannot check existence of file %s", this), e);
        }
    }


    // Listing

    @Override
    public List<File> listFiles() {
        log.debug("Listing files in directory {}", this);

        try {
            // read files from host
            List<RemoteResourceInfo> ls = connection.getSharedSftpClient().ls(getFullDirectoryPath());

            // copy files to list, skipping . and ..
            List<File> files = new ArrayList<>();
            for (RemoteResourceInfo l : ls) {

                // Skipping sub-directories
                if(l.isRegularFile()) {
                    String filename = l.getName();
                    if (filename.equals(".") || filename.equals("..")) {
                        continue;
                    }
                    files.add(getFile(filename));
                }
            }

            return files;
        } catch (IOException e) {
            throw new RuntimeIOException(format("Cannot list files in directory %s", this), e);
        }
    }

    @Override
    public List<Directory> listSubDirectories() {
        log.debug("Listing sub-directories in directory {}", this);

        try {
            // read files from host
            List<RemoteResourceInfo> ls = connection.getSharedSftpClient().ls(getFullDirectoryPath());

            // copy files to list, skipping . and ..
            List<Directory> directories = new ArrayList<>();
            for (RemoteResourceInfo l : ls) {

                // Skipping sub-directories
                if(l.isDirectory()) {
                    String dirname = l.getName();
                    directories.add(getSubDirectory(dirname));
                }
            }

            return directories;
        } catch (IOException e) {
            throw new RuntimeIOException(format("Cannot list sub-directories in directory %s", this), e);
        }
    }


    // Deleting

    @Override
    public void delete() {
        if (exists()) {
            log.debug("Deleting directory {}", this);

            try {
                connection.getSharedSftpClient().rmdir(getFullDirectoryPath());
            } catch (IOException e) {
                throw new RuntimeIOException(format("Cannot delete directory %s", this), e);
            }
        }
    }

    // Creating directories

    @Override
    public void mkdir() {
        log.debug("Creating directory {}", this);

        try {
            connection.getSharedSftpClient().mkdir(getFullDirectoryPath());
        } catch (IOException e) {
            throw new RuntimeIOException(format("Cannot create directory %s", this), e);
        }
    }

    @Override
    public File getFile(String name) {
        return connection.getFile(this, name);
    }

    @Override
    public void deleteRecursively() {
        RuntimeIOException accumulator = new RuntimeIOException("Cannot delete " + this + ", not all children are deleted.");
        for (File each : listFiles()) {
            try {
                each.delete();
            } catch (RuntimeIOException rio) {
                log.warn("Unable to delete child {}. Continue...", each);
                accumulator.addSuppressed(rio);
            }
        }

        for (Directory each : listSubDirectories()) {
            try {
                each.deleteRecursively();
            } catch (RuntimeIOException rio) {
                log.warn("Unable to delete child {}. Continue...", each);
                accumulator.addSuppressed(rio);
            }
        }

        Throwable[] suppressed = accumulator.getSuppressed();
        if (suppressed == null || suppressed.length == 0) {
            delete();
        } else {
            throw accumulator;
        }
    }
}
