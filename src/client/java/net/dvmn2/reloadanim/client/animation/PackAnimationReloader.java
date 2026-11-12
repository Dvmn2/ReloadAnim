package net.dvmn2.reloadanim.client.animation;

import net.dvmn2.reloadanim.client.ReloadAnimClient;
import net.dvmn2.reloadanim.client.ReloadAnimConfig;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Looks for the animation in the packs on every resource reload and refreshes the cache.
 *
 * <p>The prepare stage runs on a worker thread and does all disk work; the apply stage runs on the client
 * thread and only schedules a cache reload. The animation found now is shown by the <em>next</em> loading
 * screen, because the current one is already on screen.
 */
public final class PackAnimationReloader extends SinglePreparationResourceReloader<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadAnimClient.MOD_ID);

    @Override
    protected Boolean prepare(ResourceManager manager, Profiler profiler) {
        ReloadAnimConfig config = ReloadAnimConfig.get();
        if (!config.allowPackAnimations) {
            AnimationCache.clear();
            return Boolean.TRUE;
        }
        if (!config.enabled) {
            return Boolean.FALSE;
        }
        return updateCache(AnimationSource.read(manager));
    }

    @Override
    protected void apply(Boolean cacheChanged, ResourceManager manager, Profiler profiler) {
        if (cacheChanged) {
            AnimationLibrary.refreshAsync();
        }
    }

    private static Boolean updateCache(@Nullable PackAnimation animation) {
        if (animation == null) {
            AnimationCache.clear();
            return Boolean.TRUE;
        }
        try {
            AnimationCache.store(animation);
            return Boolean.TRUE;
        } catch (IOException ex) {
            LOGGER.warn("Could not cache the animation from pack {}: {}", animation.packId(), ex.getMessage());
            return Boolean.FALSE;
        }
    }
}
