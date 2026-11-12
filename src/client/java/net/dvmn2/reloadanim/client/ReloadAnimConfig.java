package net.dvmn2.reloadanim.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.dvmn2.reloadanim.client.io.SafeFiles;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Player-side settings stored in {@code config/reloadanim.json}.
 *
 * <p>Fields are volatile because they are read from the render thread and from resource reload threads,
 * and written from the client thread. A damaged file falls back to defaults and never crashes the client.
 */
public final class ReloadAnimConfig {

    public static final float MIN_INTENSITY = 0.0F;
    public static final float MAX_INTENSITY = 1.0F;

    private static final float DEFAULT_INTENSITY = 1.0F;
    private static final int MAX_CONFIG_BYTES = 64 * 1024; // bytes
    private static final String FILE_NAME = "reloadanim.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadAnimClient.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ReloadAnimConfig instance;

    /** Master switch. When false the vanilla loading screen is used and no pack is read. */
    public volatile boolean enabled = true;

    /**
     * Whether animations may be read from the player's resource packs.
     *
     * <p>Deliberate deviation from STYLE.md section 5.5: this defaults to true because the player installed
     * a mod made for exactly this purpose. The value is documented in the Privacy section of MODRINTH.md.
     */
    @SerializedName("allow-pack-animations")
    public volatile boolean allowPackAnimations = true;

    /** Strength of the animation over the black background, 0.0 (invisible) to 1.0 (full). */
    public volatile float intensity = DEFAULT_INTENSITY;

    /** Shows a single static frame instead of playing the animation. */
    @SerializedName("reduced-motion")
    public volatile boolean reducedMotion = false;

    private ReloadAnimConfig() {
    }

    /** Returns the loaded configuration, reading the file on first use. */
    public static synchronized ReloadAnimConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /** Discards the in-memory configuration and reads the file again. */
    public static synchronized ReloadAnimConfig reload() {
        instance = load();
        return instance;
    }

    /** Writes the current configuration to disk. Failures are logged, not thrown. */
    public static synchronized void save() {
        write(get());
    }

    private static ReloadAnimConfig load() {
        Path path = path();
        if (!Files.isRegularFile(path)) {
            ReloadAnimConfig defaults = new ReloadAnimConfig();
            write(defaults);
            return defaults;
        }
        ReloadAnimConfig config;
        try {
            String json = new String(SafeFiles.readLimited(path, MAX_CONFIG_BYTES), StandardCharsets.UTF_8);
            config = GSON.fromJson(json, ReloadAnimConfig.class);
        } catch (IOException | JsonParseException ex) {
            LOGGER.warn("Could not read {}, using defaults: {}", FILE_NAME, ex.getMessage());
            return new ReloadAnimConfig();
        }
        if (config == null) {
            return new ReloadAnimConfig();
        }
        config.clamp();
        return config;
    }

    private static void write(ReloadAnimConfig config) {
        try {
            SafeFiles.writeAtomically(path(), GSON.toJson(config).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            LOGGER.warn("Could not write {}: {}", FILE_NAME, ex.getMessage());
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private void clamp() {
        float value = intensity;
        if (Float.isNaN(value)) {
            intensity = DEFAULT_INTENSITY;
        } else {
            intensity = Math.clamp(value, MIN_INTENSITY, MAX_INTENSITY);
        }
    }
}
