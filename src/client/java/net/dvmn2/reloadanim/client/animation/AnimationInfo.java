package net.dvmn2.reloadanim.client.animation;

/**
 * Short description of the cached animation, shown by the status command.
 *
 * @param packId identifier of the pack that supplied the strip
 * @param frames number of frames stored in the strip
 */
public record AnimationInfo(String packId, int frames) {
}
