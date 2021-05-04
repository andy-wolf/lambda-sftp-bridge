package de.andywolf.sftpbridge.util;

import de.andywolf.sftpbridge.RuntimeIOException;
import de.andywolf.sftpbridge.base.Directory;
import de.andywolf.sftpbridge.base.File;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;


/**
 * File copy utility that uses only the input and output streams exposed by the File to perform the
 * copying action.
 */
@Slf4j
@NoArgsConstructor
public final class FileCopier extends FileDirectoryWalker {

    private final Stack<Directory> dstDirStack = new Stack<>();
    private Directory srcDir;

    private FileCopier(Directory srcDir, Directory dstDir) {
        FileCopier.this.dstDirStack.push(dstDir);
        this.srcDir = srcDir;
        checkDirectoryExists(srcDir);
    }

    /**
     * Copies a file.
     *
     * @param src the source file.
     * @param dst the destination file. Its parent directory must exist.
     */
    public static void copy(File src, File dst) {
        new FileCopier().transmitFile(src, dst);
    }

    /**
     * Copies a file.
     *
     * @param src the source file.
     * @param dst the destination directory.
     */
    public static void copy(File src, Directory dst) {
        File dstFile = dst.getFile(src.getFileName());
        new FileCopier().transmitFile(src, dstFile);
    }

    /**
     * Copies a directory recursively.
     *
     * @param srcDir the source directory. Must exist.
     * @param dstDir the destination directory. May exists. Its parent directory must exist.
     */
    public static void copy(Directory srcDir, Directory dstDir) {
        FileCopier dirCopier = new FileCopier(srcDir, dstDir);
        dirCopier.startTransmission();
    }

    /**
     * Copies a regular file.
     *
     * @param srcFile the source file. Must exists.
     * @param dstFile the destination file. May exists. Its parent directory must exist.
     */
    protected void transmitFile(final File srcFile, final File dstFile) {
        checkFileExists(srcFile);

        log.debug("Copying file {} to {}", srcFile, dstFile);
        if (dstFile.exists())
            log.trace("About to overwrite existing file {}", dstFile);

        try(InputStream is = srcFile.getInputStream();
            OutputStream os = dstFile.getOutputStream()) {
            Utils.write(is, os);
        } catch (RuntimeIOException|IOException exc) {
            throw new RuntimeIOException("Cannot copy " + srcFile + " to " + dstFile, exc.getCause());
        }
    }

    protected void startTransmission() {
        walk(srcDir, 0);
    }

    @Override
    protected void handleDirectoryStart(Directory scrDir, int depth) {
        Directory dstDir = getCurrentDestinationDir();
        if (depth != ROOT) {
            dstDir = createSubdirectoryAndMakeCurrent(dstDir, scrDir.getDirectoryName());
        }

        if (dstDir.exists()) {
            log.trace("About to copy files into existing directory {}", dstDir);
        } else {
            dstDir.mkdir();
        }
    }

    @Override
    protected void handleFile(File srcFile, int depth) {
        File dstFile = getCurrentDestinationDir().getFile(srcFile.getFileName());
        transmitFile(srcFile, dstFile);
    }

    @Override
    protected void handleDirectoryEnd(Directory directory, int depth) {
        if(depth != ROOT) {
            dstDirStack.pop();
        }
    }

    private Directory createSubdirectoryAndMakeCurrent(Directory parentDir, String subdirName) {
        Directory subdir = parentDir.getSubDirectory(subdirName);
        dstDirStack.push(subdir);
        return subdir;
    }

    private Directory getCurrentDestinationDir() {
        return dstDirStack.peek();
    }

    /**
     * Assert that the directory exists.
     *
     * @param dir            is the directory to check.
     * @throws RuntimeIOException if directory does not exist or if it a flat file.
     */
    protected void checkDirectoryExists(Directory dir) {
        if (!dir.exists()) {
            throw new RuntimeIOException("Directory " + dir + " does not exist");
        }
    }

    /**
     * Assert that the file must exist and it is not a directory.
     *
     * @param file              to check.
     * @throws RuntimeIOException if file does not exist or is a directory.
     */
    protected void checkFileExists(File file) {
        if (!file.exists()) {
            throw new RuntimeIOException("File " + file + " does not exist");
        }
    }
}
