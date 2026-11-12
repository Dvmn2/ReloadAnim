package net.dvmn2.reloadanim.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.dvmn2.reloadanim.client.overlay.LoadingAnimationRenderer;
import net.dvmn2.reloadanim.client.overlay.SplashTargets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.IntSupplier;

/**
 * Swaps the parts of the vanilla loading screen for the animation: the background becomes black, the first
 * logo half becomes the animation, the second logo half and the progress bar are skipped.
 *
 * <p>All decisions live in {@link LoadingAnimationRenderer}. Vanilla keeps running its own timing, fading and
 * completion logic, so the screen still ends exactly when the reload ends.
 */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @WrapOperation(
            method = SplashTargets.RENDER,
            at = @At(value = "INVOKE", target = SplashTargets.BRAND_COLOR))
    private int reloadanim$background(IntSupplier brand, Operation<Integer> original) {
        return LoadingAnimationRenderer.isActive()
                ? LoadingAnimationRenderer.BACKGROUND_ARGB
                : original.call(brand);
    }

    @WrapOperation(
            method = SplashTargets.RENDER,
            at = @At(value = "INVOKE", target = SplashTargets.DRAW_TEXTURE, ordinal = 0))
    private void reloadanim$drawAnimation(DrawContext context, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight,
            int textureWidth, int textureHeight, int color, Operation<Void> original) {
        if (!LoadingAnimationRenderer.draw(context, color)) {
            original.call(context, pipeline, texture, x, y, u, v, width, height, regionWidth, regionHeight,
                    textureWidth, textureHeight, color);
        }
    }

    @WrapOperation(
            method = SplashTargets.RENDER,
            at = @At(value = "INVOKE", target = SplashTargets.DRAW_TEXTURE, ordinal = 1))
    private void reloadanim$skipSecondLogoHalf(DrawContext context, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight,
            int textureWidth, int textureHeight, int color, Operation<Void> original) {
        if (!LoadingAnimationRenderer.isActive()) {
            original.call(context, pipeline, texture, x, y, u, v, width, height, regionWidth, regionHeight,
                    textureWidth, textureHeight, color);
        }
    }

    @WrapOperation(
            method = SplashTargets.RENDER,
            at = @At(value = "INVOKE", target = SplashTargets.PROGRESS_BAR))
    private void reloadanim$skipProgressBar(SplashOverlay overlay, DrawContext context, int minX, int minY,
            int maxX, int maxY, float opacity, Operation<Void> original) {
        if (!LoadingAnimationRenderer.isActive()) {
            original.call(overlay, context, minX, minY, maxX, maxY, opacity);
        }
    }
}
