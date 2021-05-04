package de.andywolf.sftpbridge.util;

import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Abstract class that walks through a directory hierarchy and provides subclasses with convenient hooks to add specific
 * behaviour.
 * <p/>
 * This class operates with a maximum depth to limit the files and direcories visited.
 * <p/>
 */
@Slf4j
@AllArgsConstructor
public abstract class FileDirectoryWalker {

    /**
     * The directory level representing the starting directory = 0
     */
    public static final int ROOT = 0;

    /**
     * The limit on the directory depth to walk.
     */
    private final int depthLimit;

    /**
     * Construct an instance with unlimited <i>depth</i>.
     */
    protected FileDirectoryWalker() {
        this(-1);
    }

    /**
     * Main recursive method to examine the directory hierarchy.
     *
     * @param directory the directory to examine, not null
     * @param depth     the directory level (starting directory = 0)
     */
    protected void walk(@NonNull Directory directory, int depth) {
        handleDirectoryStart(directory, depth);

        int childDepth = depth + 1;

        if (depthLimit < 0 || childDepth <= depthLimit) {
            List<Directory> childDirs = listSubDirectories(directory);
            for (Directory childDir : childDirs) {
                walk(childDir, childDepth);
            }

            List<File> childFiles = listFiles(directory);
            for (File childFile : childFiles) {
                handleFile(childFile, childDepth);
            }
        }

        handleDirectoryEnd(directory, depth);
    }

    /**
     * Overridable callback method invoked at the start of processing each directory.
     * <p/>
     * This implementation does nothing.
     *
     * @param directory the current directory being processed
     * @param depth     the current directory level (starting directory = 0)
     */
    protected void handleDirectoryStart(@NonNull Directory directory, int depth) {
        // do nothing - overridable by subclass
    }

    /**
     * Overridable callback method invoked for each (non-directory) file.
     * <p/>
     * This implementation does nothing.
     *
     * @param file  the current file being processed
     * @param depth the current directory level (starting directory = 0)
     */
    protected void handleFile(@NonNull File file, int depth) {
        // do nothing - overridable by subclass
    }

    /**
     * Overridable callback method invoked at the end of processing each directory.
     * <p/>
     * This implementation does nothing.
     *
     * @param directory the directory being processed
     * @param depth     the current directory level (starting directory = 0)
     */
    protected void handleDirectoryEnd(@NonNull Directory directory, int depth) {
        // do nothing - overridable by subclass
    }

    /**
     * Lists the files in the directory.
     *
     * @param directory in which to list files.
     * @return all the files in the directory as filtering.
     */
    protected List<File> listFiles(@NonNull Directory directory) {
        return directory.listFiles();
    }

    /**
     * Lists the sub-directories in the directory.
     *
     * @param directory in which to list sub-directories.
     * @return all the sub-directories in the directory as filtering.
     */
    protected List<Directory> listSubDirectories(@NonNull Directory directory) {
        return directory.listSubDirectories();
    }
}
