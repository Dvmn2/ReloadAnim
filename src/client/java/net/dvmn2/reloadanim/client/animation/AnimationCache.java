package net.dvmn2.reloadanim.client.animation;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.dvmn2.reloadanim.client.ReloadAnimClient;
import net.dvmn2.reloadanim.client.io.SafeFiles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * On-disk copy of the animation, so that the loading screen can show it before any pack is read.
 *
 * <pre>
 * &lt;gameDir&gt;/reloadanim/cache/animation.png   raw strip, copied as-is
 * &lt;gameDir&gt;/reloadanim/cache/meta.json        {@code MetaDto}, version 1
 * </pre>
 * Both files are replaced under one lock, so a reader never sees a new image with old metadata.
 * Thread-safe; every method may be called from any thread.
 */
final class AnimationCache {

    private static final int FORMAT_VERSION = 1;
    private static final int MAX_PACK_ID_LENGTH = 256;
    private static final String DIRECTORY = "reloadanim";
    private static final String CACHE_DIRECTORY = "cache";
    private static final String PNG_FILE = "animation.png";
    private static final String META_FILE = "meta.json";
    private static final Object LOCK = new Object();
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadAnimClient.MOD_ID);

    /** Format of {@code meta.json}. Unknown fields are ignored, missing ones fail validation. */
    private static final class MetaDto {
        public int version;
        public String pack;
        @SerializedName("frame-width")
        public int frameWidth;
        @SerializedName("frame-height")
        public int frameHeight;
        @SerializedName("frame-time-ms")
        public int frameTimeMs;
        public List<Integer> sequence;
    }

    private AnimationCache() {
    }

    /** Replaces the cached animation with the given one. */
    static void store(PackAnimation animation) throws IOException {
        MetaDto dto = new MetaDto();
        dto.version = FORMAT_VERSION;
        dto.pack = animation.packId();
        dto.frameWidth = animation.meta().frameWidth();
        dto.frameHeight = animation.meta().frameHeight();
        dto.frameTimeMs = animation.meta().frameTimeMs();
        dto.sequence = animation.meta().sequence();
        byte[] meta = GSON.toJson(dto).getBytes(StandardCharsets.UTF_8);
        synchronized (LOCK) {
            Path directory = directory();
            SafeFiles.writeAtomically(directory.resolve(PNG_FILE), animation.png());
            SafeFiles.writeAtomically(directory.resolve(META_FILE), meta);
        }
    }

    /** Deletes the cached animation, if any. Failures are logged. */
    static void clear() {
        synchronized (LOCK) {
            try {
                // Metadata goes first: an image without metadata reads as "no cache".
                Files.deleteIfExists(directory().resolve(META_FILE));
                Files.deleteIfExists(directory().resolve(PNG_FILE));
            } catch (IOException ex) {
                LOGGER.warn("Could not clear the animation cache: {}", ex.getMessage());
            }
        }
    }

    /**
     * Reads and decodes the cached animation.
     *
     * @return the animation, or null if there is no cache or it is unreadable
     */
    static @Nullable CachedAnimation load() {
        synchronized (LOCK) {
            Path png = directory().resolve(PNG_FILE);
            Path metaFile = directory().resolve(META_FILE);
            if (!Files.isRegularFile(png) || !Files.isRegularFile(metaFile)) {
                return null;
            }
            try {
                String json = new String(SafeFiles.readLimited(metaFile, AnimationMeta.MAX_JSON_BYTES),
                        StandardCharsets.UTF_8);
                MetaDto dto = GSON.fromJson(json, MetaDto.class);
                if (dto == null || dto.version != FORMAT_VERSION || dto.pack == null || dto.sequence == null) {
                    LOGGER.warn("Ignoring the animation cache: unsupported format");
                    return null;
                }
                byte[] bytes = SafeFiles.readLimited(png, AnimationMeta.MAX_PNG_BYTES);
                PngHeader.Size size = PngHeader.read(bytes);
                AnimationMeta meta = AnimationMeta.create(size.width(), size.height(), dto.frameWidth,
                        dto.frameHeight, dto.frameTimeMs, dto.sequence);
                NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
                return new CachedAnimation(image, meta, limit(dto.pack));
            } catch (IOException | JsonParseException | IllegalArgumentException ex) {
                LOGGER.warn("Ignoring the unreadable animation cache: {}", ex.getMessage());
                return null;
            }
        }
    }

    private static Path directory() {
        return FabricLoader.getInstance().getGameDir().resolve(DIRECTORY).resolve(CACHE_DIRECTORY);
    }

    private static String limit(String value) {
        return value.length() > MAX_PACK_ID_LENGTH ? value.substring(0, MAX_PACK_ID_LENGTH) : value;
    }
}
