package net.dvmn2.reloadanim.client.mixin;

import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read access to the loading progress that vanilla keeps in private fields. */
@Mixin(SplashOverlay.class)
public interface SplashOverlayAccessor {

    /** Smoothed loading progress, 0.0 to 1.0. */
    @Accessor("progress")
    float reloadanim$getProgress();

    /** Timestamp at which the reload finished; zero or negative while it is still running. */
    @Accessor("reloadCompleteTime")
    long reloadanim$getReloadCompleteTime();
}
