package net.dvmn2.reloadanim.client.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Disk helpers: atomic writes and size-checked reads.
 *
 * <p>Stateless and thread-safe. Sizes read from files or packs are never trusted, so every read has a limit.
 */
public final class SafeFiles {

    private static final String TEMP_SUFFIX = ".tmp";

    private SafeFiles() {
    }

    /**
     * Writes bytes to a sibling temporary file, then moves it over the target.
     *
     * @param target destination file; its parent directories are created
     * @param bytes  file content
     * @throws IOException if the file cannot be written
     */
    public static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Reads a whole file after checking that it is not larger than the limit.
     *
     * @param file     file to read
     * @param maxBytes maximum accepted size, bytes
     * @throws IOException if the file is too large or cannot be read
     */
    public static byte[] readLimited(Path file, int maxBytes) throws IOException {
        if (Files.size(file) > maxBytes) {
            throw new IOException(file.getFileName() + " is larger than " + maxBytes + " bytes");
        }
        return Files.readAllBytes(file);
    }

    /**
     * Reads a stream to the end, refusing to buffer more than the limit.
     *
     * @param in       stream to read; not closed here
     * @param maxBytes maximum accepted size, bytes
     * @throws IOException if the stream is too large or cannot be read
     */
    public static byte[] readLimited(InputStream in, int maxBytes) throws IOException {
        byte[] data = in.readNBytes(maxBytes + 1);
        if (data.length > maxBytes) {
            throw new IOException("Data is larger than " + maxBytes + " bytes");
        }
        return data;
    }
}
