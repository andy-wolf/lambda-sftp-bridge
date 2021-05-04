package de.andywolf.sftpbridge.util;

import de.andywolf.sftpbridge.RuntimeIOException;
import de.andywolf.sftpbridge.base.Directory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Contains a number of static helper methods.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    public static final char PATH_SEPARATOR_CHAR = '/';
    public static final String PATH_SEPARATOR = Character.toString(PATH_SEPARATOR_CHAR);
    public static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile(PATH_SEPARATOR);

    /**
     * Writes the contents of an {@link InputStream} to an {@link OutputStream}.
     *
     * @param from the {@link InputStream} to read from.
     * @param to the {@link OutputStream} to write to.
     */
    public static void write(InputStream from, OutputStream to) {
        try {
            byte[] bytes = new byte[1024];
            int nRead;
            while ((nRead = from.read(bytes, 0, bytes.length)) != -1) {
                to.write(bytes, 0, nRead);
            }
        } catch (IOException ioe) {
            throw new RuntimeIOException(ioe);
        }
    }

    /**
     * Construct a new (host) path from a parent directory, and a child.
     *
     * @param parent The parent directory
     * @param child  The path that should be appended to the parent.
     * @return A newly constructed path.
     */
    public static String constructPath(final Directory parent, final String child) {
        return parent.getFullDirectoryPath() + PATH_SEPARATOR + child;
    }

    public static String mkString(List<String> strings, String sep) {
        if (strings.isEmpty()) return "";

        StringBuilder b = new StringBuilder(strings.get(0));
        for (int i = 1; i < strings.size(); i++) {
             b.append(sep).append(strings.get(i));
        }
        return b.toString();
    }

    public static List<String> splitPath(String path) {
        Pattern s = PATH_SEPARATOR_PATTERN;
        List<String> l = new ArrayList<String>();
        for (String p : s.split(path)) {
            if (p.isEmpty()) continue;
            l.add(p);
        }
        return l;
    }

    public static String joinPath(List<String> pathComponents) {

        if (pathComponents.isEmpty()) {
            return PATH_SEPARATOR;
        }

        return PATH_SEPARATOR + mkString(pathComponents, PATH_SEPARATOR);
    }

}
