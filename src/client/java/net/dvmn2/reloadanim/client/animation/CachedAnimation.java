package net.dvmn2.reloadanim.client.animation;

import net.minecraft.client.texture.NativeImage;

/**
 * An animation decoded from the cache.
 *
 * <p>The holder owns {@code image}: it must be closed, or handed to a texture that takes ownership of it.
 *
 * @param image  decoded strip
 * @param meta   validated strip description
 * @param packId identifier of the pack that supplied the strip
 */
public record CachedAnimation(NativeImage image, AnimationMeta meta, String packId) {
}
