package net.dvmn2.reloadanim.client.animation;

import net.dvmn2.reloadanim.client.ReloadAnimClient;
import net.dvmn2.reloadanim.client.io.SafeFiles;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Finds the animation strip in the active resource packs.
 *
 * <p>Looks for {@code reloadanim:textures/gui/loading.png} and the optional {@code .mcmeta} file next to it;
 * the pack with the highest priority wins. Called from the prepare stage of a reload (a worker thread).
 */
final class AnimationSource {

    private static final Identifier PACK_TEXTURE = Identifier.of(ReloadAnimClient.MOD_ID, "textures/gui/loading.png");
    private static final Identifier PACK_META =
            Identifier.of(ReloadAnimClient.MOD_ID, "textures/gui/loading.png.mcmeta");
    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadAnimClient.MOD_ID);

    private AnimationSource() {
    }

    /**
     * Reads and validates the animation from the pack stack.
     *
     * @return the animation, or null if no pack provides a usable one
     */
    static @Nullable PackAnimation read(ResourceManager manager) {
        Optional<Resource> texture = manager.getResource(PACK_TEXTURE);
        if (texture.isEmpty()) {
            return null;
        }
        String packId = texture.get().getPackId();
        try {
            byte[] png;
            try (InputStream in = texture.get().getInputStream()) {
                png = SafeFiles.readLimited(in, AnimationMeta.MAX_PNG_BYTES);
            }
            PngHeader.Size size = PngHeader.read(png);
            AnimationMeta meta = AnimationMeta.fromMcmeta(readMeta(manager), size.width(), size.height());
            // Decode once so that a damaged image is rejected here instead of being cached.
            try (NativeImage ignored = NativeImage.read(new ByteArrayInputStream(png))) {
                return new PackAnimation(png, meta, packId);
            }
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.warn("Ignoring animation from pack {}: {}", packId, ex.getMessage());
            return null;
        }
    }

    private static @Nullable String readMeta(ResourceManager manager) throws IOException {
        Optional<Resource> meta = manager.getResource(PACK_META);
        if (meta.isEmpty()) {
            return null;
        }
        try (InputStream in = meta.get().getInputStream()) {
            return new String(SafeFiles.readLimited(in, AnimationMeta.MAX_JSON_BYTES), StandardCharsets.UTF_8);
        }
    }
}
