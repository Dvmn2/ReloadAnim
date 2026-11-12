package net.dvmn2.reloadanim.client.animation;

import net.dvmn2.reloadanim.client.ReloadAnimClient;
import net.dvmn2.reloadanim.client.ReloadAnimConfig;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Holds the animation that the loading screen shows.
 *
 * <p>Two stages: a background thread decodes the cached strip and <em>stages</em> it; the render thread picks
 * the staged strip up on the next frame and uploads it as a GPU texture, because textures may only be created
 * on the render thread. Staging and status are thread-safe; {@link #acquire} is render-thread only.
 */
public final class AnimationLibrary {

    /** Identifier of the dynamic texture that holds the strip. */
    public static final Identifier DYNAMIC_TEXTURE = Identifier.of(ReloadAnimClient.MOD_ID, "dynamic/loading");

    private static final String IO_THREAD_NAME = "ReloadAnim-IO";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadAnimClient.MOD_ID);
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, IO_THREAD_NAME);
        thread.setDaemon(true);
        return thread;
    });
    private static final Object LOCK = new Object();

    /** Result of a cache read that the render thread has not applied yet. A null animation means "clear". */
    private record Staged(@Nullable CachedAnimation animation) {
    }

    private static @Nullable Staged pending; // guarded by LOCK
    private static volatile boolean available;
    private static volatile boolean failed;
    private static volatile @Nullable AnimationInfo info;
    private static @Nullable AnimationMeta uploadedMeta; // render thread only

    private AnimationLibrary() {
    }

    /** Reloads the cached animation on a background thread and stages it for the render thread. */
    public static void refreshAsync() {
        IO_EXECUTOR.execute(AnimationLibrary::refresh);
    }

    /** Returns true if an animation is ready to be drawn and has not been switched off after an error. */
    public static boolean isReady() {
        return available && !failed;
    }

    /** Returns a description of the cached animation, or null if there is none. */
    public static @Nullable AnimationInfo info() {
        return info;
    }

    /**
     * Switches the animation off after a rendering error, so the vanilla screen takes over.
     * Only the first call logs.
     */
    public static void disable(RuntimeException cause) {
        if (!failed) {
            failed = true;
            LOGGER.error("Animation rendering failed; falling back to the vanilla loading screen", cause);
        }
    }

    /** Clears the error state set by {@link #disable}. */
    public static void resetFailure() {
        failed = false;
    }

    /**
     * Makes the staged animation current and returns its description.
     * Must be called on the render thread.
     *
     * @return the description of the uploaded animation, or null if there is none
     */
    public static @Nullable AnimationMeta acquire(TextureManager textures) {
        Staged next = takePending();
        if (next != null) {
            replaceTexture(textures, next.animation());
        }
        return uploadedMeta;
    }

    private static void refresh() {
        ReloadAnimConfig config = ReloadAnimConfig.get();
        CachedAnimation loaded = null;
        if (config.enabled && config.allowPackAnimations) {
            loaded = AnimationCache.load();
        }
        stage(loaded);
    }

    private static void stage(@Nullable CachedAnimation animation) {
        synchronized (LOCK) {
            if (pending != null && pending.animation() != null) {
                // Never reached the GPU, so nobody else owns the native memory.
                pending.animation().image().close();
            }
            pending = new Staged(animation);
            available = animation != null;
            info = animation == null ? null : new AnimationInfo(animation.packId(), animation.meta().stripFrames());
        }
    }

    private static @Nullable Staged takePending() {
        synchronized (LOCK) {
            Staged result = pending;
            pending = null;
            return result;
        }
    }

    private static void replaceTexture(TextureManager textures, @Nullable CachedAnimation animation) {
        if (uploadedMeta != null) {
            textures.destroyTexture(DYNAMIC_TEXTURE);
            uploadedMeta = null;
        }
        if (animation == null) {
            return;
        }
        NativeImageBackedTexture texture = new NativeImageBackedTexture(DYNAMIC_TEXTURE::toString, animation.image());
        textures.registerTexture(DYNAMIC_TEXTURE, texture);
        // Make sure the pixel data is on the GPU before the first draw.
        texture.upload();
        uploadedMeta = animation.meta();
    }
}
