package net.dvmn2.reloadanim.client.animation;

/**
 * An animation found in a resource pack, ready to be cached.
 *
 * @param png    raw PNG bytes of the strip, already size-checked
 * @param meta   validated strip description
 * @param packId identifier of the pack that supplied the strip
 */
public record PackAnimation(byte[] png, AnimationMeta meta, String packId) {
}
