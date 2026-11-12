package net.dvmn2.reloadanim.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Validated description of an animation strip: a PNG whose frames are stacked vertically.
 *
 * <p>Immutable and thread-safe. The optional {@code .png.mcmeta} file follows the vanilla animation format
 * with one extra key; only a subset is supported:
 * <pre>
 * {
 *   "animation": {
 *     "mode": "loop",     // optional: "loop" (default), "once" or "pingpong"
 *     "width": 1920,      // optional, must equal the image width
 *     "height": 1080,     // optional, defaults to the image width (square frames)
 *     "frametime": 2,     // optional, ticks per frame (1 tick = 50 ms), 1..200; unused by "once"
 *     "frames": [0, 1, 2] // optional, playback order; entries may be numbers or {"index": n}
 *   }
 * }
 * </pre>
 * The image height must be a whole multiple of the frame height: a 1920x3240 image with {@code "height": 1080}
 * is three frames of 1920x1080. Per-frame {@code time}, {@code interpolate} and horizontal strips are not supported.
 *
 * @param frameWidth  width of one frame, pixels
 * @param frameHeight height of one frame, pixels
 * @param stripFrames number of frames stored in the image
 * @param frameTimeMs how long each frame is shown, milliseconds
 * @param mode        how the sequence is played
 * @param sequence    playback order as indices into the strip, never empty
 */
public record AnimationMeta(int frameWidth, int frameHeight, int stripFrames, int frameTimeMs, Mode mode,
        List<Integer> sequence) {

    public static final int MAX_IMAGE_WIDTH_PX = 4_096;
    public static final int MAX_STRIP_HEIGHT_PX = 16_384; // common GPU texture size limit
    public static final int MAX_STRIP_FRAMES = 256;
    public static final int MAX_PIXELS = 32 * 1024 * 1024; // 32 megapixels, 128 MiB of RGBA
    public static final int MAX_SEQUENCE_LENGTH = 1_024;
    public static final int MAX_PNG_BYTES = 64 * 1024 * 1024; // bytes
    public static final int MAX_JSON_BYTES = 64 * 1024; // bytes

    private static final int MS_PER_TICK = 50;
    private static final int MIN_FRAME_TIME_TICKS = 1;
    private static final int MAX_FRAME_TIME_TICKS = 200;
    private static final String SECTION_KEY = "animation";

    /** How the playback sequence is traversed. */
    public enum Mode {
        /** Plays the sequence again and again by time. */
        LOOP,
        /** Plays the sequence once, following the loading progress instead of time. */
        ONCE,
        /** Plays forward, then backward, by time, without repeating the end frames. */
        PINGPONG;

        /** Returns the name used in {@code .mcmeta} and in the cache. */
        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * Finds a mode by its serialized name.
         *
         * @throws IllegalArgumentException if the name is unknown
         */
        public static Mode fromName(String name) {
            for (Mode mode : values()) {
                if (mode.serializedName().equals(name.toLowerCase(Locale.ROOT))) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Unknown animation mode: " + name);
        }
    }

    public AnimationMeta {
        sequence = List.copyOf(sequence);
    }

    /**
     * Builds metadata after checking every value against the limits.
     *
     * @param imageWidth        image width, pixels
     * @param imageHeight       image height, pixels
     * @param frameWidth        frame width, pixels; must equal the image width
     * @param frameHeight       frame height, pixels; must divide the image height
     * @param frameTimeMs       frame duration, milliseconds; clamped to the supported range
     * @param mode              playback mode
     * @param requestedSequence playback order, or empty for all frames in order
     * @throws IllegalArgumentException if any value is outside the supported limits
     */
    public static AnimationMeta create(int imageWidth, int imageHeight, int frameWidth, int frameHeight,
            int frameTimeMs, Mode mode, List<Integer> requestedSequence) {
        if (imageWidth < 1 || imageWidth > MAX_IMAGE_WIDTH_PX || imageHeight < 1
                || imageHeight > MAX_STRIP_HEIGHT_PX || (long) imageWidth * imageHeight > MAX_PIXELS) {
            throw new IllegalArgumentException("Image size " + imageWidth + "x" + imageHeight + " is not supported");
        }
        if (frameWidth != imageWidth) {
            throw new IllegalArgumentException("Frame width must equal the image width (vertical strips only)");
        }
        if (frameHeight < 1 || frameHeight > imageHeight || imageHeight % frameHeight != 0) {
            throw new IllegalArgumentException("Frame height " + frameHeight + " does not divide the image height "
                    + imageHeight);
        }
        int stripFrames = imageHeight / frameHeight;
        if (stripFrames > MAX_STRIP_FRAMES) {
            throw new IllegalArgumentException("Too many frames in the strip: " + stripFrames);
        }
        if (requestedSequence.size() > MAX_SEQUENCE_LENGTH) {
            throw new IllegalArgumentException("Frame sequence is too long: " + requestedSequence.size());
        }
        List<Integer> sequence = requestedSequence.isEmpty()
                ? IntStream.range(0, stripFrames).boxed().toList()
                : requestedSequence;
        for (Integer index : sequence) {
            if (index == null || index < 0 || index >= stripFrames) {
                throw new IllegalArgumentException("Frame index " + index + " is out of range");
            }
        }
        int clampedTime = Math.clamp(frameTimeMs, MIN_FRAME_TIME_TICKS * MS_PER_TICK,
                MAX_FRAME_TIME_TICKS * MS_PER_TICK);
        return new AnimationMeta(frameWidth, frameHeight, stripFrames, clampedTime, mode, sequence);
    }

    /**
     * Parses the {@code .png.mcmeta} content that belongs to an image.
     *
     * @param json        file content, or null when the pack has no metadata file
     * @param imageWidth  image width, pixels
     * @param imageHeight image height, pixels
     * @throws IllegalArgumentException if the content is malformed or outside the supported limits
     */
    public static AnimationMeta fromMcmeta(@Nullable String json, int imageWidth, int imageHeight) {
        int frameWidth = imageWidth;
        int frameHeight = imageWidth;
        int frameTimeTicks = MIN_FRAME_TIME_TICKS;
        Mode mode = Mode.LOOP;
        List<Integer> sequence = List.of();
        if (json != null) {
            try {
                JsonObject section = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject(SECTION_KEY);
                if (section != null) {
                    frameWidth = readInt(section, "width", frameWidth);
                    frameHeight = readInt(section, "height", frameHeight);
                    frameTimeTicks = readInt(section, "frametime", frameTimeTicks);
                    if (section.has("mode")) {
                        mode = Mode.fromName(section.get("mode").getAsString());
                    }
                    if (section.has("frames")) {
                        sequence = readFrames(section.getAsJsonArray("frames"));
                    }
                }
            } catch (JsonParseException | IllegalStateException | ClassCastException
                     | UnsupportedOperationException | NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid animation metadata: " + ex.getMessage(), ex);
            }
        }
        int ticks = Math.clamp(frameTimeTicks, MIN_FRAME_TIME_TICKS, MAX_FRAME_TIME_TICKS);
        return create(imageWidth, imageHeight, frameWidth, frameHeight, ticks * MS_PER_TICK, mode, sequence);
    }

    private static int readInt(JsonObject section, String key, int fallback) {
        JsonElement value = section.get(key);
        return value == null ? fallback : value.getAsInt();
    }

    private static List<Integer> readFrames(JsonArray frames) {
        if (frames.size() > MAX_SEQUENCE_LENGTH) {
            throw new IllegalArgumentException("Frame sequence is too long: " + frames.size());
        }
        List<Integer> result = new ArrayList<>(frames.size());
        for (JsonElement entry : frames) {
            if (entry.isJsonObject()) {
                JsonElement index = entry.getAsJsonObject().get("index");
                if (index == null) {
                    throw new IllegalArgumentException("Frame entry has no index");
                }
                result.add(index.getAsInt());
            } else {
                result.add(entry.getAsInt());
            }
        }
        return result;
    }
}
