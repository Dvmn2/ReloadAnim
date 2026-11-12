package net.dvmn2.reloadanim.client.animation;

/**
 * Reads the image size from a PNG header without decoding the image.
 *
 * <p>Dimensions are checked before decoding so that a small file cannot make the decoder allocate gigabytes.
 * Stateless and thread-safe.
 */
final class PngHeader {

    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] IHDR = {'I', 'H', 'D', 'R'};
    private static final int IHDR_TYPE_OFFSET = 12;
    private static final int WIDTH_OFFSET = 16;
    private static final int HEIGHT_OFFSET = 20;
    private static final int HEADER_LENGTH = 24;
    private static final int BYTE_MASK = 0xFF;

    private PngHeader() {
    }

    /** Pixel dimensions of an image. */
    record Size(int width, int height) {
    }

    /**
     * Returns the size stored in the PNG header.
     *
     * @throws IllegalArgumentException if the data is not a PNG or has a non-positive size
     */
    static Size read(byte[] data) {
        if (data.length < HEADER_LENGTH) {
            throw new IllegalArgumentException("PNG data is too short");
        }
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (data[i] != SIGNATURE[i]) {
                throw new IllegalArgumentException("Not a PNG file");
            }
        }
        for (int i = 0; i < IHDR.length; i++) {
            if (data[IHDR_TYPE_OFFSET + i] != IHDR[i]) {
                throw new IllegalArgumentException("PNG header chunk is missing");
            }
        }
        int width = readInt(data, WIDTH_OFFSET);
        int height = readInt(data, HEIGHT_OFFSET);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("PNG size is invalid");
        }
        return new Size(width, height);
    }

    private static int readInt(byte[] data, int offset) {
        return (data[offset] & BYTE_MASK) << 24
                | (data[offset + 1] & BYTE_MASK) << 16
                | (data[offset + 2] & BYTE_MASK) << 8
                | (data[offset + 3] & BYTE_MASK);
    }
}
