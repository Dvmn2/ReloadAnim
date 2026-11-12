package net.dvmn2.reloadanim.client.overlay;

/**
 * Mixin target descriptors for the loading overlay, kept apart from the mixin so that its package holds only
 * mixin classes. Yarn names for Minecraft 1.21.11; update together with the game version.
 */
public final class SplashTargets {

    /** {@code SplashOverlay#render}, the only method that draws the loading screen. */
    public static final String RENDER = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V";

    /** The call that supplies the background colour (Mojang red, or black in monochrome mode). */
    public static final String BRAND_COLOR = "Ljava/util/function/IntSupplier;getAsInt()I";

    /** {@code DrawContext#drawTexture} with a colour: used by vanilla for both halves of the logo. */
    public static final String DRAW_TEXTURE = "Lnet/minecraft/client/gui/DrawContext;drawTexture("
            + "Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V";

    /** {@code SplashOverlay#renderProgressBar}. */
    public static final String PROGRESS_BAR = "Lnet/minecraft/client/gui/screen/SplashOverlay;"
            + "renderProgressBar(Lnet/minecraft/client/gui/DrawContext;IIIIF)V";

    private SplashTargets() {
    }
}
