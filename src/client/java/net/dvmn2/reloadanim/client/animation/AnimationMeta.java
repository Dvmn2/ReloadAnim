package net.dvmn2.reloadanim.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Validated description of an animation strip: a PNG whose frames are stacked vertically.
 *
 * <p>Immutable and thread-safe. The optional {@code .png.mcmeta} file follows the vanilla animation format;
 * only a subset is supported:
 * <pre>
 * {
 *   "animation": {
 *     "width": 64,        // optional, must equal the image width
 *     "height": 64,       // optional, defaults to the image width (square frames)
 *     "frametime": 2,     // optional, ticks per frame (1 tick = 50 ms), 1..200
 *     "frames": [0, 1, 2] // optional, playback order; entries may be numbers or {"index": n}
 *   }
 * }
 * </pre>
 * Per-frame {@code time}, {@code interpolate} and horizontal strips are ignored or rejected.
 *
 * @param frameWidth  width of one frame, pixels
 * @param frameHeight height of one frame, pixels
 * @param stripFrames number of frames stored in the image
 * @param frameTimeMs how long each frame is shown, milliseconds
 * @param sequence    playback order as indices into the strip, never empty
 */
public record AnimationMeta(int frameWidth, int frameHeight, int stripFrames, int frameTimeMs,
        List<Integer> sequence) {

    public static final int MAX_FRAME_SIZE_PX = 512;
    public static final int MAX_STRIP_HEIGHT_PX = 16_384; // common GPU texture size limit
    public static final int MAX_STRIP_FRAMES = 256;
    public static final int MAX_SEQUENCE_LENGTH = 1_024;
    public static final int MAX_PNG_BYTES = 16 * 1024 * 1024; // bytes
    public static final int MAX_JSON_BYTES = 64 * 1024; // bytes

    private static final int MS_PER_TICK = 50;
    private static final int MIN_FRAME_TIME_TICKS = 1;
    private static final int MAX_FRAME_TIME_TICKS = 200;
    private static final String SECTION_KEY = "animation";

    public AnimationMeta {
        sequence = List.copyOf(sequence);
    }

    /**
     * Builds metadata after checking every value against the limits.
     *
     * @param imageWidth        image width, pixels
     * @param imageHeight       image height, pixels
     * @param frameWidth        frame width, pixels; must equal the image width
     * @param frameHeight       frame height, pixels
     * @param frameTimeMs       frame duration, milliseconds; clamped to the supported range
     * @param requestedSequence playback order, or empty for all frames in order
     * @throws IllegalArgumentException if any value is outside the supported limits
     */
    public static AnimationMeta create(int imageWidth, int imageHeight, int frameWidth, int frameHeight,
            int frameTimeMs, List<Integer> requestedSequence) {
        if (imageWidth < 1 || imageWidth > MAX_FRAME_SIZE_PX || imageHeight < 1
                || imageHeight > MAX_STRIP_HEIGHT_PX) {
            throw new IllegalArgumentException("Image size " + imageWidth + "x" + imageHeight + " is not supported");
        }
        if (frameWidth != imageWidth) {
            throw new IllegalArgumentException("Frame width must equal the image width (vertical strips only)");
        }
        if (frameHeight < 1 || frameHeight > MAX_FRAME_SIZE_PX || imageHeight % frameHeight != 0) {
            throw new IllegalArgumentException("Frame height " + frameHeight + " does not fit the image");
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
        return new AnimationMeta(frameWidth, frameHeight, stripFrames, clampedTime, sequence);
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
        List<Integer> sequence = List.of();
        if (json != null) {
            try {
                JsonObject section = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject(SECTION_KEY);
                if (section != null) {
                    frameWidth = readInt(section, "width", frameWidth);
                    frameHeight = readInt(section, "height", frameHeight);
                    frameTimeTicks = readInt(section, "frametime", frameTimeTicks);
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
        return create(imageWidth, imageHeight, frameWidth, frameHeight, ticks * MS_PER_TICK, sequence);
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
