package net.dvmn2.reloadanim.client.overlay;

import net.dvmn2.reloadanim.client.ReloadAnimConfig;
import net.dvmn2.reloadanim.client.animation.AnimationLibrary;
import net.dvmn2.reloadanim.client.animation.AnimationMeta;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;

/**
 * Draws the animation in place of the vanilla logo and tells the overlay mixin when to step aside.
 *
 * <p>Render-thread only. Fading is not computed here: the vanilla overlay already passes its current opacity
 * as the alpha of the logo colour, so the animation fades in and out exactly like the logo would.
 */
public final class LoadingAnimationRenderer {

    /** Opaque black that replaces the vanilla background colour. */
    public static final int BACKGROUND_ARGB = 0xFF000000;

    private static final int WHITE_RGB = 0x00FFFFFF;
    private static final int ALPHA_SHIFT = 24;
    private static final long NANOS_PER_MS = 1_000_000L;
    private static final long SESSION_GAP_NANOS = 1_000_000_000L; // a longer pause means a new loading screen

    private static long sessionStartNanos;
    private static long lastDrawNanos;

    private LoadingAnimationRenderer() {
    }

    /** Returns true if the animation replaces the vanilla loading screen right now. */
    public static boolean isActive() {
        return ReloadAnimConfig.get().enabled && AnimationLibrary.isReady();
    }

    /**
     * Draws the current frame, centred and scaled to fit the screen.
     *
     * @param context      draw context of the overlay
     * @param vanillaColor ARGB colour vanilla uses for the logo; only its alpha is used
     * @return true if the frame was drawn (or intentionally skipped), false if vanilla should draw the logo
     */
    public static boolean draw(DrawContext context, int vanillaColor) {
        if (!isActive()) {
            return false;
        }
        try {
            return renderFrame(context, vanillaColor);
        } catch (RuntimeException ex) {
            AnimationLibrary.disable(ex);
            return false;
        }
    }

    private static boolean renderFrame(DrawContext context, int vanillaColor) {
        ReloadAnimConfig config = ReloadAnimConfig.get();
        AnimationMeta meta = AnimationLibrary.acquire(MinecraftClient.getInstance().getTextureManager());
        if (meta == null) {
            return false;
        }
        int alpha = Math.round((vanillaColor >>> ALPHA_SHIFT) * config.intensity);
        if (alpha <= 0) {
            return true;
        }
        int stripFrame = meta.sequence().get(sequenceIndex(meta, config.reducedMotion));
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        float scale = Math.min((float) screenWidth / meta.frameWidth(), (float) screenHeight / meta.frameHeight());
        int width = Math.max(1, Math.round(meta.frameWidth() * scale));
        int height = Math.max(1, Math.round(meta.frameHeight() * scale));
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                AnimationLibrary.DYNAMIC_TEXTURE,
                (screenWidth - width) / 2,
                (screenHeight - height) / 2,
                0.0F,
                (float) (stripFrame * meta.frameHeight()),
                width,
                height,
                meta.frameWidth(),
                meta.frameHeight(),
                meta.frameWidth(),
                meta.stripFrames() * meta.frameHeight(),
                (alpha << ALPHA_SHIFT) | WHITE_RGB);
        return true;
    }

    private static int sequenceIndex(AnimationMeta meta, boolean reducedMotion) {
        if (reducedMotion) {
            return 0;
        }
        long now = System.nanoTime();
        if (now - lastDrawNanos > SESSION_GAP_NANOS) {
            sessionStartNanos = now;
        }
        lastDrawNanos = now;
        long elapsedMs = (now - sessionStartNanos) / NANOS_PER_MS;
        return (int) ((elapsedMs / meta.frameTimeMs()) % meta.sequence().size());
    }
}
